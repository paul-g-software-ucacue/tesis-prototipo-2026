package ec.mdd.docentes.dominio;

/**
 * Estado de un proyecto aprobado (carga 9). Solo uno aprobado o en ejecución recibe horas; ninguno
 * se borra.
 */
public enum EstadoDeProyecto {
    APROBADO,
    EN_EJECUCION,
    FINALIZADO,
    SUSPENDIDO;

    /** Si un proyecto en este estado puede recibir horas (RF-18). */
    public boolean recibeHoras() {
        return this == APROBADO || this == EN_EJECUCION;
    }
}
