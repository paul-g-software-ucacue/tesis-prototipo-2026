package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import java.util.Locale;

/**
 * La fuente de datos de las cargas cuando llegan como archivo (RF-57): la extensión, sin distinguir
 * mayúsculas, elige el lector de CSV o el de XLSX, y cada lector comprueba que el contenido
 * corresponde a su formato (research §3).
 */
public final class LectorPorFormato implements LectorDeArchivo {

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
