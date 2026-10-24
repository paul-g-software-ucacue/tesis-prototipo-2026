package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.puertos.salida.Formato;

/**
 * Un archivo de carga que se puede leer (RF-06): de qué carga es, cómo se llama, en qué formato
 * vino y cuántos registros tiene.
 *
 * @param carga el código de la carga
 * @param archivo el nombre del archivo, sin ruta
 * @param formato CSV o XLSX
 * @param registros las filas de datos, sin el encabezado ni las filas vacías del final
 */
public record ArchivoValidado(String carga, String archivo, Formato formato, int registros) {}
