package ec.mdd.cargas.dominio;

/** El archivo no tiene registros debajo de los encabezados (RF-06). */
public class ArchivoSinRegistros extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** El archivo está vacío o solo tiene los encabezados. */
    public ArchivoSinRegistros() {
        super("El archivo no tiene registros");
    }
}
