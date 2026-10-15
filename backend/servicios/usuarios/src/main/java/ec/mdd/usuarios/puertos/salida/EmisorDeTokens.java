package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;

/** Firma el token de un usuario que inició sesión (ADR-014) o el de un proceso (ADR-018). */
public interface EmisorDeTokens {

    /** Emite un token con el nombre del usuario, sus roles y su vencimiento. */
    TokenEmitido emitir(Usuario usuario);

    /**
     * Emite un token de proceso: {@code sub} = {@code proceso:<proceso>}, el rol {@code PROCESO} y
     * una vida más corta que la de un usuario (D-49, ADR-018).
     */
    TokenEmitido emitirParaProceso(String proceso);
}
