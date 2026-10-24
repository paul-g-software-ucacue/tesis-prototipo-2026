package ec.mdd.cargas.dominio;

/** Por qué un encabezado del archivo no coincide con la plantilla de su carga (RF-07). */
public enum MotivoDeDiferencia {
    /** La columna de la plantilla no está en el archivo. */
    FALTA,
    /** La columna del archivo no está en la plantilla. */
    SOBRA,
    /** La columna aparece más de una vez en el archivo. */
    REPETIDA,
    /** Están todas las columnas, pero esta no va en su lugar. */
    FUERA_DE_LUGAR
}
