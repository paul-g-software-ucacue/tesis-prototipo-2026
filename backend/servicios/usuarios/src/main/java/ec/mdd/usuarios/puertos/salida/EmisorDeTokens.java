package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.Instant;

/** Firma el token de un usuario que inició sesión (ADR-014) o el de un proceso (ADR-018). */
public interface EmisorDeTokens {

    /**
     * Emite un token con el nombre del usuario, su nombre para mostrar, sus roles y permisos, el
     * inicio de la sesión y su vencimiento (ADR-020, ADR-021).
     *
     * @param usuario el usuario de la sesión
     * @param autorizacion sus roles y permisos, de Roles
     * @param inicio cuándo inició sesión; se conserva al renovar
     */
    TokenEmitido emitir(Usuario usuario, AutorizacionDeUsuario autorizacion, Instant inicio);

    /**
     * Emite un token de proceso: {@code sub} = {@code proceso:<proceso>}, el rol {@code PROCESO} y
     * una vida más corta que la de un usuario (D-49, ADR-018).
     */
    TokenEmitido emitirParaProceso(String proceso);
}
