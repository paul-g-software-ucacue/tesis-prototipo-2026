package ec.mdd.validacionocs.dominio;

/**
 * Se devuelve solo un parcial enviado (F12-H05). Lleva la carrera, para nombrarla en el detalle del
 * error. Responde {@code VALIDACIONOCS_PARCIAL_NO_ENVIADO}.
 */
public final class ParcialNoEnviado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String carrera;

    /**
     * @param carrera la carrera de la devolución
     */
    public ParcialNoEnviado(String carrera) {
        super("ParcialNoEnviado");
        this.carrera = carrera;
    }

    /** La carrera de la devolución. */
    public String carrera() {
        return carrera;
    }
}
