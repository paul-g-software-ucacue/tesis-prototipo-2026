package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.PeriodoListo;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarDistributivo;
import ec.mdd.generacion.puertos.entrada.CrearDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Crea el distributivo solo si el tablero de preparación dice que el periodo está listo (RF-56): el
 * servidor no se fía del botón. Primero consulta y después guarda, para no abrir una transacción
 * durante una llamada a otro proceso.
 */
public final class CrearDistributivoServicio implements CrearDistributivo, ConsultarDistributivo {

    private final DatosParaSugerencia datos;
    private final RepositorioDistributivos distributivos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida y el reloj. */
    public CrearDistributivoServicio(
            DatosParaSugerencia datos,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.datos = datos;
        this.distributivos = distributivos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public DistributivoDelPeriodo crear(String periodo, String quien) {
        if (distributivos.vigenteDe(periodo).isPresent()) {
            throw new DistributivoRepetido();
        }
        PeriodoListo preparacion =
                datos.periodoListo(periodo).orElseThrow(PeriodoNoEncontrado::new);
        if (!preparacion.listo() || preparacion.cerrado()) {
            throw new PeriodoNoListo();
        }
        List<String> carreras =
                datos.ofertaDelPeriodo(periodo).orElseThrow(PeriodoNoEncontrado::new).stream()
                        .filter(una -> !una.retirada())
                        .map(AsignaturaParalelo::carrera)
                        .toList();
        VersionDistributivo version = VersionDistributivo.primera(UUID.randomUUID(), carreras);
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    distributivos.crear(periodo, version, quien, ahora);
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    quien,
                                    CambioDeDistributivo.Accion.DISTRIBUTIVO_CREADO,
                                    Map.of("periodo", periodo, "version", version.numero())));
                    return version;
                });
        return delPeriodo(periodo, version);
    }

    @Override
    public DistributivoDelPeriodo distributivo(String periodo) {
        return distributivos
                .vigenteDe(periodo)
                .map(version -> delPeriodo(periodo, version))
                .orElseThrow(DistributivoNoEncontrado::new);
    }

    private static DistributivoDelPeriodo delPeriodo(String periodo, VersionDistributivo version) {
        return new DistributivoDelPeriodo(
                periodo,
                version.numero(),
                version.parciales().stream().map(ParcialCarrera::carrera).toList());
    }
}
