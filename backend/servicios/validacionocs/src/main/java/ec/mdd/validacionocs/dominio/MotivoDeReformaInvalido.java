package ec.mdd.validacionocs.dominio;

/**
 * El motivo de una reforma no tiene de 10 a 500 caracteres sin los espacios de los extremos
 * (F18-H02, D-113). Lo traduce el catálogo de errores.
 */
public final class MotivoDeReformaInvalido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public MotivoDeReformaInvalido() {
        super("MotivoDeReformaInvalido");
    }
}
