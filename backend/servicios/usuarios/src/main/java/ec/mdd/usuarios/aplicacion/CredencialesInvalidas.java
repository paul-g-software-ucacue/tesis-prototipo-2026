package ec.mdd.usuarios.aplicacion;

/**
 * El inicio de sesión no se aceptó. No dice si falló el usuario, la contraseña o si el usuario está
 * inactivo, para no darle pistas a un atacante (principio XIII).
 */
public final class CredencialesInvalidas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error sin causa ni detalle. */
    public CredencialesInvalidas() {
        super("Credenciales inválidas");
    }
}
