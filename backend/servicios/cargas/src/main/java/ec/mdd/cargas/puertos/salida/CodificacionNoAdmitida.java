package ec.mdd.cargas.puertos.salida;

/** El CSV no está en UTF-8 (RF-07). */
public class CodificacionNoAdmitida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin detalles: el código del catálogo lo dice todo. */
    public CodificacionNoAdmitida() {
        super("El CSV no está en UTF-8");
    }
}
