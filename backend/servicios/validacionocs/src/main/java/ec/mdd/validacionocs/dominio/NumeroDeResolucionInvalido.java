package ec.mdd.validacionocs.dominio;

/**
 * El número de la resolución del OCS, sin los espacios de los extremos, vacío o de más de 60
 * caracteres (F15-H01). Lo traduce el catálogo de errores.
 */
public final class NumeroDeResolucionInvalido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public NumeroDeResolucionInvalido() {
        super("NumeroDeResolucionInvalido");
    }
}
