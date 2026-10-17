package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;
import java.util.List;

/**
 * Caso de uso: renovar el token de una sesión vigente mientras el usuario trabaja (RNF-SEG-07,
 * ADR-020).
 *
 * <p>El token nuevo conserva el instante del inicio de sesión. Pasado el máximo de la sesión (8
 * horas), o si el token es de un proceso o el usuario ya no está activo, lanza {@code
 * SesionNoRenovable}.
 */
public interface RenovarSesion {

    /**
     * Renueva la sesión de un token ya validado.
     *
     * @param nombreUsuario el {@code sub} del token
     * @param roles los roles del token
     * @param inicio el claim {@code inicio} del token; {@code null} si no lo tiene
     * @return el token nuevo y su vencimiento
     */
    TokenEmitido renovar(String nombreUsuario, List<String> roles, Instant inicio);
}
