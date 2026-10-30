package ec.mdd.cargas.dominio;

import java.util.List;

/**
 * El estado de una carga (glosario «Estado de la carga», F01-H05). Se calcula con lo que hay en el
 * sistema; no se guarda.
 *
 * @param numero su orden en la ERS §8.3
 * @param codigo su código
 * @param estado si está pendiente, cargada o, la de proyectos, «sin proyectos»
 * @param faltan las cargas de las que depende que no están cargadas, en su orden; vacía si se puede
 *     subir
 * @param periodosSinProyectos solo en la carga de proyectos, los periodos confirmados «sin
 *     proyectos» que siguen valiendo, en orden
 */
public record EstadoDeCarga(
        int numero,
        String codigo,
        EstadoDeLaCarga estado,
        List<CargaQueFalta> faltan,
        List<String> periodosSinProyectos) {

    /** Copia las listas. */
    public EstadoDeCarga {
        faltan = List.copyOf(faltan);
        periodosSinProyectos = List.copyOf(periodosSinProyectos);
    }
}
