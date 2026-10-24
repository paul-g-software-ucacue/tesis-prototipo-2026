package ec.mdd.cargas.puertos.salida;

/** El archivo no es CSV ni XLSX (RF-06). */
public class FormatoNoAdmitido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin detalles: el código del catálogo lo dice todo. */
    public FormatoNoAdmitido() {
        super("No es CSV ni XLSX");
    }
}
