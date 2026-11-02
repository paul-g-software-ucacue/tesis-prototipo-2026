package ec.mdd.normativa.infraestructura;

import ec.mdd.normativa.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioActividadesJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.adaptadores.salida.TransaccionSpring;
import ec.mdd.normativa.aplicacion.AsociarVersionAPeriodoServicio;
import ec.mdd.normativa.aplicacion.ConsultarActividadesDelPeriodoServicio;
import ec.mdd.normativa.aplicacion.ConsultarNormativaServicio;
import ec.mdd.normativa.aplicacion.ConsultarPesosDelPeriodoServicio;
import ec.mdd.normativa.aplicacion.ConsultarProporcionDelPeriodoServicio;
import ec.mdd.normativa.aplicacion.ConsultarReglasDeAsignacionServicio;
import ec.mdd.normativa.aplicacion.CrearVersionServicio;
import ec.mdd.normativa.aplicacion.EditarVersionServicio;
import ec.mdd.normativa.aplicacion.cargas.CargasDeNormativa;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarActividadesDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarPesosDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarProporcionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarReglasDeAsignacion;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.normativa.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Normativa: el repositorio del esquema {@code normativa}, su registro de cambios, la
 * transacción y los casos de uso. El repositorio espera a que Flyway migre el esquema, que trae la
 * versión 1 (F04-H01).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionNormativa {

    @Bean
    @DependsOn("flywayNormativa")
    public RepositorioVersiones repositorioVersiones(JdbcClient jdbc) {
        return new RepositorioVersionesJdbc(jdbc);
    }

    @Bean
    public RegistroDeCambios registroDeCambiosNormativa(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionNormativa(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** La consulta de las versiones de normativa (RF-19). */
    @Bean
    public ConsultarNormativa consultarNormativa(RepositorioVersiones repositorio) {
        return new ConsultarNormativaServicio(repositorio);
    }

    /** Las cifras de las reglas duras de un periodo (RN-02, RN-13). */
    @Bean
    public ConsultarReglasDeAsignacion consultarReglasDeAsignacion(
            RepositorioVersiones repositorio) {
        return new ConsultarReglasDeAsignacionServicio(repositorio);
    }

    /** La meta de la proporción de un periodo (RN-05). */
    @Bean
    public ConsultarProporcionDelPeriodo consultarProporcionDelPeriodo(
            RepositorioVersiones repositorio) {
        return new ConsultarProporcionDelPeriodoServicio(repositorio);
    }

    /** Los pesos de afinidad de un periodo, para otros procesos (F06-H01, ADR-023). */
    @Bean
    public ConsultarPesosDelPeriodo consultarPesosDelPeriodo(RepositorioVersiones repositorio) {
        return new ConsultarPesosDelPeriodoServicio(repositorio);
    }

    /** El catálogo de actividades de cada versión (D-79). */
    @Bean
    public RepositorioActividades repositorioActividades(JdbcClient jdbc) {
        return new RepositorioActividadesJdbc(jdbc);
    }

    /** El catálogo de un periodo, para otros procesos (F06-H04, ADR-023). */
    @Bean
    public ConsultarActividadesDelPeriodo consultarActividadesDelPeriodo(
            RepositorioVersiones versiones, RepositorioActividades actividades) {
        return new ConsultarActividadesDelPeriodoServicio(versiones, actividades);
    }

    /** Crear una versión nueva copiando otra (F04-H02). */
    @Bean
    public CrearVersion crearVersion(
            RepositorioVersiones repositorio, RegistroDeCambios registro, Transaccion transaccion) {
        return new CrearVersionServicio(repositorio, registro, transaccion, Clock.systemUTC());
    }

    /** Editar una versión que no rige ningún periodo (F04-H02). */
    @Bean
    public EditarVersion editarVersion(
            RepositorioVersiones repositorio, RegistroDeCambios registro, Transaccion transaccion) {
        return new EditarVersionServicio(repositorio, registro, transaccion, Clock.systemUTC());
    }

    /** Asociar una versión a un periodo; Académico lo usará en F02-H01 (F04-H02). */
    /**
     * La carga de Normativa (F01-H04, MD-13, ADR-022): la 07, la política de carga horaria, como
     * una versión nueva o editada, completa o nada.
     */
    @Bean
    public AplicadorDeCargas aplicadorNormativa(
            Transaccion transaccionNormativa,
            RepositorioVersiones repositorioVersiones,
            CrearVersion crearVersion,
            EditarVersion editarVersion) {
        return CargasDeNormativa.aplicador(
                transaccionNormativa, repositorioVersiones, crearVersion, editarVersion);
    }

    @Bean
    public AsociarVersionAPeriodo asociarVersionAPeriodo(
            RepositorioVersiones repositorio, RegistroDeCambios registro, Transaccion transaccion) {
        return new AsociarVersionAPeriodoServicio(
                repositorio, registro, transaccion, Clock.systemUTC());
    }
}
