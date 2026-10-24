package ec.mdd.cargas.puertos.entrada;

/** El archivo de una carga pasa de 5 MB (F01-H02). */
public class ArchivoDemasiadoGrande extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param bytes el tamaño del archivo
     */
    public ArchivoDemasiadoGrande(long bytes) {
        super("El archivo tiene " + bytes + " bytes");
    }
}
