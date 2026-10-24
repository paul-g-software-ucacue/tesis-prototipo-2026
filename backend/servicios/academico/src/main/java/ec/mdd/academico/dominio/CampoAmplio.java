package ec.mdd.academico.dominio;

/**
 * Uno de los 11 campos amplios de la CINE, con su nombre (catálogo del diccionario de datos).
 *
 * @param codigo de {@code 00} a {@code 10}
 * @param nombre como «Tecnologías de la información y la comunicación»
 */
public record CampoAmplio(String codigo, String nombre) {}
