package ec.mdd.validacionocs.dominio;

/**
 * El avance genérico desde EN_OCS (F15-H01, D-107): a APROBADO solo se pasa registrando la
 * resolución del OCS (RF-37). Lo traduce el catálogo de errores.
 */
public final class RequiereResolucion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public RequiereResolucion() {
        super("RequiereResolucion");
    }
}
