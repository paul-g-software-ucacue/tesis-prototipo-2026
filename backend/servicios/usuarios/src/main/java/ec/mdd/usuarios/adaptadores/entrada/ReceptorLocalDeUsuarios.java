package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.plataforma.eventos.ReceptorLocal;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;

/**
 * El receptor de Usuarios para los servicios del mismo proceso, como Roles (ADR-024). Hace lo mismo
 * que {@link EventosControlador}, pero sin HTTP: traduce el sobre al caso de uso y deja salir sus
 * errores, para que la publicación quede pendiente y se reenvíe.
 */
public final class ReceptorLocalDeUsuarios implements ReceptorLocal {

    private final RegistrarEventoRecibido registrar;

    /** Receptor sobre el caso de uso que registra los eventos recibidos. */
    public ReceptorLocalDeUsuarios(RegistrarEventoRecibido registrar) {
        this.registrar = registrar;
    }

    @Override
    public String destino() {
        return "usuarios";
    }

    @Override
    public void recibir(EventoEntreProcesos evento) {
        registrar.registrar(
                new EventoRecibido(
                        evento.idEvento(),
                        evento.tipo(),
                        evento.origen(),
                        evento.momento(),
                        evento.contenido()));
    }
}
