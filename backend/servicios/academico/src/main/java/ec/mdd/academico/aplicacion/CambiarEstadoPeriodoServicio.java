package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.CambiarEstadoPeriodo;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.AvisosDelPeriodo;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.util.Map;

/**
 * Cambiar el estado de un periodo. Si está en Planificación, primero se pregunta a Normativa si
 * tiene versión, fuera de la transacción de Académico (principio V); después se bloquea el periodo,
 * se cambia y se registra en una sola transacción.
 *
 * <p>La respuesta de Normativa sigue valiendo dentro de la transacción: los estados solo avanzan,
 * así que si el periodo bloqueado sigue en Planificación, ya lo estaba al preguntar; y si ya salió,
 * es porque tenía versión.
 */
public final class CambiarEstadoPeriodoServicio implements CambiarEstadoPeriodo {

    private final RepositorioPeriodos repositorio;
    private final NormativaDelPeriodo normativa;
    private final RegistroDeCambios registro;
    private final AvisosDelPeriodo avisos;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public CambiarEstadoPeriodoServicio(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registro,
            AvisosDelPeriodo avisos,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.normativa = normativa;
        this.registro = registro;
        this.avisos = avisos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public PeriodoConsultado cambiar(String codigo, EstadoPeriodo destino, String quien) {
        PeriodoAcademico leido =
                repositorio.porCodigo(codigo).orElseThrow(() -> new PeriodoNoEncontrado(codigo));
        boolean tieneVersion =
                leido.estado() != EstadoPeriodo.PLANIFICACION
                        || normativa.versionDe(codigo).isPresent();
        PeriodoAcademico cambiado =
                transaccion.ejecutar(
                        () -> {
                            PeriodoAcademico actual =
                                    repositorio
                                            .bloquear(codigo)
                                            .orElseThrow(() -> new PeriodoNoEncontrado(codigo));
                            PeriodoAcademico nuevo = actual.conEstado(destino, tieneVersion);
                            if (nuevo.equals(actual)) {
                                return actual;
                            }
                            repositorio.guardar(nuevo);
                            registro.registrar(
                                    new CambioRegistrado(
                                            reloj.instant(),
                                            quien,
                                            Accion.PERIODO_ESTADO_CAMBIADO,
                                            Map.of(
                                                    "codigo", codigo,
                                                    "anterior", actual.estado().name(),
                                                    "nuevo", nuevo.estado().name())));
                            avisos.actualizado(nuevo);
                            return nuevo;
                        });
        return new PeriodoConsultado(cambiado, normativa.versionDe(codigo));
    }
}
