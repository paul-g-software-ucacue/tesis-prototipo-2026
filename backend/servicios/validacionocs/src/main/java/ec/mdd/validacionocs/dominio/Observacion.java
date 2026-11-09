package ec.mdd.validacionocs.dominio;

/**
 * El texto que dice qué tiene que corregir quien recibe una devolución (glosario §5, RF-36). Se
 * guarda sin los espacios de los extremos, y así tiene de 10 a 500 caracteres, como la
 * justificación de una blanda (F10-H01).
 *
 * @param texto la observación, sin los espacios de los extremos
 */
public record Observacion(String texto) {

    /** El mínimo de caracteres. */
    public static final int MINIMO = 10;

    /** El máximo de caracteres. */
    public static final int MAXIMO = 500;

    /**
     * Quita los espacios de los extremos y exige la longitud.
     *
     * @throws IllegalArgumentException si falta o no tiene de 10 a 500 caracteres
     */
    public Observacion {
        texto = texto == null ? "" : texto.strip();
        if (texto.length() < MINIMO || texto.length() > MAXIMO) {
            throw new IllegalArgumentException("La observación tiene de 10 a 500 caracteres");
        }
    }
}
