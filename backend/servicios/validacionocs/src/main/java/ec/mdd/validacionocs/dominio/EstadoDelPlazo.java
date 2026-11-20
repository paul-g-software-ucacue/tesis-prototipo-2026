package ec.mdd.validacionocs.dominio;

/** Cómo va el plazo de la etapa actual en un día (F12-H03, D-114). */
public enum EstadoDelPlazo {
    /** Falta al menos un día para la fecha límite. */
    A_TIEMPO,
    /** Hoy es la fecha límite: todavía no venció. */
    VENCE_HOY,
    /** Ya pasó la fecha límite. */
    VENCIDO
}
