package ec.mdd.cargas.puertos.salida;

/** El XLSX tiene más de una hoja (RF-07). */
public class VariasHojas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin detalles: el código del catálogo lo dice todo. */
    public VariasHojas() {
        super("El XLSX tiene más de una hoja");
    }
}
