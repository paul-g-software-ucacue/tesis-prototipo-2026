package ec.mdd.validacionocs.dominio;

/** Lo que un usuario puede hacer ahora con el distributivo (F12-H02, FR-014). */
public enum AccionDelProceso {
    /** Enviar el parcial de su carrera. */
    ENVIAR_PARCIAL,
    /** El avance genérico de una etapa opcional sin historia propia. */
    AVANZAR,
    /** Enviar el consolidado a revisión, desde EN_CONSOLIDACION (F10-H03). */
    ENVIAR_A_REVISION,
    /** Devolver parciales a sus carreras, desde EN_CONSOLIDACION (F12-H05). */
    DEVOLVER_PARCIALES
}
