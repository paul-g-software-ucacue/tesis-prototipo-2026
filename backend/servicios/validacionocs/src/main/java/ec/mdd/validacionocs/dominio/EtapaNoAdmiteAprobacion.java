package ec.mdd.validacionocs.dominio;

/**
 * Aprobar el contenido con el distributivo fuera de EN_REVISION_VRA, también la segunda de dos
 * aprobaciones simultáneas (F13-H01, D-100); lo traduce el catálogo de errores.
 */
public final class EtapaNoAdmiteAprobacion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteAprobacion() {
        super("EtapaNoAdmiteAprobacion");
    }
}
