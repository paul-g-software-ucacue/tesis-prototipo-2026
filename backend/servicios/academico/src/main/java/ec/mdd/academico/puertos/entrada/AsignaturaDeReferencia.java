package ec.mdd.academico.puertos.entrada;

/**
 * Una asignatura, con lo que Docentes necesita de ella (F03-H04).
 *
 * @param codigo el código de la asignatura
 * @param nombre su nombre
 * @param carrera el código de su carrera
 */
public record AsignaturaDeReferencia(String codigo, String nombre, String carrera) {}
