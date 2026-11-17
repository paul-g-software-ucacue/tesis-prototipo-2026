package ec.mdd.notificaciones.aplicacion;

import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.dominio.NotificacionNoEncontrada;
import ec.mdd.notificaciones.puertos.entrada.BandejaDeUsuario;
import ec.mdd.notificaciones.puertos.entrada.ConsultarBandeja;
import ec.mdd.notificaciones.puertos.entrada.MarcarLeida;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * La bandeja del usuario de la sesión (F16-H01, US2). No pide un permiso de la matriz D-16: el
 * límite es el destinatario, que es el usuario del token; la notificación de otro usuario se
 * responde igual que una que no existe (D-108, principio IX). Marcar como leída no deja entrada en
 * la bitácora: no es un cambio del distributivo ni de los datos maestros.
 *
 * <p>No es {@code final} porque la transacción la pone un proxy de Spring.
 */
public class BandejaServicio implements ConsultarBandeja, MarcarLeida {

    private final RepositorioNotificaciones notificaciones;
    private final Clock reloj;

    /**
     * @param notificaciones las bandejas
     * @param reloj el momento de la lectura
     */
    public BandejaServicio(RepositorioNotificaciones notificaciones, Clock reloj) {
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public BandejaDeUsuario bandeja(String usuario, int limite) {
        return new BandejaDeUsuario(
                notificaciones.bandeja(usuario, limite), notificaciones.sinLeer(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public int sinLeer(String usuario) {
        return notificaciones.sinLeer(usuario);
    }

    @Override
    @Transactional
    public Notificacion marcar(UUID id, String usuario) {
        Notificacion notificacion =
                notificaciones.de(id, usuario).orElseThrow(NotificacionNoEncontrada::new);
        if (notificacion.leida()) {
            return notificacion;
        }
        // En microsegundos, como la guarda PostgreSQL: marcar otra vez devuelve la misma lectura.
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        notificaciones.marcarLeida(id, ahora);
        return notificacion.marcarLeida(ahora);
    }
}
