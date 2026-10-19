package ec.mdd.normativa.dominio;

/**
 * La versión rige al menos un periodo, así que ya no cambia: cualquier ajuste crea una versión
 * nueva a partir de ella (RF-19, modelo de dominio §5.1, principio VII).
 */
public class VersionEnUso extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int numero;

    /**
     * @param numero el número de la versión
     */
    public VersionEnUso(int numero) {
        super("La versión de normativa " + numero + " rige un periodo y no se puede cambiar");
        this.numero = numero;
    }

    /** El número de la versión. */
    public int numero() {
        return numero;
    }
}
