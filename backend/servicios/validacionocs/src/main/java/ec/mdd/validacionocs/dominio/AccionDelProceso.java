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
    DEVOLVER_PARCIALES,
    /** Aprobar el contenido y congelar la versión, desde EN_REVISION_VRA (F13-H01). */
    APROBAR_CONTENIDO,
    /** Registrar la resolución del OCS y aprobar el distributivo, desde EN_OCS (F15-H01). */
    REGISTRAR_RESOLUCION,
    /** Publicar el distributivo aprobado, desde APROBADO (F18-H01). */
    PUBLICAR,
    /** Iniciar una reforma del distributivo publicado, desde PUBLICADO (F18-H02). */
    INICIAR_REFORMA
}
