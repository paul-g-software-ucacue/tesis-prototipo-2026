package ec.mdd.validacionocs.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Lo que Generación dice del consolidado al enviarlo a revisión (F10-H03, D-97), calculado en ese
 * momento (FR-008). Es un valor: no cambia.
 *
 * @param consolidado si la versión vigente se consolidó alguna vez
 * @param desactualizado si una propuesta o una actividad cambió después de consolidar
 * @param informe la huella del informe sustentado vigente; vacía si no hay
 * @param duras las inconsistencias duras abiertas
 * @param blandas las blandas sin justificar (MD-07)
 */
public record CondicionesDeRevision(
        boolean consolidado,
        boolean desactualizado,
        Optional<String> informe,
        long duras,
        long blandas) {

    /** Exige el informe opcional y cuentas no negativas. */
    public CondicionesDeRevision {
        Objects.requireNonNull(informe, "informe");
        if (duras < 0 || blandas < 0) {
            throw new IllegalArgumentException("Las abiertas no son negativas");
        }
    }
}
