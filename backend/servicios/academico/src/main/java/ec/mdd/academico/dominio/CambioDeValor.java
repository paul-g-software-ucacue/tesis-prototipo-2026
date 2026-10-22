package ec.mdd.academico.dominio;

/**
 * Un valor que cambió al corregir un periodo o una unidad, para el registro (principio VII).
 *
 * @param campo qué cambió, por ejemplo {@code nombre}, {@code inicio} o {@code padre}
 * @param anterior el valor anterior, en texto
 * @param nuevo el valor nuevo, en texto
 */
public record CambioDeValor(String campo, String anterior, String nuevo) {}
