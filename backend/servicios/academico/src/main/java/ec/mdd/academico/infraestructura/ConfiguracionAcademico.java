package ec.mdd.academico.infraestructura;

import ec.mdd.academico.adaptadores.salida.NormativaDesdePuertos;
import ec.mdd.academico.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioPeriodosJdbc;
import ec.mdd.academico.adaptadores.salida.TransaccionSpring;
import ec.mdd.academico.aplicacion.AbrirPeriodoServicio;
import ec.mdd.academico.aplicacion.CambiarEstadoPeriodoServicio;
import ec.mdd.academico.aplicacion.ConsultarPeriodosServicio;
import ec.mdd.academico.aplicacion.CorregirPeriodoServicio;
import ec.mdd.academico.aplicacion.RegistrarPeriodoServicio;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.CambiarEstadoPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Académico: el repositorio del esquema {@code academico}, su registro de cambios, la
 * transacción, la versión de normativa de cada periodo, pedida a Normativa por su interfaz
 * publicada (research §4), y los casos de uso. Los nombres de los beans no se repiten con los de
 * Normativa, que vive en el mismo proceso.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionAcademico {

    @Bean
    @DependsOn("flywayAcademico")
    public RepositorioPeriodos repositorioPeriodos(JdbcClient jdbc) {
        return new RepositorioPeriodosJdbc(jdbc);
    }

    @Bean
    public RegistroDeCambios registroDeCambiosAcademico(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionAcademico(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** La versión de normativa de cada periodo, que guarda Normativa (research §2 y §4). */
    @Bean
    public NormativaDelPeriodo normativaDelPeriodo(
            ConsultarNormativa consultar, AsociarVersionAPeriodo asociar) {
        return new NormativaDesdePuertos(consultar, asociar);
    }

    /** La consulta de los periodos con su versión (FR-009). */
    @Bean
    public ConsultarPeriodos consultarPeriodos(
            RepositorioPeriodos repositorio, NormativaDelPeriodo normativa) {
        return new ConsultarPeriodosServicio(repositorio, normativa);
    }

    /** Registrar un periodo, en Planificación (FR-001); la carga 3 lo usará (F01). */
    @Bean
    public RegistrarPeriodo registrarPeriodo(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new RegistrarPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }

    /** Abrir un periodo con su versión de normativa (FR-002, research §3). */
    @Bean
    public AbrirPeriodo abrirPeriodo(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new AbrirPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }

    /** Pasar un periodo al estado siguiente (FR-004). */
    @Bean
    public CambiarEstadoPeriodo cambiarEstadoPeriodo(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new CambiarEstadoPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }

    /** Corregir el nombre y las fechas de un periodo que no está cerrado (FR-005, FR-006). */
    @Bean
    public CorregirPeriodo corregirPeriodo(
            RepositorioPeriodos repositorio,
            NormativaDelPeriodo normativa,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new CorregirPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }
}
