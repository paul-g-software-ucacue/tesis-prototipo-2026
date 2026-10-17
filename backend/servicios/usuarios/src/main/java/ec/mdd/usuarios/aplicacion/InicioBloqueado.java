package ec.mdd.usuarios.aplicacion;

/**
 * El nombre de usuario está bloqueado por intentos fallidos seguidos (D-60). No dice si el usuario
 * existe; solo cuánto falta, para el encabezado {@code Retry-After}.
 */
public final class InicioBloqueado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long segundosRestantes;

    /**
     * @param segundosRestantes segundos que faltan para que termine el bloqueo; al menos 1
     */
    public InicioBloqueado(long segundosRestantes) {
        super("Inicio de sesión bloqueado");
        this.segundosRestantes = segundosRestantes;
    }

    /** Segundos que faltan para que termine el bloqueo. */
    public long segundosRestantes() {
        return segundosRestantes;
    }
}
