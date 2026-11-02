package ec.mdd.generacion.dominio;

/** Lo que el panel de inconsistencias comprueba (RF-29, D-85, D-86). */
public enum TipoDeInconsistencia {
    /** Un docente activo sin horas de clase. */
    DOCENTE_SIN_CLASE("RN-17"),
    /** Una asignatura-paralelo sin propuesta. */
    PARALELO_SIN_PROPUESTA("RF-24"),
    /** Una carrera ofertada sin exactamente un coordinador. */
    COORDINADORES_DE_LA_CARRERA("RN-12"),
    /** El número de docentes con cargo de gestión fuera del rango. */
    GESTORES_FUERA_DE_RANGO("RN-10"),
    /** Paralelos con horas sobre las de su asignatura en la malla. */
    HORAS_SOBRE_LA_MALLA("RN-18"),
    /** Un docente sobre su límite de horas. */
    SOBRECARGA("RN-02"),
    /** Un docente fuera del margen de 60/40. */
    PROPORCION("RN-05"),
    /** Un docente con clase sin la planificación o la evaluación. */
    ACTIVIDADES_OBLIGATORIAS("RN-05"),
    /** Una propuesta bajo el mínimo, justificada en la propuesta. */
    EXCEPCION_DE_AFINIDAD("RN-19");

    private final String regla;

    TipoDeInconsistencia(String regla) {
        this.regla = regla;
    }

    /** El identificador de su regla. */
    public String regla() {
        return regla;
    }

    /** Si la evalúa el panel: la excepción de RN-19 solo se lee de la propuesta. */
    public boolean evalua() {
        return this != EXCEPCION_DE_AFINIDAD;
    }
}
