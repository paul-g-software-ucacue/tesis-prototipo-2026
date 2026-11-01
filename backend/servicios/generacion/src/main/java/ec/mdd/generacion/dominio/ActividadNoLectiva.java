package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Horas de un docente en una actividad del catálogo, en el parcial de una carrera (RF-26). Junto
 * con la propuesta, es una asignación.
 *
 * @param carrera la carrera dueña del parcial
 * @param docente su número de identificación
 * @param nombreDocente sus apellidos y nombres
 * @param actividad el código de la actividad
 * @param nombreActividad el texto de la actividad
 * @param componente dónde cuentan sus horas
 * @param proyecto el código del proyecto; {@code null} si no lo pide
 * @param nombreProyecto el nombre del proyecto; {@code null} si no lo pide
 * @param horas sus horas semanales
 * @param asignadaEn cuándo
 * @param asignadaPor quién
 */
public record ActividadNoLectiva(
        String carrera,
        String docente,
        String nombreDocente,
        String actividad,
        String nombreActividad,
        Componente componente,
        String proyecto,
        String nombreProyecto,
        BigDecimal horas,
        Instant asignadaEn,
        String asignadaPor) {

    private static final BigDecimal TOPE_DE_HORAS = new BigDecimal("40");
    private static final int DECIMALES = 2;

    /** Exige los datos, y unas horas mayores que 0, hasta 40, con hasta dos decimales. */
    public ActividadNoLectiva {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(nombreDocente, "nombreDocente");
        Objects.requireNonNull(actividad, "actividad");
        Objects.requireNonNull(nombreActividad, "nombreActividad");
        Objects.requireNonNull(componente, "componente");
        Objects.requireNonNull(horas, "horas");
        Objects.requireNonNull(asignadaEn, "asignadaEn");
        Objects.requireNonNull(asignadaPor, "asignadaPor");
        if (horas.signum() <= 0
                || horas.compareTo(TOPE_DE_HORAS) > 0
                || horas.stripTrailingZeros().scale() > DECIMALES) {
            throw new HorasNoValidas();
        }
    }

    /** Si es esta misma actividad del parcial: la llave incluye el proyecto. */
    public boolean esDe(String otraCarrera, String otroDocente, String otra, String otroProyecto) {
        return carrera.equals(otraCarrera)
                && docente.equals(otroDocente)
                && actividad.equals(otra)
                && Objects.equals(proyecto, otroProyecto);
    }
}
