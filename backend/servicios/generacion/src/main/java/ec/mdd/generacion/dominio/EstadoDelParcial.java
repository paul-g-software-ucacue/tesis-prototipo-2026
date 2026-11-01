package ec.mdd.generacion.dominio;

/** El estado del parcial de una carrera (RF-24). */
public enum EstadoDelParcial {
    /** La carrera lo está armando. */
    EN_ELABORACION,
    /** Enviado: no se edita hasta que se devuelve. */
    ENVIADO,
    /** Devuelto a la carrera. */
    DEVUELTO
}
