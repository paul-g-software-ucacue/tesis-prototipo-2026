package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;

/**
 * Token firmado que recibe quien inicia sesión.
 *
 * @param valor el JWT serializado
 * @param venceEn cuándo vence
 */
public record TokenEmitido(String valor, Instant venceEn) {

    /** No incluye el token, para que no termine en un registro. */
    @Override
    public String toString() {
        return "TokenEmitido[venceEn=" + venceEn + "]";
    }
}
