package ec.mdd.validacionocs.dominio;

/** El PDF de la resolución del OCS pasa de 10 MB (F15-H01). Lo traduce el catálogo de errores. */
public final class ResolucionDemasiadoGrande extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ResolucionDemasiadoGrande() {
        super("ResolucionDemasiadoGrande");
    }
}
