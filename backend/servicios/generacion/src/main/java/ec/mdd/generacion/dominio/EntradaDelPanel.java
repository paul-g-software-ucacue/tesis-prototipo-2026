package ec.mdd.generacion.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Una inconsistencia en el panel, con su justificación si la tiene.
 *
 * @param inconsistencia lo que se incumple
 * @param justificacion por qué se acepta; vacía si no se justificó
 */
public record EntradaDelPanel(
        InconsistenciaGlobal inconsistencia,
        Optional<JustificacionDeInconsistencia> justificacion) {

    /** Exige los datos. */
    public EntradaDelPanel {
        Objects.requireNonNull(inconsistencia, "inconsistencia");
        Objects.requireNonNull(justificacion, "justificacion");
    }

    /** Si sigue abierta: una dura nunca se cierra con una justificación (MD-07). */
    public boolean abierta() {
        return inconsistencia.severidad() == Severidad.DURA || justificacion.isEmpty();
    }
}
