package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Abrir un periodo en tres pasos, sin una transacción entre los dos servicios (principio V,
 * research §3): comprobar el periodo y su versión actual, pedir a Normativa que asocie la versión
 * (en su propia transacción) y, solo si confirmó, registrar {@code PERIODO_ABIERTO} en una
 * transacción de Académico.
 *
 * <p>Es seguro sin una transacción común porque un periodo sin versión no puede estar cerrado ni
 * cerrarse mientras tanto (salir de Planificación exige la versión), y Normativa garantiza una sola
 * versión por periodo. Si el registro falla, la asociación queda, con su {@code VERSION_ASOCIADA}
 * en Normativa, y abrir otra vez con la misma versión no cambia nada.
 */
public final class AbrirPeriodoServicio implements AbrirPeriodo {

    private final RepositorioPeriodos repositorio;
    private final NormativaDelPeriodo normativa;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public AbrirPeriodoServicio(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.normativa = normativa;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public PeriodoConsultado abrir(String codigo, int version, String quien) {
        PeriodoAcademico periodo =
                repositorio.porCodigo(codigo).orElseThrow(() -> new PeriodoNoEncontrado(codigo));
        if (periodo.estado() == EstadoPeriodo.CERRADO) {
            throw new PeriodoCerrado(codigo);
        }
        OptionalInt actual = normativa.versionDe(codigo);
        if (actual.isPresent()) {
            if (actual.getAsInt() != version) {
                throw new PeriodoConOtraVersion(codigo, actual.getAsInt());
            }
            return new PeriodoConsultado(periodo, actual);
        }
        normativa.asociar(codigo, version, quien);
        transaccion.ejecutar(
                () -> {
                    registro.registrar(
                            new CambioRegistrado(
                                    reloj.instant(),
                                    quien,
                                    Accion.PERIODO_ABIERTO,
                                    Map.of("codigo", codigo, "version", version)));
                    return null;
                });
        return new PeriodoConsultado(periodo, OptionalInt.of(version));
    }
}
