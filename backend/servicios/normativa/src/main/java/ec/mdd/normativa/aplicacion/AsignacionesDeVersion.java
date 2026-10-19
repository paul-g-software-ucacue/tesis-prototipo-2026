package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import java.util.Optional;

/** La asignación de un periodo dentro de su versión, como la devuelven los casos de uso. */
final class AsignacionesDeVersion {

    static Optional<AsignacionDelPeriodo> del(VersionNormativa version, String periodo) {
        return version.asignaciones().stream()
                .filter(asignacion -> asignacion.periodo().equals(periodo))
                .findFirst()
                .map(
                        asignacion ->
                                new AsignacionDelPeriodo(
                                        periodo, version.numero(), asignacion.asociadaEn()));
    }

    private AsignacionesDeVersion() {}
}
