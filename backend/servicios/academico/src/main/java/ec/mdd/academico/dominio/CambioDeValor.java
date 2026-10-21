package ec.mdd.academico.dominio;

/**
 * Un valor que cambió al corregir un periodo, para el registro (FR-008, principio VII).
 *
 * @param campo qué cambió: {@code nombre}, {@code inicio} o {@code fin}
 * @param anterior el valor anterior, en texto
 * @param nuevo el valor nuevo, en texto
 */
public record CambioDeValor(String campo, String anterior, String nuevo) {}
