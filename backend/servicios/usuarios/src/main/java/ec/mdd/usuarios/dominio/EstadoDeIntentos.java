package ec.mdd.usuarios.dominio;

import java.time.Instant;

/**
 * Los intentos fallidos seguidos de un nombre de usuario, exista o no (D-60).
 *
 * @param fallidos intentos fallidos desde el último correcto o el último bloqueo
 * @param bloqueadoHasta hasta cuándo está bloqueado; {@code null} si no lo está
 */
public record EstadoDeIntentos(int fallidos, Instant bloqueadoHasta) {

    /** Un nombre sin intentos fallidos. */
    public static final EstadoDeIntentos SIN_INTENTOS = new EstadoDeIntentos(0, null);
}
