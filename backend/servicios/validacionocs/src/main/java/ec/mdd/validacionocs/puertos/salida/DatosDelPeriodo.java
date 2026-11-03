package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import java.util.Optional;

/** Lo que otros procesos saben de un periodo (principio V, ADR-023). */
public interface DatosDelPeriodo {

    /**
     * @param periodo el código del periodo
     * @return su estado, según Académico; vacío si no existe
     */
    Optional<EstadoDelPeriodo> estado(String periodo);

    /**
     * @param periodo el código del periodo
     * @return quién puede firmar, según Roles
     */
    Firmantes firmantes(String periodo);
}
