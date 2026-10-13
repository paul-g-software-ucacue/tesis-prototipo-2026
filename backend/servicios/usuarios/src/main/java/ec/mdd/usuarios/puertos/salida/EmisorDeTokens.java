package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;

/** Firma el token de un usuario que inició sesión (ADR-014). */
public interface EmisorDeTokens {

    /** Emite un token con el nombre del usuario, sus roles y su vencimiento. */
    TokenEmitido emitir(Usuario usuario);
}
