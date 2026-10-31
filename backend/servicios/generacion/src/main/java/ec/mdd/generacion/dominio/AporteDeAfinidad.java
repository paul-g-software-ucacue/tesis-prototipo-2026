package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una línea del desglose del puntaje: qué título, experiencia o curso aportó y cuánto (RF-17).
 *
 * @param tipo de qué parte de la ficha sale
 * @param nombre el título, el cargo o el nombre del curso
 * @param coincidencia qué campo coincide
 * @param aporte lo que suma; mayor que 0
 */
public record AporteDeAfinidad(
        TipoDeAporte tipo, String nombre, CoincidenciaDeCampo coincidencia, BigDecimal aporte) {

    /**
     * Exige los cuatro datos.
     *
     * @throws IllegalArgumentException si el aporte no es mayor que 0
     */
    public AporteDeAfinidad {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(coincidencia, "coincidencia");
        Objects.requireNonNull(aporte, "aporte");
        if (aporte.signum() <= 0) {
            throw new IllegalArgumentException("Un aporte al puntaje es mayor que 0");
        }
    }
}
