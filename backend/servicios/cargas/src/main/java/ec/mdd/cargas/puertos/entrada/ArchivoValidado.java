package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.dominio.VistaPrevia;
import ec.mdd.cargas.puertos.salida.Formato;

/**
 * Un archivo de carga con una estructura válida, el informe de sus filas y su vista previa
 * (F01-H02, F01-H03, F01-H04).
 *
 * @param carga el código de la carga
 * @param archivo el nombre del archivo, sin ruta
 * @param formato el formato en que vino
 * @param informe los errores y las advertencias de sus filas
 * @param vistaPrevia lo que pasaría si se confirma
 */
public record ArchivoValidado(
        String carga,
        String archivo,
        Formato formato,
        InformeValidacionCarga informe,
        VistaPrevia vistaPrevia) {

    /** Cuántos registros tiene: las filas sin el encabezado ni las vacías. */
    public int registros() {
        return informe.registros();
    }
}
