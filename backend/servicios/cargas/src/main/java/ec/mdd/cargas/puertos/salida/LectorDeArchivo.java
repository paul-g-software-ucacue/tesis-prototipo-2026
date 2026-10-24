package ec.mdd.cargas.puertos.salida;

/**
 * La fuente de datos de una carga cuando llega como archivo (RF-57): entrega la tabla de
 * encabezados y filas, sin validar su contenido. Otra fuente, como Moodle o SIGA, sería otro
 * adaptador que entregue la misma tabla.
 */
public interface LectorDeArchivo {

    /**
     * Lee el archivo.
     *
     * @param nombre el nombre del archivo, sin ruta; su extensión decide el formato
     * @param contenido los bytes del archivo
     * @return el formato y la tabla
     * @throws FormatoNoAdmitido si no es CSV ni XLSX
     * @throws CodificacionNoAdmitida si es un CSV que no está en UTF-8
     * @throws VariasHojas si es un XLSX con más de una hoja
     * @throws ArchivoIlegible si no se puede leer, con el motivo
     */
    ArchivoLeido leer(String nombre, byte[] contenido);
}
