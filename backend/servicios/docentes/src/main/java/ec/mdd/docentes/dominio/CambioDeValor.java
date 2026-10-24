package ec.mdd.docentes.dominio;

/**
 * Un valor que cambió al corregir un docente o su estado, para el registro (principio VII).
 *
 * @param campo qué cambió, por ejemplo {@code dedicacion} o {@code ficha.cursos}
 * @param anterior el valor anterior, en texto; una lista de la ficha va entera
 * @param nuevo el valor nuevo, en texto
 */
public record CambioDeValor(String campo, String anterior, String nuevo) {}
