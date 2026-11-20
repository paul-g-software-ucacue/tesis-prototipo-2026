package ec.mdd.validacionocs.dominio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * El estado de un periodo, según Académico.
 *
 * @param estado el estado, como lo nombra Académico
 * @param inicio el primer día del periodo, para la advertencia de RN-23 (F12-H03); vacío si el
 *     proceso {@code datos} es de una versión anterior que no lo responde (D-55)
 */
public record EstadoDelPeriodo(String estado, Optional<LocalDate> inicio) {

    /** Exige el inicio, presente o vacío. */
    public EstadoDelPeriodo {
        Objects.requireNonNull(inicio, "inicio");
    }

    /** Un estado sin su inicio, como antes de F12-H03. */
    public EstadoDelPeriodo(String estado) {
        this(estado, Optional.empty());
    }

    /** La configuración solo cambia en Planificación (RF-34). */
    public boolean enPlanificacion() {
        return "PLANIFICACION".equals(estado);
    }

    /** Un periodo cerrado: el distributivo ya no cambia de etapa (F12-H02). */
    public boolean cerrado() {
        return "CERRADO".equals(estado);
    }
}
