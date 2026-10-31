package ec.mdd.cargas.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Algo que falta para crear el distributivo de un periodo (F02-H02, glosario «Tablero de
 * preparación»).
 *
 * @param tipo qué falta
 * @param carga solo en {@link TipoDeLoQueFalta#CARGA}, la carga que falta
 * @param estado solo en {@link TipoDeLoQueFalta#CARGA}, su estado en el periodo
 */
public record LoQueFalta(
        TipoDeLoQueFalta tipo, Optional<CargaQueFalta> carga, Optional<EstadoDeLaCarga> estado) {

    /** Valida que solo lo que falta de una carga tenga carga y estado. */
    public LoQueFalta {
        Objects.requireNonNull(tipo, "tipo");
        if ((tipo == TipoDeLoQueFalta.CARGA) != carga.isPresent()
                || carga.isPresent() != estado.isPresent()) {
            throw new IllegalArgumentException(
                    "Solo lo que falta de una carga lleva carga y estado");
        }
    }

    /** Una carga obligatoria que no está cargada. */
    static LoQueFalta laCarga(CargaDelPeriodo carga) {
        return new LoQueFalta(
                TipoDeLoQueFalta.CARGA,
                Optional.of(new CargaQueFalta(carga.numero(), carga.codigo(), carga.nombre())),
                Optional.of(carga.estado()));
    }

    /** Algo del periodo, sin carga. */
    static LoQueFalta del(TipoDeLoQueFalta tipo) {
        return new LoQueFalta(tipo, Optional.empty(), Optional.empty());
    }
}
