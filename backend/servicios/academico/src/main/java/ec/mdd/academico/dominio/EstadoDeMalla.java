package ec.mdd.academico.dominio;

/**
 * Estado de una malla (F03-H02, aclaración 2): una derogada conserva sus asignaturas, pero no
 * admite nuevas.
 */
public enum EstadoDeMalla {
    /** Admite asignaturas nuevas. */
    VIGENTE,
    /** Reemplazada; sus asignaturas se conservan. */
    DEROGADA
}
