package ec.mdd.generacion.dominio;

import java.math.BigDecimal;

/**
 * Las horas ya asignadas a un docente en un periodo. Admiten decimales, como las de la oferta.
 *
 * @param asignadas todas, en todas las carreras y unidades
 * @param enOtrasCarreras las que no son de la carrera de la asignatura-paralelo (D-01)
 */
public record HorasDelDocente(BigDecimal asignadas, BigDecimal enOtrasCarreras) {

    /** Un docente sin asignaciones. */
    public static final HorasDelDocente NINGUNA =
            new HorasDelDocente(BigDecimal.ZERO, BigDecimal.ZERO);

    /** Sus horas contratadas menos las asignadas, nunca menos de 0 (D-75). */
    public BigDecimal disponibles(int horasContratadas) {
        return BigDecimal.valueOf(horasContratadas).subtract(asignadas).max(BigDecimal.ZERO);
    }

    /** Exige horas no negativas, y que las de otras carreras no pasen de las asignadas. */
    public HorasDelDocente {
        if (asignadas.signum() < 0 || enOtrasCarreras.signum() < 0) {
            throw new IllegalArgumentException("Las horas no pueden ser negativas");
        }
        if (enOtrasCarreras.compareTo(asignadas) > 0) {
            throw new IllegalArgumentException(
                    "Las horas en otras carreras no pueden pasar de las asignadas");
        }
    }
}
