package ec.mdd.docentes.dominio;

/**
 * Estado laboral de un docente (glosario «Estado laboral», carga 6). Solo un docente activo recibe
 * asignaciones; ninguno se borra.
 */
public enum EstadoLaboral {
    ACTIVO,
    LICENCIA,
    INACTIVO
}
