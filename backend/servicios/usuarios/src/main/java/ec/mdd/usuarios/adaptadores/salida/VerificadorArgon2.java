package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Contraseñas con Argon2id (constitución §3, RNF-SEG-02). */
public final class VerificadorArgon2 implements VerificadorDeClaves {

    private final PasswordEncoder argon2;

    /**
     * @param argon2 codificador Argon2id configurado por la infraestructura
     */
    public VerificadorArgon2(PasswordEncoder argon2) {
        this.argon2 = argon2;
    }

    @Override
    public String calcularHash(String clave) {
        return argon2.encode(clave);
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return argon2.matches(clave, hash);
    }
}
