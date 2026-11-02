package ec.mdd.generacion.dominio;

/** Las reglas duras que se evalúan al guardar una asignación (RF-28). */
public enum ReglaDeAsignacion {
    /** El total del docente no supera su límite. */
    RN_02,
    /** Un docente con proyecto no supera el tope de horas de clase. */
    RN_13
}
