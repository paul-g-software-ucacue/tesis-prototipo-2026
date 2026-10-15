package ec.mdd.generacion.puertos.salida;

import java.time.Instant;
import java.util.UUID;

/** Los eventos de prueba publicados ({@code generacion.evento_prueba}, D-48). */
public interface RepositorioEventosDePrueba {

    /** Guarda el evento de prueba en la transacción en curso. */
    void guardar(UUID idEvento, Instant momento);
}
