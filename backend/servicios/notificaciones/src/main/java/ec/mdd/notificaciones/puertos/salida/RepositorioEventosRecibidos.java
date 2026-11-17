package ec.mdd.notificaciones.puertos.salida;

import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;

/** Los eventos de otros procesos ya registrados en Notificaciones (ADR-018 §4). */
@FunctionalInterface
public interface RepositorioEventosRecibidos {

    /**
     * Registra el evento si es la primera vez que llega.
     *
     * @param evento el evento
     * @return {@code true} si es nuevo; {@code false} si ya estaba
     */
    boolean registrarSiEsNuevo(EventoRecibido evento);
}
