package ec.mdd.normativa.dominio;

/**
 * Una fila de la tabla de carga horaria que no cumple sus invariantes (RN-03 a RN-15): horas
 * negativas, horas de clase fuera de su mínimo y su máximo, o una suma mayor que la dedicación.
 *
 * <p>Hereda de {@link IllegalArgumentException}, como las demás invariantes del agregado. Además
 * del mensaje, que nombra el perfil, lleva aparte el perfil, el tipo de IST y el motivo, para que
 * la respuesta marque la fila con un texto en español sin códigos internos (F04-H02).
 */
public class FilaInvalida extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String perfil;
    private final TipoIst aplicaA;
    private final String motivo;

    /**
     * @param perfil el perfil de la fila
     * @param aplicaA el tipo de IST de la fila, o {@code null} si aplica a todos
     * @param motivo qué está mal, en español y sin el perfil
     */
    public FilaInvalida(String perfil, TipoIst aplicaA, String motivo) {
        super("La fila de " + perfil + " no es válida: " + motivo);
        this.perfil = perfil;
        this.aplicaA = aplicaA;
        this.motivo = motivo;
    }

    /** El perfil de la fila. */
    public String perfil() {
        return perfil;
    }

    /** El tipo de IST de la fila, o {@code null} si aplica a todos. */
    public TipoIst aplicaA() {
        return aplicaA;
    }

    /** Qué está mal, en español. */
    public String motivo() {
        return motivo;
    }
}
