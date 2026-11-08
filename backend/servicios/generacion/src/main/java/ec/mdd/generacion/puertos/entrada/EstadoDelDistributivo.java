package ec.mdd.generacion.puertos.entrada;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * El distributivo de un periodo visto por Aprobación (F12-H02).
 *
 * @param periodo el código del periodo
 * @param version el número de la versión vigente
 * @param creadoEn cuándo se creó el distributivo
 * @param parciales el estado de cada parcial, por carrera
 */
public record EstadoDelDistributivo(
        String periodo, int version, Instant creadoEn, List<ParcialYSuEstado> parciales) {

    /** Exige los datos y copia la lista. */
    public EstadoDelDistributivo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(creadoEn, "creadoEn");
        parciales = List.copyOf(parciales);
    }

    /**
     * Un parcial y si está enviado. Expone un booleano y no el estado del dominio: la API publicada
     * no comparte clases internas (principio V).
     *
     * @param carrera la carrera
     * @param enviado si está enviado
     */
    public record ParcialYSuEstado(String carrera, boolean enviado) {}
}
