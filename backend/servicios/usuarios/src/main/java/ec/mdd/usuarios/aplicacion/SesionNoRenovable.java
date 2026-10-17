package ec.mdd.usuarios.aplicacion;

/**
 * La sesión no se puede renovar: pasaron 8 horas desde el inicio, el token es de un proceso o el
 * usuario ya no existe o está inactivo (ADR-020). Responde como un token inválido, sin decir cuál.
 */
public final class SesionNoRenovable extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error sin detalle. */
    public SesionNoRenovable() {
        super("Sesión no renovable");
    }
}
