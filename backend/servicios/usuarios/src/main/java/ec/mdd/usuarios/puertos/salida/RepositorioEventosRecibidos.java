package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.puertos.entrada.EventoRecibido;

/** Los eventos de otros procesos que Usuarios ya registró ({@code usuarios.evento_recibido}). */
public interface RepositorioEventosRecibidos {

    /**
     * Guarda el evento si su identificador no estaba registrado. Es atómico: con dos entregas
     * simultáneas del mismo evento, solo una devuelve {@code true}.
     *
     * @param evento el evento recibido
     * @return {@code true} si se registró ahora; {@code false} si ya estaba
     */
    boolean registrarSiEsNuevo(EventoRecibido evento);
}
