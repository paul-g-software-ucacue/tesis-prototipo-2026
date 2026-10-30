package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.FuenteDeDatos;
import java.util.Locale;

/**
 * El adaptador de archivos de la fuente de datos de las cargas (RF-57): la extensión, sin
 * distinguir mayúsculas, elige el lector de CSV o el de XLSX, y cada lector comprueba que el
 * contenido corresponde a su formato (research §3). Es el único que conoce los formatos.
 */
public final class LectorPorFormato implements FuenteDeDatos {

    private final LectorCsv csv = new LectorCsv();
    private final LectorXlsx xlsx = new LectorXlsx();

    @Override
    public ArchivoLeido leer(String nombre, byte[] contenido) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        if (minusculas.endsWith(".csv")) {
            return new ArchivoLeido(Formato.CSV, csv.leer(contenido));
        }
        if (minusculas.endsWith(".xlsx")) {
            return new ArchivoLeido(Formato.XLSX, xlsx.leer(contenido));
        }
        throw new FormatoNoAdmitido();
    }
}
