package ec.mdd.academico.puertos.entrada;

/**
 * Una unidad de la estructura orgánica, como la ve Docentes para validar la unidad de un cargo
 * (F03-H05).
 *
 * @param codigo como en la carga 01
 * @param nombre su nombre
 * @param tipo el nombre de su tipo de unidad, como {@code GESTION} o {@code COMISION}
 * @param gestionAutorizada la gestión de POL 6 de una unidad de gestión, como {@code G07}; nulo en
 *     las demás
 * @param activa si no se inactivó
 */
public record UnidadDeReferencia(
        String codigo, String nombre, String tipo, String gestionAutorizada, boolean activa) {}
