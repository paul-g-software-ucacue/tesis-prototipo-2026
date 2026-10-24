package ec.mdd.cargas.puertos.salida;

import ec.mdd.cargas.dominio.TablaDeDatos;

/**
 * Un archivo de carga que se pudo leer.
 *
 * @param formato el formato en que vino
 * @param tabla sus encabezados y sus filas, como textos
 */
public record ArchivoLeido(Formato formato, TablaDeDatos tabla) {}
