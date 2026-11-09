package ec.mdd.validacionocs.dominio;

/**
 * El envío del parcial de una carrera, para Aprobación (F12-H02). {@code PENDIENTE} equivale a «en
 * elaboración» en Generación, y {@code DEVUELTO}, a «devuelto» (F12-H05).
 */
public enum EstadoDeEnvio {
    /** Todavía no se envió. */
    PENDIENTE,
    /** Enviado, con su transición registrada. */
    ENVIADO,
    /** Devuelto a su carrera con una observación; se corrige y se reenvía (F12-H05). */
    DEVUELTO
}
