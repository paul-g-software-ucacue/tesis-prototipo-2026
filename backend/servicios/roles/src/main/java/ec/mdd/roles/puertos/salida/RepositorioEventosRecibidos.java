package ec.mdd.roles.puertos.salida;

import java.time.Instant;
import java.util.UUID;

/** Los eventos de otros procesos ya aplicados, para aplicar cada uno una sola vez (ADR-018). */
public interface RepositorioEventosRecibidos {

    /**
     * Anota el evento si es la primera vez que llega.
     *
     * @return {@code true} si es nuevo; {@code false} si ya estaba
     */
    boolean registrarSiEsNuevo(UUID idEvento, String tipo, String origen, Instant momento);
}
