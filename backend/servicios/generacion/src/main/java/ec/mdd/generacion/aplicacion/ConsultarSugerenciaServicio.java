package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.HorasAsignadas;
import java.util.Optional;

/**
 * Arma la sugerencia en cada consulta, así que siempre refleja las fichas de ese momento; nada se
 * guarda (FR-022). Pide los datos uno a uno y el primero que falla corta: nunca hay una lista
 * parcial.
 */
public final class ConsultarSugerenciaServicio implements ConsultarSugerencia {

    private final DatosParaSugerencia datos;
    private final DatosParaAfinidad afinidad;
    private final HorasAsignadas horas;

    /** Recibe los datos de los otros servicios y las horas asignadas. */
    public ConsultarSugerenciaServicio(
            DatosParaSugerencia datos, DatosParaAfinidad afinidad, HorasAsignadas horas) {
        this.datos = datos;
        this.afinidad = afinidad;
        this.horas = horas;
    }

    @Override
    public Sugerencia sugerencia(
            String periodo, String asignatura, String paralelo, Optional<String> carrera) {
        AsignaturaParalelo uno =
                datos.paralelo(periodo, asignatura, paralelo)
                        .orElseThrow(ParaleloNoEncontrado::new);
        if (carrera.isPresent() && !carrera.get().equals(uno.carrera())) {
            throw new OtraCarrera();
        }
        if (uno.retirada()) {
            throw new ParaleloRetirado();
        }
        PesosDelPeriodo pesos = afinidad.pesos(periodo).orElseThrow(PeriodoSinAbrir::new);
        return Sugerencia.armar(
                periodo,
                uno,
                pesos,
                datos.docentesActivos(),
                horas.delPeriodo(periodo, uno.carrera()));
    }
}
