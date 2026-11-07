package ec.mdd.usuarios.dominio;

/**
 * Un filtro o una página de la consulta de la bitácora que no cumple su regla (FR-010, principio
 * XIV). El adaptador de entrada la traduce a {@code COMUN_SOLICITUD_INVALIDA}, con un detalle en su
 * campo.
 *
 * <p>El motivo dice qué corregir, en español; nunca cita el valor recibido.
 */
public final class FiltroDeBitacoraInvalido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo el nombre del parámetro en el contrato, por ejemplo {@code hasta}
     * @param motivo qué corregir
     */
    public FiltroDeBitacoraInvalido(String campo, String motivo) {
        super(campo + ": " + motivo);
        this.campo = campo;
        this.motivo = motivo;
    }

    /** El parámetro que no cumple su regla. */
    public String campo() {
        return campo;
    }

    /** Qué corregir. */
    public String motivo() {
        return motivo;
    }
}
