package ec.mdd.notificaciones.aplicacion;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.MensajeSolicitado;
import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.dominio.SolicitudDeNotificaciones;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import ec.mdd.notificaciones.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * El receptor de Notificaciones (F16-H01, D-108, ADR-018).
 *
 * <p>Admite los eventos {@code <servicio>.NotificacionesSolicitadas} de los servicios de {@link
 * #ORIGENES}, con el prefijo del tipo igual a su {@code origen}. Primero valida la solicitud;
 * después, en una sola transacción, registra el {@code idEvento} y, solo si es nuevo, resuelve cada
 * destino a sus usuarios activos y guarda una notificación por cada uno, sin el autor: nadie recibe
 * su propia notificación (FR-002). Si Roles o Usuarios fallan, nada queda y el emisor lo vuelve a
 * enviar (FR-011).
 *
 * <p>El destino es un rol o una persona (D-110). Una persona sin cuenta activa no recibe nada, sin
 * respaldo por rol: el firmante esperado es esa persona, no cualquiera con su rol. No es un error,
 * así que el evento queda recibido y no se reenvía.
 *
 * <p>Los destinatarios se fijan al recibir: quien recibe el rol después no ve las anteriores, y
 * quien lo pierde conserva las que ya tenía.
 *
 * <p>No es {@code final} porque la transacción la pone un proxy de Spring.
 */
public class RecibirNotificacionesServicio implements RecibirNotificaciones {

    /** El nombre del evento, después del prefijo del servicio. */
    public static final String NOTIFICACIONES_SOLICITADAS = "NotificacionesSolicitadas";

    /**
     * Los servicios que piden notificaciones: Aprobación por cada transición (D-108) y Firma por
     * cada firma aceptada que tiene un paso siguiente (F16-H02, D-110).
     */
    public static final List<String> ORIGENES = List.of("validacionocs", "firma");

    /** El autor de lo que no pide una persona, como la cadena de firmas completa. */
    public static final String SISTEMA = "sistema";

    /** Lo que se muestra como autor cuando lo hizo el sistema. */
    public static final String NOMBRE_DEL_SISTEMA = "el sistema";

    private final RepositorioEventosRecibidos eventos;
    private final RepositorioNotificaciones notificaciones;
    private final Destinatarios destinatarios;
    private final Clock reloj;

    /**
     * @param eventos los eventos ya registrados
     * @param notificaciones las bandejas
     * @param destinatarios los usuarios de cada destino, de Roles y Usuarios
     * @param reloj el momento de la recepción
     */
    public RecibirNotificacionesServicio(
            RepositorioEventosRecibidos eventos,
            RepositorioNotificaciones notificaciones,
            Destinatarios destinatarios,
            Clock reloj) {
        this.eventos = eventos;
        this.notificaciones = notificaciones;
        this.destinatarios = destinatarios;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void recibir(EventoRecibido evento) {
        exigirTipo(evento);
        SolicitudDeNotificaciones solicitud = SolicitudDeNotificaciones.de(contenido(evento));
        if (!eventos.registrarSiEsNuevo(evento)) {
            return;
        }
        String autorNombre =
                SISTEMA.equals(solicitud.autor())
                        ? NOMBRE_DEL_SISTEMA
                        : destinatarios.nombre(solicitud.autor()).orElse(solicitud.autor());
        Instant ahora = reloj.instant();
        List<Notificacion> nuevas = new ArrayList<>();
        for (MensajeSolicitado mensaje : solicitud.mensajes()) {
            for (Destinatario destinatario :
                    destinatarios.de(mensaje.destino(), solicitud.periodo())) {
                if (destinatario.usuario().equalsIgnoreCase(solicitud.autor())) {
                    continue;
                }
                nuevas.add(
                        new Notificacion(
                                UUID.randomUUID(),
                                evento.idEvento(),
                                destinatario.usuario(),
                                destinatario.carrera().or(() -> mensaje.destino().carrera()),
                                solicitud.periodo(),
                                mensaje.titulo(),
                                mensaje.detalle(),
                                mensaje.enlace(),
                                solicitud.autor(),
                                autorNombre,
                                solicitud.momento(),
                                ahora,
                                Optional.empty()));
            }
        }
        if (!nuevas.isEmpty()) {
            notificaciones.guardar(nuevas);
        }
    }

    private static void exigirTipo(EventoRecibido evento) {
        String tipo = evento.tipo();
        int punto = tipo == null ? -1 : tipo.indexOf('.');
        if (punto < 0) {
            throw new EventoNoReconocido(String.valueOf(tipo));
        }
        String servicio = tipo.substring(0, punto);
        if (!ORIGENES.contains(servicio)
                || !NOTIFICACIONES_SOLICITADAS.equals(tipo.substring(punto + 1))
                || !servicio.equals(evento.origen())) {
            throw new EventoNoReconocido(tipo);
        }
    }

    private static Map<String, Object> contenido(EventoRecibido evento) {
        return evento.contenido();
    }
}
