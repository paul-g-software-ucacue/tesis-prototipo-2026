package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

/**
 * Corregir el nombre y las fechas de un periodo: se bloquea, se corrige y, si algo cambió, se
 * guarda y se registra en la misma transacción. Sin cambios, no se registra.
 */
public final class CorregirPeriodoServicio implements CorregirPeriodo {

    private final RepositorioPeriodos repositorio;
    private final NormativaDelPeriodo normativa;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public CorregirPeriodoServicio(
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
    public PeriodoConsultado corregir(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien) {
        PeriodoAcademico corregido =
                transaccion.ejecutar(
                        () -> {
                            PeriodoAcademico.Correccion correccion =
                                    repositorio
                                            .bloquear(codigo)
                                            .orElseThrow(() -> new PeriodoNoEncontrado(codigo))
                                            .corregido(nombre, inicio, fin);
                            if (!correccion.cambios().isEmpty()) {
                                repositorio.guardar(correccion.periodo());
                                registro.registrar(
                                        new CambioRegistrado(
                                                reloj.instant(),
                                                quien,
                                                Accion.PERIODO_CORREGIDO,
                                                Map.of(
                                                        "codigo",
                                                        codigo,
                                                        "cambios",
                                                        correccion.cambios().stream()
                                                                .map(Detalles::de)
                                                                .toList())));
                            }
                            return correccion.periodo();
                        });
        return new PeriodoConsultado(corregido, normativa.versionDe(codigo));
    }
}
