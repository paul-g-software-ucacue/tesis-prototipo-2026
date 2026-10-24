package ec.mdd.academico.dominio;

/**
 * El estado de una asignatura-paralelo (glosario «Asignatura-paralelo», F03-H03). Ninguna se borra:
 * se retira y se puede volver a ofertar mientras el periodo está en Planificación (principio VII).
 */
public enum EstadoDeAsignaturaParalelo {
    /** Suma horas a cubrir y recibe propuestas. */
    OFERTADA,
    /** Se sigue viendo, pero no suma horas a cubrir ni recibe propuestas. */
    RETIRADA
}
