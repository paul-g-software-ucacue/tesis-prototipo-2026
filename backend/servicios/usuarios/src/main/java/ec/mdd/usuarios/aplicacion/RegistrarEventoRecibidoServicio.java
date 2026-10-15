package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;

/**
 * El receptor de eventos de Usuarios (D-47, ADR-018).
 *
 * <p>Registra el evento con su identificador y, solo si es nuevo, aplica el manejador de su tipo,
 * los dos en la misma transacción: si el efecto falla, el registro se revierte y el emisor lo
 * vuelve a enviar. Un evento repetido no se aplica dos veces. En B00-H05 solo admite el evento de
 * prueba, sin efecto; F17-H01 agrega los tipos de la bitácora.
 *
 * <p>No es {@code final} porque la transacción la pone un proxy de Spring.
 */
public class RegistrarEventoRecibidoServicio implements RegistrarEventoRecibido {

    /** El tipo del evento de prueba de Generación (D-48). */
    public static final String EVENTO_DE_PRUEBA = "generacion.EventoDePrueba";

    private final RepositorioEventosRecibidos repositorio;
    private final Map<String, Consumer<EventoRecibido>> manejadores;

    /**
     * @param repositorio los eventos ya registrados
     * @param manejadores el efecto de cada tipo admitido
     */
    public RegistrarEventoRecibidoServicio(
            RepositorioEventosRecibidos repositorio,
            Map<String, Consumer<EventoRecibido>> manejadores) {
        this.repositorio = repositorio;
        this.manejadores = Map.copyOf(manejadores);
    }

    /** Los manejadores del receptor de prueba: solo el evento de prueba, sin efecto (D-47). */
    public static Map<String, Consumer<EventoRecibido>> manejadoresDePrueba() {
        return Map.of(EVENTO_DE_PRUEBA, evento -> {});
    }

    @Override
    @Transactional
    public void registrar(EventoRecibido evento) {
        Consumer<EventoRecibido> manejador = manejadores.get(evento.tipo());
        if (manejador == null) {
            throw new EventoNoReconocido(evento.tipo());
        }
        if (repositorio.registrarSiEsNuevo(evento)) {
            manejador.accept(evento);
        }
    }
}
