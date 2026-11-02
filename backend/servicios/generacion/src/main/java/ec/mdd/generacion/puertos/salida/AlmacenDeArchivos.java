package ec.mdd.generacion.puertos.salida;

/** Los archivos del negocio, separados de la base de datos (RNF-POR-01, D-88). */
public interface AlmacenDeArchivos {

    /**
     * Guarda un archivo completo.
     *
     * @param contenido sus bytes
     * @return la clave que le dio el almacén: nunca depende del nombre recibido
     */
    String guardar(byte[] contenido);

    /**
     * Lee un archivo.
     *
     * @param clave la que dio el almacén
     * @return sus bytes
     * @throws IllegalArgumentException si la clave no es una del almacén
     * @throws java.io.UncheckedIOException si no se puede leer
     */
    byte[] leer(String clave);
}
