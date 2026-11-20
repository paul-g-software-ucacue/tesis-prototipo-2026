package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * El plazo de la etapa actual (`Plazo` del glosario, F12-H03, D-89, D-114): la fecha límite es la
 * fecha de la llegada en {@link ProcesoAprobacion#ZONA} más los días corridos, sin descontar fines
 * de semana ni feriados. La etapa vence al terminar ese día; se comparan días de Ecuador, no
 * instantes, para que una llegada de noche no corra la fecha por la diferencia con UTC.
 *
 * @param etapa la etapa actual; nunca PUBLICADO, que no tiene plazo
 * @param llegada cuándo llegó el distributivo a la etapa
 * @param dias los días corridos de la etapa, 1 o más
 * @param fechaLimite el último día del plazo
 */
public record Plazo(TipoDeEtapa etapa, Instant llegada, int dias, LocalDate fechaLimite) {

    /** Exige todo, un plazo de 1 día o más y una etapa con plazo. */
    public Plazo {
        Objects.requireNonNull(etapa, "etapa");
        Objects.requireNonNull(llegada, "llegada");
        Objects.requireNonNull(fechaLimite, "fechaLimite");
        if (dias < 1) {
            throw new IllegalArgumentException("Un plazo tiene 1 día o más");
        }
        if (etapa == TipoDeEtapa.PUBLICADO) {
            throw new IllegalArgumentException("PUBLICADO no tiene plazo");
        }
    }

    /**
     * El plazo de una llegada.
     *
     * @param etapa la etapa a la que llegó
     * @param llegada cuándo
     * @param dias los días de la etapa en la configuración del distributivo
     * @return el plazo, con su fecha límite
     */
    public static Plazo de(TipoDeEtapa etapa, Instant llegada, int dias) {
        Objects.requireNonNull(llegada, "llegada");
        return new Plazo(
                etapa,
                llegada,
                dias,
                LocalDate.ofInstant(llegada, ProcesoAprobacion.ZONA).plusDays(dias));
    }

    /**
     * Cómo va el plazo en un día.
     *
     * @param hoy el día, en Ecuador
     * @return el estado y los días que faltan hasta la fecha límite, negativos si venció
     */
    public Situacion en(LocalDate hoy) {
        int restantes = (int) ChronoUnit.DAYS.between(hoy, fechaLimite);
        EstadoDelPlazo estado =
                restantes > 0
                        ? EstadoDelPlazo.A_TIEMPO
                        : restantes == 0 ? EstadoDelPlazo.VENCE_HOY : EstadoDelPlazo.VENCIDO;
        return new Situacion(estado, restantes);
    }

    /**
     * @param hoy el día, en Ecuador
     * @return los días desde la fecha límite, o 0 si todavía no venció
     */
    public int diasDeAtraso(LocalDate hoy) {
        return Math.max(0, -en(hoy).diasRestantes());
    }

    /**
     * Cómo va un plazo en un día.
     *
     * @param estado a tiempo, vence hoy o vencido
     * @param diasRestantes la fecha límite menos el día; negativo si venció
     */
    public record Situacion(EstadoDelPlazo estado, int diasRestantes) {

        /** Exige el estado. */
        public Situacion {
            Objects.requireNonNull(estado, "estado");
        }
    }
}
