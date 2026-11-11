package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.VersionCongelada;
import ec.mdd.generacion.puertos.entrada.ConsolidadoEnRevision;
import ec.mdd.generacion.puertos.entrada.Consolidar;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.EnRevision;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Consolida la versión vigente: marca quién y cuándo, sin bloquear (RF-32). No toca el estado de
 * los parciales, que es de Aprobación (MD-04), y admite inconsistencias abiertas.
 */
public final class ConsolidarServicio implements Consolidar {

    private final ConsultarConsolidado consultar;
    private final RepositorioConsolidacion consolidacion;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final EnRevision enRevision;
    private final Clock reloj;

    /** Recibe la consulta y los puertos de salida. */
    public ConsolidarServicio(
            ConsultarConsolidado consultar,
            RepositorioConsolidacion consolidacion,
            RegistroDeCambios registro,
            Transaccion transaccion,
            EnRevision enRevision,
            Clock reloj) {
        this.consultar = consultar;
        this.consolidacion = consolidacion;
        this.registro = registro;
        this.transaccion = transaccion;
        this.enRevision = enRevision;
        this.reloj = reloj;
    }

    @Override
    public ConsolidadoDelPeriodo consolidar(String periodo, String quien) {
        ConsolidadoDelPeriodo actual = consultar.consolidado(periodo);
        UUID version = actual.version().orElseThrow(SinDistributivo::new);
        if (!actual.modificable()) {
            throw new PeriodoCerrado();
        }
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    // #215, D-99: dentro de la transacción, para no quedar después de un envío.
                    // F13-H01, research §4: primero la versión congelada, la causa más fuerte.
                    exigirNoCongelada(enRevision, periodo);
                    if (enRevision.enRevision(periodo)) {
                        throw new ConsolidadoEnRevision();
                    }
                    consolidacion.guardarConsolidacion(version, new Consolidacion(quien, ahora));
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.DISTRIBUTIVO_CONSOLIDADO,
                                    Map.of("periodo", periodo, "version", actual.numero()),
                                    actual.consolidacion()
                                            .map(ConsolidarServicio::valores)
                                            .orElse(Map.of()),
                                    valores(new Consolidacion(quien, ahora))));
                    return version;
                });
        return consultar.consolidado(periodo);
    }

    /**
     * Rechaza un cambio del consolidado si la versión vigente está congelada para la firma
     * (F13-H01, D-100). Lo usan consolidar, justificar y adjuntar el informe, dentro de su
     * transacción y antes de la revisión.
     *
     * @throws VersionCongelada si lo está, con su número
     */
    static void exigirNoCongelada(EnRevision enRevision, String periodo) {
        OptionalInt congelada = enRevision.versionCongelada(periodo);
        if (congelada.isPresent()) {
            throw new VersionCongelada(congelada.getAsInt());
        }
    }

    private static Map<String, Object> valores(Consolidacion consolidacion) {
        return Map.of(
                "consolidadoPor", consolidacion.por(),
                "consolidadoEn", consolidacion.en().toString());
    }
}
