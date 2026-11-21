package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una actividad no lectiva de un docente tal como quedó al congelar la versión (F20-H01, D-115).
 *
 * @param codigo el código de la actividad del catálogo
 * @param nombre su nombre
 * @param componente su componente; nunca {@link Componente#CLASE}
 * @param carrera la carrera del parcial que la registró
 * @param proyecto el código del proyecto; {@code null} si no tiene
 * @param nombreProyecto su nombre; {@code null} si no tiene
 * @param horas sus horas semanales
 */
public record ActividadCongelada(
        String codigo,
        String nombre,
        Componente componente,
        String carrera,
        String proyecto,
        String nombreProyecto,
        BigDecimal horas) {

    /** Exige los datos y horas no negativas. */
    public ActividadCongelada {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(componente, "componente");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(horas, "horas");
        if (componente == Componente.CLASE) {
            throw new IllegalArgumentException("una actividad no lectiva no es de clase");
        }
        if (horas.signum() < 0) {
            throw new IllegalArgumentException("horas negativas");
        }
    }
}
