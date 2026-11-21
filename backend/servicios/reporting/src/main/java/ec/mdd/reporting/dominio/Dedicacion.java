package ec.mdd.reporting.dominio;

/** La dedicación de un docente (RN-01). */
public enum Dedicacion {
    /** Tiempo completo. */
    TC("Tiempo completo"),
    /** Medio tiempo. */
    MT("Medio tiempo"),
    /** Tiempo parcial. */
    TP("Tiempo parcial");

    private final String etiqueta;

    Dedicacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** El nombre que ve el usuario. */
    public String etiqueta() {
        return etiqueta;
    }
}
