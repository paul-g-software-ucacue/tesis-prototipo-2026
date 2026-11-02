package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.InformeSustentado;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Las consolidaciones y los informes de una versión. Solo se insertan (principio VII). */
public interface RepositorioConsolidacion {

    /** La última consolidación de la versión. */
    Optional<Consolidacion> ultimaConsolidacion(UUID version);

    /** Agrega una consolidación: la anterior se conserva. */
    void guardarConsolidacion(UUID version, Consolidacion consolidacion);

    /** El último informe de la versión. */
    Optional<InformeSustentado> ultimoInforme(UUID version);

    /** Agrega un informe: el anterior se conserva. */
    void guardarInforme(UUID version, InformeSustentado informe);

    /** Cuándo cambió por última vez una propuesta o una actividad del periodo. */
    Optional<Instant> ultimoCambioDeAsignaciones(String periodo);
}
