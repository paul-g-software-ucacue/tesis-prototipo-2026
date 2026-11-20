package ec.mdd.validacionocs.dominio;

/**
 * Publicar el distributivo fuera de APROBADO, también si ya está publicado o en la segunda de dos
 * publicaciones a la vez (F18-H01, D-111). Lo traduce el catálogo de errores.
 */
public final class EtapaNoAdmitePublicacion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmitePublicacion() {
        super("EtapaNoAdmitePublicacion");
    }
}
