package ec.mdd.academico.puertos.entrada;

/**
 * Uno de los 11 campos amplios de la CINE (F03-H04).
 *
 * @param codigo de {@code 00} a {@code 10}
 * @param nombre como «Tecnologías de la información y la comunicación»
 */
public record CampoAmplioDeReferencia(String codigo, String nombre) {}
