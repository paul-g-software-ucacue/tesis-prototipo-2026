package ec.mdd.cargas.dominio;

/**
 * Qué falta para crear el distributivo de un periodo (F02-H02, glosario «Tablero de preparación»).
 */
public enum TipoDeLoQueFalta {
    /** Una carga obligatoria que no está cargada. */
    CARGA,
    /** El periodo todavía no tiene su versión de normativa (F02-H01). */
    PERIODO_SIN_ABRIR,
    /** El periodo está cerrado: no admite un distributivo nuevo. */
    PERIODO_CERRADO,
    /** No hay ningún periodo en los ciclos académicos. */
    SIN_PERIODOS
}
