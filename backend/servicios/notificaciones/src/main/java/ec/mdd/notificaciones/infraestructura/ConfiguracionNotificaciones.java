package ec.mdd.notificaciones.infraestructura;

import ec.mdd.notificaciones.adaptadores.salida.DestinatariosDesdeIdentidad;
import ec.mdd.notificaciones.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.notificaciones.adaptadores.salida.RepositorioNotificacionesJdbc;
import ec.mdd.notificaciones.aplicacion.BandejaServicio;
import ec.mdd.notificaciones.aplicacion.RecibirNotificacionesServicio;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import ec.mdd.notificaciones.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import ec.mdd.roles.puertos.entrada.ConsultarDestinatarios;
import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los beans de Notificaciones (F16-H01): la bandeja en su esquema, los destinatarios desde Roles y
 * Usuarios en el mismo proceso, y el receptor de los eventos.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionNotificaciones {

    /** Los eventos recibidos, después de migrar el esquema. */
    @Bean
    @DependsOn("flywayNotificaciones")
    public RepositorioEventosRecibidos repositorioEventosRecibidosNotificaciones(JdbcClient jdbc) {
        return new RepositorioEventosRecibidosJdbc(jdbc);
    }

    /** Las bandejas, después de migrar el esquema. */
    @Bean
    @DependsOn("flywayNotificaciones")
    public RepositorioNotificaciones repositorioNotificaciones(JdbcClient jdbc) {
        return new RepositorioNotificacionesJdbc(jdbc);
    }

    /** Quién recibe cada mensaje, por las interfaces publicadas de Roles y Usuarios. */
    @Bean
    public Destinatarios destinatariosDeNotificaciones(
            ConsultarDestinatarios consultarDestinatarios, ListarUsuarios listarUsuarios) {
        return new DestinatariosDesdeIdentidad(consultarDestinatarios, listarUsuarios);
    }

    /** La bandeja del usuario de la sesión y marcar como leída (US2). */
    @Bean
    public BandejaServicio bandejaDeNotificaciones(
            RepositorioNotificaciones repositorioNotificaciones) {
        return new BandejaServicio(repositorioNotificaciones, Clock.systemUTC());
    }

    /** El receptor de las notificaciones que piden otros procesos (ADR-018). */
    @Bean
    public RecibirNotificaciones recibirNotificaciones(
            RepositorioEventosRecibidos repositorioEventosRecibidosNotificaciones,
            RepositorioNotificaciones repositorioNotificaciones,
            Destinatarios destinatariosDeNotificaciones) {
        return new RecibirNotificacionesServicio(
                repositorioEventosRecibidosNotificaciones,
                repositorioNotificaciones,
                destinatariosDeNotificaciones,
                Clock.systemUTC());
    }
}
