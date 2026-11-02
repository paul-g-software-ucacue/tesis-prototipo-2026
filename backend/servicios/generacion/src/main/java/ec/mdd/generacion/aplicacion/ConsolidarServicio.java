package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.Consolidar;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
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
    private final Clock reloj;

    /** Recibe la consulta y los puertos de salida. */
    public ConsolidarServicio(
            ConsultarConsolidado consultar,
            RepositorioConsolidacion consolidacion,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.consultar = consultar;
        this.consolidacion = consolidacion;
        this.registro = registro;
        this.transaccion = transaccion;
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
                    consolidacion.guardarConsolidacion(version, new Consolidacion(quien, ahora));
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.DISTRIBUTIVO_CONSOLIDADO,
                                    Map.of("periodo", periodo, "version", actual.numero())));
                    return version;
                });
        return consultar.consolidado(periodo);
    }
}
