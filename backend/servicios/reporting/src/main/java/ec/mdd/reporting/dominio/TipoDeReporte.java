package ec.mdd.reporting.dominio;

/**
 * Qué reporte se descarga (F20-H01, F20-H02, D-117). Cada tipo tiene su acción en la bitácora, para
 * distinguir sin mirar el objeto quién bajó el distributivo completo y quién el de un docente.
 */
public enum TipoDeReporte {
    /** El reporte individual de un docente (RF-45). */
    INDIVIDUAL("REPORTE_INDIVIDUAL_DESCARGADO", "reporte-individual"),
    /** El reporte general consolidado de la institución (RF-46). */
    GENERAL("REPORTE_GENERAL_DESCARGADO", "reporte-general");

    private final String accion;
    private final String prefijo;

    TipoDeReporte(String accion, String prefijo) {
        this.accion = accion;
        this.prefijo = prefijo;
    }

    /** La acción de la bitácora de cada descarga. */
    public String accion() {
        return accion;
    }

    /** El comienzo del nombre del archivo, sin espacios. */
    String prefijo() {
        return prefijo;
    }
}
