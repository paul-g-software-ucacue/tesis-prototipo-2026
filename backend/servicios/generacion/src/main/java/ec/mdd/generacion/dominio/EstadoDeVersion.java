package ec.mdd.generacion.dominio;

/** El estado del contenido de una versión del distributivo (RF-59). */
public enum EstadoDeVersion {
    /** Admite cambios. */
    EDITABLE,
    /** Inmutable: sobre ella se firma. */
    CONGELADA,
    /** Reemplazada por otra. */
    ANULADA
}
