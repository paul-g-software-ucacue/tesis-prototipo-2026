package ec.mdd.cargas.dominio;

/**
 * Una carga anterior que todavía no tiene datos y de la que depende otra (F01-H05).
 *
 * @param numero su orden en la ERS §8.3
 * @param codigo su código, como {@code carreras}
 * @param nombre su nombre, como «Carreras»
 */
public record CargaQueFalta(int numero, String codigo, String nombre) {

    /** La que falta, a partir de la carga del diccionario. */
    static CargaQueFalta de(Carga carga) {
        return new CargaQueFalta(carga.numero(), carga.codigo(), carga.nombre());
    }
}
