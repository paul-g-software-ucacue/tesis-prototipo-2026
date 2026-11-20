package ec.mdd.validacionocs.dominio;

/** Qué movió una transición del flujo (F12-H02). */
public enum TipoDeTransicion {
    /** Una carrera envió su parcial; el distributivo sigue en BORRADOR_CARRERA. */
    ENVIO_PARCIAL,
    /** El distributivo pasó a otra etapa. */
    PASO_DE_ETAPA,
    /**
     * El distributivo volvió de EN_CONSOLIDACION a BORRADOR_CARRERA, con parciales devueltos y la
     * observación de cada uno (F12-H05, D-98).
     */
    DEVOLUCION,
    /**
     * El distributivo volvió de CONGELADO_EN_FIRMA a la etapa activa anterior porque el firmante en
     * turno rechazó en firma; la registra el sistema, con el rechazo (F14-H04, D-105).
     */
    RECHAZO_EN_FIRMA,
    /**
     * Después de un rechazo en firma, Generación anuló la versión rechazada y creó la siguiente; el
     * distributivo sigue en la etapa a la que volvió. La registra el sistema, con la versión nueva
     * y su hash (F12-H04, D-106).
     */
    VERSION_NUEVA,

    /** El Vicerrector inició una reforma del distributivo publicado (F18-H02, D-113). */
    REFORMA
}
