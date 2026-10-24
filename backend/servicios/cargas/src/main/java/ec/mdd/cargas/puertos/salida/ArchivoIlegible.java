package ec.mdd.cargas.puertos.salida;

/**
 * El archivo no se puede leer: comillas sin cerrar, una fila con otro número de valores o un XLSX
 * dañado o cifrado (RF-07). El motivo dice la fila, pero nunca un valor del archivo (principio IX).
 */
public class ArchivoIlegible extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param motivo en español, para el usuario, sin valores del archivo
     */
    public ArchivoIlegible(String motivo) {
        super(motivo);
    }

    /** El motivo, para el detalle de la respuesta. */
    public String motivo() {
        return getMessage();
    }
}
