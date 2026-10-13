package ec.mdd.plataforma.errores.fixtures.dominio;

/** Error de dominio de prueba que nadie registró en el catálogo. */
public final class ReglaSinCatalogo extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaSinCatalogo() {
        super("Regla de prueba sin código");
    }
}
