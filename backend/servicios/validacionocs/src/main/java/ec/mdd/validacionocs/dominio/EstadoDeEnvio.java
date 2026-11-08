package ec.mdd.validacionocs.dominio;

/**
 * El envío del parcial de una carrera, para Aprobación (F12-H02). {@code PENDIENTE} equivale a «en
 * elaboración» o «devuelto» en Generación.
 */
public enum EstadoDeEnvio {
    /** Todavía no se envió. */
    PENDIENTE,
    /** Enviado, con su transición registrada. */
    ENVIADO
}
