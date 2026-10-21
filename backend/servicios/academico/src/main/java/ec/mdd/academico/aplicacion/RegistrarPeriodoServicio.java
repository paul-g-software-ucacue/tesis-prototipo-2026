package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Registrar un periodo: se guarda y se registra en la misma transacción. Un código repetido no se
 * guarda, sin una consulta previa que otra transacción podría adelantar.
 */
public final class RegistrarPeriodoServicio implements RegistrarPeriodo {

    private final RepositorioPeriodos repositorio;
    private final NormativaDelPeriodo normativa;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public RegistrarPeriodoServicio(
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
    public PeriodoConsultado registrar(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien) {
        PeriodoAcademico periodo = PeriodoAcademico.nuevo(codigo, nombre, inicio, fin);
        transaccion.ejecutar(
                () -> {
                    Instant ahora = reloj.instant();
                    if (!repositorio.registrar(periodo, quien, ahora)) {
                        throw new CodigoRepetido(codigo);
                    }
                    registro.registrar(
                            new CambioDePeriodo(
                                    ahora,
                                    quien,
                                    Accion.PERIODO_REGISTRADO,
                                    Map.of(
                                            "codigo", periodo.codigo(),
                                            "nombre", periodo.nombre(),
                                            "inicio", periodo.inicio().toString(),
                                            "fin", periodo.fin().toString())));
                    return null;
                });
        // Normativa no comprueba que el periodo exista (D-69): si alguien ya asoció su código, se
        // ve.
        return new PeriodoConsultado(periodo, normativa.versionDe(codigo));
    }
}
