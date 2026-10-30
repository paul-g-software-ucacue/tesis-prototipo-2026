package ec.mdd.cargas.puertos.salida;

/**
 * El puerto de fuente de datos de las cargas (RF-57, glosario «Fuente de datos»): entrega la tabla
 * de encabezados y filas de una carga, sin validar su contenido. Las reglas de las filas, la vista
 * previa y el orden de las cargas solo ven esa tabla, nunca el formato ni el origen. Su adaptador
 * inicial lee archivos CSV o XLSX ({@code LectorPorFormato}); Moodle, SIGA o Talento Humano serían
 * otros adaptadores que entreguen la misma tabla (F01-H05).
 */
public interface FuenteDeDatos {

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
