package ec.mdd.usuarios.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class VerificadorArgon2Test {

    private final VerificadorArgon2 verificador =
            new VerificadorArgon2(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

    @Test
    void dadoUnaContrasena_cuandoSeCalculaSuHash_entoncesEsArgon2idYLaVerifica() {
        String hash = verificador.calcularHash("una-clave-de-prueba");

        assertThat(hash).startsWith("$argon2id$").doesNotContain("una-clave-de-prueba");
        assertThat(verificador.coincide("una-clave-de-prueba", hash)).isTrue();
        assertThat(verificador.coincide("otra-clave", hash)).isFalse();
    }
}
