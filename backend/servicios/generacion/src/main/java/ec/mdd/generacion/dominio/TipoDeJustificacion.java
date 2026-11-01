package ec.mdd.generacion.dominio;

/** Por qué se hizo una asignación (RNF-EQU-01). */
public enum TipoDeJustificacion {
    /** El docente alcanza el mínimo de afinidad. */
    AFINIDAD,
    /** No lo alcanza, pero la asignatura es de tronco común: está exenta (RN-19). */
    DISPONIBILIDAD,
    /** No lo alcanza y no hay exención: lleva un texto. */
    EXCEPCION
}
