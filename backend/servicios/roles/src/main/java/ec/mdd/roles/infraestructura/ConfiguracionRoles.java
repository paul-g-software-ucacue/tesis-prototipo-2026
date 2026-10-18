package ec.mdd.roles.infraestructura;

import ec.mdd.roles.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioAsignacionesDeRolJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioRolesJdbc;
import ec.mdd.roles.aplicacion.AsignarRolesInicialesServicio;
import ec.mdd.roles.aplicacion.AsignarRolesServicio;
import ec.mdd.roles.aplicacion.ConsultarAutorizacionServicio;
import ec.mdd.roles.aplicacion.GestionarRolesServicio;
import ec.mdd.roles.puertos.entrada.AsignarRoles;
import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
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

    /** Roles y permisos efectivos, para el token de sesión (ADR-021). */
    @Bean
    public ConsultarAutorizacion consultarAutorizacion(
            RepositorioRoles roles, RepositorioAsignacionesDeRol asignaciones) {
        return new ConsultarAutorizacionServicio(roles, asignaciones);
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
            RegistroDeCambios registro) {
        return new AsignarRolesServicio(roles, asignaciones, registro, Clock.systemUTC());
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
