package ec.mdd.validacionocs.dominio;

/** Los documentos que se pueden firmar (RF-58). El prototipo firma el consolidado. */
public enum DocumentoDeFirma {
    /** El distributivo institucional. */
    CONSOLIDADO,
    /** El distributivo parcial de una carrera. */
    PARCIAL_CARRERA,
    /** La resolución del OCS. */
    RESOLUCION_OCS
}
