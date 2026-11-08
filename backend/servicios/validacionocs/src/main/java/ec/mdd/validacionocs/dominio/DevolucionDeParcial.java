package ec.mdd.validacionocs.dominio;

import java.util.Objects;

/**
 * Una carrera de una devolución, con su observación (F12-H05, D-98).
 *
 * @param carrera la carrera cuyo parcial se devuelve
 * @param observacion lo que tiene que corregir
 */
public record DevolucionDeParcial(String carrera, Observacion observacion) {

    /** Exige los datos. */
    public DevolucionDeParcial {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(observacion, "observacion");
    }

    /**
     * La devolución de una carrera, a partir del texto que llegó.
     *
     * @param carrera la carrera
     * @param texto la observación tal como llegó
     * @return la devolución
     * @throws ObservacionInvalida con la carrera, si la observación no tiene de 10 a 500 caracteres
     *     sin los espacios de los extremos
     */
    public static DevolucionDeParcial de(String carrera, String texto) {
        try {
            return new DevolucionDeParcial(carrera, new Observacion(texto));
        } catch (IllegalArgumentException invalida) {
            throw new ObservacionInvalida(carrera);
        }
    }
}
