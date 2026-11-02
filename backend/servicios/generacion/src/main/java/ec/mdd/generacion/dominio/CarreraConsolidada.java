package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Una carrera en el consolidado.
 *
 * @param carrera su código
 * @param estado el estado de su parcial
 * @param avance cuántos paralelos tienen propuesta
 * @param horas sus horas por componente
 */
public record CarreraConsolidada(
        String carrera, EstadoDelParcial estado, AvanceDelParcial avance, CargaHoraria horas) {

    /** Exige los datos. */
    public CarreraConsolidada {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(avance, "avance");
        Objects.requireNonNull(horas, "horas");
    }
}
