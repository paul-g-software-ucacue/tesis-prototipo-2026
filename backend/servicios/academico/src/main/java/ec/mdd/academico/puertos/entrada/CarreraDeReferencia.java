package ec.mdd.academico.puertos.entrada;

/**
 * Una carrera, con lo que Docentes necesita de ella (F03-H04).
 *
 * @param codigo el código SENESCYT
 * @param nombre el nombre, en mayúsculas
 * @param siglas las siglas
 */
public record CarreraDeReferencia(String codigo, String nombre, String siglas) {}
