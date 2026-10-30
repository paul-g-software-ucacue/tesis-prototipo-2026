package ec.mdd.docentes.puertos.entrada;

/**
 * Si un periodo tiene proyectos que pueden recibir horas (F01-H05, glosario «Proyecto asignable»).
 */
public enum AsignablesDelPeriodo {
    /** El periodo no está en Académico. */
    PERIODO_NO_ENCONTRADO,
    /** Ningún proyecto es asignable en el periodo. */
    NINGUNO,
    /** Al menos un proyecto es asignable en el periodo. */
    ALGUNO
}
