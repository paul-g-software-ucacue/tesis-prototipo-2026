package ec.mdd.validacionocs.dominio;

/**
 * Aprobar el contenido, o enviar a revisión cuando eso congela la versión, con el distributivo
 * devuelto por un rechazo en firma y sin una versión nueva (F14-H04, D-105). Congelar otra vez la
 * misma versión pediría a Firma el documento rechazado; la versión nueva es de F12-H04. Lo traduce
 * el catálogo de errores.
 */
public final class RequiereVersionNueva extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public RequiereVersionNueva() {
        super("RequiereVersionNueva");
    }
}
