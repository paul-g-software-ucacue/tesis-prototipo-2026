package ec.mdd.validacionocs.dominio;

/** Qué movió una transición del flujo (F12-H02). */
public enum TipoDeTransicion {
    /** Una carrera envió su parcial; el distributivo sigue en BORRADOR_CARRERA. */
    ENVIO_PARCIAL,
    /** El distributivo pasó a otra etapa. */
    PASO_DE_ETAPA
}
