package ec.mdd.validacionocs.dominio;

/**
 * El código estable de una advertencia de la carga de la configuración (F12-H03, D-114). No es un
 * código del catálogo de errores: una advertencia no rechaza nada.
 */
public enum CodigoDeAdvertencia {
    /** Con estos plazos, el distributivo se aprobaría después de la fecha que pide RN-23. */
    RN23_MARGEN_INSUFICIENTE,
    /** El periodo no tiene versión de normativa, así que RN-23 no se pudo comprobar. */
    RN23_SIN_NORMATIVA
}
