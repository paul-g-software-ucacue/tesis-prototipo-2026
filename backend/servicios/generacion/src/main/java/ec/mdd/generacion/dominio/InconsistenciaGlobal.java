package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Una violación que mira la versión completa, no una asignación (RF-29). La severidad sale de la
 * versión de normativa.
 *
 * @param tipo qué se comprobó
 * @param severidad dura o blanda
 * @param objeto lo que la incumple
 * @param datos sus cifras
 */
public record InconsistenciaGlobal(
        TipoDeInconsistencia tipo,
        Severidad severidad,
        ObjetoAfectado objeto,
        DatosDeLaInconsistencia datos) {

    /** Exige los datos. */
    public InconsistenciaGlobal {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(severidad, "severidad");
        Objects.requireNonNull(objeto, "objeto");
        Objects.requireNonNull(datos, "datos");
    }
}
