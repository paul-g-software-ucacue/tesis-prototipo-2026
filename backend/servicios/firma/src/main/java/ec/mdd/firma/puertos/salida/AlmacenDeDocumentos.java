package ec.mdd.firma.puertos.salida;

/**
 * Guarda los PDF del documento firmable fuera de la base (RNF-POR-01, D-101): nombrados por su
 * SHA-256 y nunca sobrescritos.
 */
public interface AlmacenDeDocumentos {

    /**
     * Guarda el PDF con el nombre de su hash. Si ya existe un archivo con ese nombre, su contenido
     * es el mismo por construcción: no se escribe otra vez.
     *
     * @param pdf los bytes
     * @param sha256 su SHA-256 en hexadecimal
     * @return dónde quedó, relativo al almacén: {@code documentos/<sha256>.pdf}
     * @throws java.io.UncheckedIOException si no se puede escribir
     */
    String guardar(byte[] pdf, String sha256);
}
