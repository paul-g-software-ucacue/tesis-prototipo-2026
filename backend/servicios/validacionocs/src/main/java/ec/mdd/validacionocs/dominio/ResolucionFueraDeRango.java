package ec.mdd.validacionocs.dominio;

/**
 * La fecha de la resolución del OCS es futura o anterior al día en que el distributivo llegó a
 * EN_OCS, en la zona de la institución (F15-H01, D-107). Lo traduce el catálogo de errores.
 */
public final class ResolucionFueraDeRango extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ResolucionFueraDeRango() {
        super("ResolucionFueraDeRango");
    }
}
