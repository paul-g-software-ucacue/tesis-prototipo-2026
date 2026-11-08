package ec.mdd.validacionocs.dominio;

/**
 * Sin los espacios de los extremos, la observación no tiene de 10 a 500 caracteres (RF-36,
 * F12-H05). Lleva la carrera, para nombrarla en el detalle del error. Responde {@code
 * VALIDACIONOCS_OBSERVACION_INVALIDA}.
 */
public final class ObservacionInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String carrera;

    /**
     * @param carrera la carrera de la devolución
     */
    public ObservacionInvalida(String carrera) {
        super("ObservacionInvalida");
        this.carrera = carrera;
    }

    /** La carrera de la devolución. */
    public String carrera() {
        return carrera;
    }
}
