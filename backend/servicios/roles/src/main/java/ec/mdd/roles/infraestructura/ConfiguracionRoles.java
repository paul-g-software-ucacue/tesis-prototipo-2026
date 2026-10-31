package ec.mdd.roles.infraestructura;

import ec.mdd.roles.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioAsignacionesDeRolJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioCarrerasDeCoordinadoresJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioDesignacionesJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioPeriodosConocidosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioRolesJdbc;
import ec.mdd.roles.adaptadores.salida.TransaccionSpring;
import ec.mdd.roles.aplicacion.AsignarCarreraServicio;
import ec.mdd.roles.aplicacion.AsignarRolesInicialesServicio;
import ec.mdd.roles.aplicacion.AsignarRolesServicio;
import ec.mdd.roles.aplicacion.ConsultarAutorizacionServicio;
import ec.mdd.roles.aplicacion.DesignarResponsableServicio;
import ec.mdd.roles.aplicacion.GestionarRolesServicio;
import ec.mdd.roles.aplicacion.RecibirEventosServicio;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.AsignarRoles;
import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import ec.mdd.roles.puertos.entrada.RecibirEventos;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import ec.mdd.roles.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Roles: los repositorios del esquema {@code roles} y los casos de uso.
 *
 * <p>Los repositorios esperan a que Flyway migre el esquema, porque Usuarios pide los roles
 * iniciales de la demo al arrancar.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionRoles {

    @Bean
    @DependsOn("flywayRoles")
    public RepositorioRoles repositorioRoles(
            JdbcClient jdbc, TransactionOperations transactionTemplate) {
        return new RepositorioRolesJdbc(jdbc, transactionTemplate);
    }

    @Bean
    @DependsOn("flywayRoles")
    public RepositorioAsignacionesDeRol repositorioAsignacionesDeRol(
            JdbcClient jdbc, TransactionOperations transactionTemplate) {
        return new RepositorioAsignacionesDeRolJdbc(jdbc, transactionTemplate);
    }

    @Bean
    public RegistroDeCambios registroDeCambios(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    @Bean
    @DependsOn("flywayRoles")
    public RepositorioPeriodosConocidos repositorioPeriodosConocidos(JdbcClient jdbc) {
        return new RepositorioPeriodosConocidosJdbc(jdbc);
    }

    @Bean
    @DependsOn("flywayRoles")
    public RepositorioDesignaciones repositorioDesignaciones(JdbcClient jdbc) {
        return new RepositorioDesignacionesJdbc(jdbc);
    }

    @Bean
    @DependsOn("flywayRoles")
    public RepositorioEventosRecibidos repositorioEventosRecibidosRoles(JdbcClient jdbc) {
        return new RepositorioEventosRecibidosJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionRoles(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Designar al Responsable de consolidación y consultarlos (F05-H03, D-13, ADR-003). */
    @Bean
    public DesignarResponsableServicio designarResponsable(
            RepositorioPeriodosConocidos periodos,
            RepositorioDesignaciones designaciones,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Transaccion transaccionRoles) {
        return new DesignarResponsableServicio(
                periodos,
                designaciones,
                asignaciones,
                registro,
                transaccionRoles,
                Clock.systemUTC());
    }

    /** Los avisos de Académico con cada periodo y su estado (F05-H03, ADR-018). */
    @Bean
    public RecibirEventos recibirEventosRoles(
            RepositorioEventosRecibidos eventos,
            RepositorioPeriodosConocidos periodos,
            Transaccion transaccionRoles) {
        return new RecibirEventosServicio(eventos, periodos, transaccionRoles, Clock.systemUTC());
    }

    /** Roles y permisos efectivos, para el token de sesión (ADR-021). */
    @Bean
    public ConsultarAutorizacion consultarAutorizacion(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RepositorioDesignaciones designaciones,
            RepositorioCarrerasDeCoordinadores carreras) {
        return new ConsultarAutorizacionServicio(roles, asignaciones, designaciones, carreras);
    }

    /** La administración de roles (RF-02, FR-004). */
    @Bean
    public GestionarRoles gestionarRoles(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro) {
        return new GestionarRolesServicio(roles, asignaciones, registro, Clock.systemUTC());
    }

    /** Las asignaciones de rol a usuarios (RF-03). */
    @Bean
    public AsignarRoles asignarRoles(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            RepositorioCarrerasDeCoordinadores carreras,
            Transaccion transaccion) {
        return new AsignarRolesServicio(
                roles, asignaciones, registro, carreras, transaccion, Clock.systemUTC());
    }

    /** La carrera de cada coordinador (F06-H05). */
    @Bean
    public RepositorioCarrerasDeCoordinadores repositorioCarrerasDeCoordinadores(JdbcClient jdbc) {
        return new RepositorioCarrerasDeCoordinadoresJdbc(jdbc);
    }

    /** Indicar y quitar la carrera de un coordinador (F06-H05, D-64). */
    @Bean
    public AsignarCarrera asignarCarrera(
            RepositorioCarrerasDeCoordinadores carreras,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro,
            Transaccion transaccion) {
        return new AsignarCarreraServicio(
                carreras, asignaciones, registro, transaccion, Clock.systemUTC());
    }

    /** Roles iniciales de la demo, una sola vez por usuario (research §8). */
    @Bean
    public AsignarRolesIniciales asignarRolesIniciales(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RegistroDeCambios registro) {
        return new AsignarRolesInicialesServicio(roles, asignaciones, registro, Clock.systemUTC());
    }
}
