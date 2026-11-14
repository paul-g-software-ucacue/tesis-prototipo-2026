package ec.mdd.firma.dominio;

/** El estado de la generación del documento firmable de una versión (F13-H02). */
public enum EstadoDeGeneracion {
    /** Pedida y todavía sin resultado. */
    EN_CURSO,
    /** Falló; se puede reintentar. */
    FALLIDA,
    /** El documento quedó registrado; no cambia. */
    GENERADA
}
