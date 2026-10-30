package ec.mdd.docentes.puertos.entrada;

/**
 * Caso de uso: si un periodo tiene proyectos asignables (F01-H05). Lo usa Cargas para la
 * confirmación «sin proyectos», sin conocer los proyectos ni el dominio de Docentes (principio V).
 */
public interface ConsultarAsignablesDelPeriodo {

    /**
     * Si el periodo tiene al menos un proyecto aprobado o en ejecución cuya vigencia se cruza con
     * sus fechas (F03-H05).
     *
     * @param periodo el código de un periodo de Académico
     */
    AsignablesDelPeriodo asignablesEn(String periodo);
}
