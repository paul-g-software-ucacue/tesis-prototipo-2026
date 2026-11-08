package ec.mdd.generacion.puertos.entrada;

import java.util.Objects;
import java.util.Optional;

/**
 * Lo que Aprobación necesita saber del consolidado para enviarlo a revisión (F10-H03, D-97),
 * calculado al pedirlo con el mismo panel y el mismo consolidado que ve la pantalla. Lleva tipos
 * simples: la API publicada no comparte clases del dominio de Generación (principio V).
 *
 * @param version el número de la versión vigente
 * @param consolidado si la versión vigente se consolidó alguna vez
 * @param desactualizado si una propuesta o una actividad cambió después de consolidar
 * @param informe la huella SHA-256 del informe sustentado vigente; vacía si no hay
 * @param duras las inconsistencias duras abiertas
 * @param blandas las inconsistencias blandas sin justificar (MD-07)
 */
public record CondicionesDeRevision(
        int version,
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
