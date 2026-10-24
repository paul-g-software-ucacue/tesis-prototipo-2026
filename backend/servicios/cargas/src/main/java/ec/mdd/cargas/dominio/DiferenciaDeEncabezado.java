package ec.mdd.cargas.dominio;

/**
 * Un encabezado del archivo que no coincide con la plantilla de su carga (RF-07).
 *
 * @param columna el nombre de la columna, sin espacios en los extremos
 * @param motivo por qué no coincide
 * @param posicion dónde va en la plantilla, desde 1, si está fuera de lugar; si no, {@code null}
 */
public record DiferenciaDeEncabezado(String columna, MotivoDeDiferencia motivo, Integer posicion) {}
