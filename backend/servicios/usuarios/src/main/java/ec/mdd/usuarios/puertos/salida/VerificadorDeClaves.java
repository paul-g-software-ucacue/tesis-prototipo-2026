package ec.mdd.usuarios.puertos.salida;

/** Hash y verificación de contraseñas con un algoritmo de derivación (Argon2id). */
public interface VerificadorDeClaves {

    /** Calcula el hash de una contraseña, con una sal nueva cada vez. */
    String calcularHash(String clave);

    /**
     * Comprueba una contraseña contra su hash. Tarda lo mismo aunque no coincida, así que también
     * sirve para igualar el tiempo de respuesta cuando el usuario no existe.
     */
    boolean coincide(String clave, String hash);
}
