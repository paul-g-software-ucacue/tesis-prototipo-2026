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

    /**
     * Lee un archivo guardado (F14-H01).
     *
     * @param nombre el que devolvió {@link #guardar}: {@code documentos/<sha256>.pdf}
     * @return su contenido
     * @throws IllegalArgumentException si el nombre no es de un documento
     * @throws java.io.UncheckedIOException si no se puede leer
     */
    byte[] leer(String nombre);
}
