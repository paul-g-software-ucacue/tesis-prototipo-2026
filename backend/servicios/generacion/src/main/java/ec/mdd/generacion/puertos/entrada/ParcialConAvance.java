package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.AvanceDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import java.util.List;
import java.util.Objects;

/**
 * El parcial de una carrera, con cuánto le falta.
 *
 * @param parcial el parcial
 * @param avance las ofertadas y las que tienen propuesta
 * @param propuestas sus propuestas, también las de paralelos retirados
 */
public record ParcialConAvance(
        ParcialCarrera parcial, AvanceDelParcial avance, List<PropuestaVista> propuestas) {

    /** Exige los datos y copia la lista. */
    public ParcialConAvance {
        Objects.requireNonNull(parcial, "parcial");
        Objects.requireNonNull(avance, "avance");
        propuestas = List.copyOf(propuestas);
    }
}
