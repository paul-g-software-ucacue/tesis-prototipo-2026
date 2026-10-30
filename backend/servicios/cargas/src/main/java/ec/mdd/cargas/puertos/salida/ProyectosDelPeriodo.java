package ec.mdd.cargas.puertos.salida;

/**
 * Si un periodo tiene proyectos que pueden recibir horas (F01-H05, RF-18): la regla es de Docentes
 * (F03-H05, glosario «Proyecto asignable»); Cargas solo pregunta.
 */
public interface ProyectosDelPeriodo {

    /**
     * Si el periodo tiene al menos un proyecto aprobado o en ejecución cuya vigencia se cruza con
     * sus fechas.
     *
     * @throws PeriodoNoEncontrado si el periodo no está en los ciclos académicos
     */
    boolean hayAsignables(String periodo);
}
