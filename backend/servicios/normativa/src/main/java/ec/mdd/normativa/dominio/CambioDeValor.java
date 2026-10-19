package ec.mdd.normativa.dominio;

/**
 * Un valor que cambió al editar una versión, para la bitácora (FR-009, principio VII).
 *
 * @param campo qué cambió: {@code codigo}, {@code origen}, {@code vigenteDesde}, {@code
 *     tablaCargaHoraria[<perfil>/<tipo>].<horas>} o {@code reglas[<RN-xx>].<parametro>}
 * @param anterior el valor anterior, en texto; {@code null} si no tenía
 * @param nuevo el valor nuevo, en texto; {@code null} si ya no tiene
 */
public record CambioDeValor(String campo, String anterior, String nuevo) {}
