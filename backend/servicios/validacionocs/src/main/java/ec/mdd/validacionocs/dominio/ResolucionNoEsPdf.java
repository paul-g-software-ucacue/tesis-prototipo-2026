package ec.mdd.validacionocs.dominio;

/**
 * El archivo de la resolución del OCS está vacío o, por su contenido, no es un PDF (F15-H01). Sus
 * firmas no se miran (D-19). Lo traduce el catálogo de errores.
 */
public final class ResolucionNoEsPdf extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ResolucionNoEsPdf() {
        super("ResolucionNoEsPdf");
    }
}
