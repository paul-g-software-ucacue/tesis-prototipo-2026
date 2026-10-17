package ec.mdd.plataforma.errores.fixtures.dominio;

/** Error de prueba con un dato que el catálogo convierte en un encabezado. */
public final class EsperaParaReintentar extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long segundos;

    public EsperaParaReintentar(long segundos) {
        super("Hay que esperar");
        this.segundos = segundos;
    }

    public long segundos() {
        return segundos;
    }
}
