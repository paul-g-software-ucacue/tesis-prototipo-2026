package ec.mdd.generacion.dominio;

/**
 * El tipo de documento de un docente, como lo da Docentes. Se guarda al congelar para que el CSV
 * para SIGA salga del contenido congelado y no de los datos de hoy (F19-H01, D-119).
 */
public enum TipoDeIdentificacion {
    CEDULA,
    PASAPORTE
}
