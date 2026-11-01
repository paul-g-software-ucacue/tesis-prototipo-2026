package ec.mdd.generacion.dominio;

/** Dónde cuentan las horas de un docente (RF-27). Las clases son las propuestas. */
public enum Componente {
    /** Horas de clase. */
    CLASE,
    /** Planificación, evaluación, tutorías, titulación y demás del art. 140 del RCE. */
    OTRAS_DOCENCIA,
    /** Actividades del art. 141 del RCE. */
    INVESTIGACION,
    /** Actividades del art. 142 del RCE. */
    VINCULACION,
    /** Actividades del art. 143 del RCE. */
    GESTION
}
