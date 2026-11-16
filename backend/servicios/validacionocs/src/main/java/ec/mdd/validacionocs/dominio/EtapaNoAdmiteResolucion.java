package ec.mdd.validacionocs.dominio;

/**
 * Registrar la resolución del OCS con el distributivo fuera de EN_OCS, también la segunda de dos a
 * la vez (F15-H01, D-107). Lo traduce el catálogo de errores.
 */
public final class EtapaNoAdmiteResolucion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteResolucion() {
        super("EtapaNoAdmiteResolucion");
    }
}
