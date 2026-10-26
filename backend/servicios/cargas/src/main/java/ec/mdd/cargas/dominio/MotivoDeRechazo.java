package ec.mdd.cargas.dominio;

/** Por qué no se aplicó una confirmación (FR-003, FR-005, FR-006). */
public enum MotivoDeRechazo {
    /** El archivo tiene filas con errores. */
    CON_ERRORES,
    /** Los datos cambiaron entre la vista previa y la confirmación. */
    VISTA_PREVIA_DESACTUALIZADA,
    /** Una regla del servicio dueño rechazó una fila. */
    RECHAZADA_POR_EL_DUENO
}
