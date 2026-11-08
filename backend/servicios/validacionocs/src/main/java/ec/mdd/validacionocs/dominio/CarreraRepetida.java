package ec.mdd.validacionocs.dominio;

/**
 * La misma carrera dos veces en una devolución (F12-H05). Lleva la carrera, para nombrarla en el
 * detalle del error. Responde {@code VALIDACIONOCS_CARRERA_REPETIDA}.
 */
public final class CarreraRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String carrera;

    /**
     * @param carrera la carrera de la devolución
     */
    public CarreraRepetida(String carrera) {
        super("CarreraRepetida");
        this.carrera = carrera;
    }

    /** La carrera de la devolución. */
    public String carrera() {
        return carrera;
    }
}
