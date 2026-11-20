package ec.mdd.validacionocs.dominio;

/**
 * Iniciar una reforma con el distributivo fuera de PUBLICADO, también en la segunda de dos reformas
 * a la vez (F18-H02, D-113). Lo traduce el catálogo de errores.
 */
public final class EtapaNoAdmiteReforma extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteReforma() {
        super("EtapaNoAdmiteReforma");
    }
}
