package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.puertos.salida.Formato;

/**
 * Un archivo de carga con una estructura válida y el informe de sus filas (F01-H02, F01-H03).
 *
 * @param carga el código de la carga
 * @param archivo el nombre del archivo, sin ruta
 * @param formato el formato en que vino
 * @param informe los errores y las advertencias de sus filas
 */
public record ArchivoValidado(
        String carga, String archivo, Formato formato, InformeValidacionCarga informe) {

    /** Cuántos registros tiene: las filas sin el encabezado ni las vacías. */
    public int registros() {
        return informe.registros();
    }
}
