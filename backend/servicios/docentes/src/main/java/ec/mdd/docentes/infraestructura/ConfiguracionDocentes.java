package ec.mdd.docentes.infraestructura;

import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.docentes.adaptadores.salida.AcademicoDesdePuertos;
import ec.mdd.docentes.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioCargosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioDocentesJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioProyectosJdbc;
import ec.mdd.docentes.adaptadores.salida.TransaccionSpring;
import ec.mdd.docentes.aplicacion.ConsultarAsignablesServicio;
import ec.mdd.docentes.aplicacion.ConsultarCargosServicio;
import ec.mdd.docentes.aplicacion.ConsultarDocentesServicio;
import ec.mdd.docentes.aplicacion.ConsultarIdentificacionesServicio;
import ec.mdd.docentes.aplicacion.ConsultarProyectosServicio;
import ec.mdd.docentes.aplicacion.MantenerCargosServicio;
import ec.mdd.docentes.aplicacion.MantenerDocentesServicio;
import ec.mdd.docentes.aplicacion.MantenerProyectosServicio;
import ec.mdd.docentes.aplicacion.cargas.CargasDeDocentes;
import ec.mdd.docentes.puertos.entrada.ConsultarAsignablesDelPeriodo;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import ec.mdd.docentes.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Docentes (F03-H04): el repositorio del esquema {@code docentes}, su registro de cambios,
 * la transacción, las referencias de Académico pedidas por su interfaz publicada (research §4) y
 * los casos de uso. Los nombres de los beans no se repiten con los de Académico y Normativa, que
 * viven en el mismo proceso.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionDocentes {

    @Bean
    @DependsOn("flywayDocentes")
    public RepositorioDocentes repositorioDocentes(JdbcClient jdbc) {
        return new RepositorioDocentesJdbc(jdbc);
    }

    @Bean
    public RegistroDeCambios registroDeCambiosDocentes(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    @Bean
    public Transaccion transaccionDocentes(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Las carreras, las asignaturas y los campos amplios de Académico (research §4). */
    @Bean
    public ReferenciasAcademicas referenciasAcademicas(ConsultarReferencias consultar) {
        return new AcademicoDesdePuertos(consultar);
    }

    /** Buscar y consultar los docentes con su ficha (FR-007, FR-008). */
    @Bean
    public ConsultarDocentes consultarDocentes(
            RepositorioDocentes repositorio, ReferenciasAcademicas referencias) {
        return new ConsultarDocentesServicio(repositorio, referencias);
    }

    /**
     * Registrar, corregir y cambiar el estado de los docentes, con su registro (FR-003, FR-010).
     */
    /**
     * Las cargas de Docentes (F01-H04, MD-13, ADR-022): la vista previa y la aplicación completa o
     * nada de cada lote de las cargas 06, 08 y 09, con los casos de uso de siempre.
     */
    @Bean
    public AplicadorDeCargas aplicadorDocentes(
            Transaccion transaccionDocentes,
            RepositorioDocentes repositorioDocentes,
            MantenerDocentes mantenerDocentes,
            RepositorioCargos repositorioCargos,
            MantenerCargos mantenerCargos,
            RepositorioProyectos repositorioProyectos,
            MantenerProyectos mantenerProyectos) {
        return CargasDeDocentes.aplicador(
                transaccionDocentes,
                repositorioDocentes,
                mantenerDocentes,
                repositorioCargos,
                mantenerCargos,
                repositorioProyectos,
                mantenerProyectos);
    }

    /** La API publicada para Cargas (F01-H03): solo las identificaciones. */
    @Bean
    public ConsultarIdentificaciones consultarIdentificaciones(RepositorioDocentes repositorio) {
        return new ConsultarIdentificacionesServicio(repositorio);
    }

    @Bean
    public MantenerDocentes mantenerDocentes(
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registroDeCambiosDocentes,
            Transaccion transaccionDocentes) {
        return new MantenerDocentesServicio(
                repositorio,
                referencias,
                registroDeCambiosDocentes,
                transaccionDocentes,
                Clock.systemUTC());
    }

    // Las advertencias de los cargos dependen del día, que es el de la institución (Ecuador).
    private static final Clock RELOJ_LOCAL = Clock.system(ZoneId.of("America/Guayaquil"));

    @Bean
    @DependsOn("flywayDocentes")
    public RepositorioCargos repositorioCargos(JdbcClient jdbc) {
        return new RepositorioCargosJdbc(jdbc);
    }

    @Bean
    @DependsOn("flywayDocentes")
    public RepositorioProyectos repositorioProyectos(JdbcClient jdbc) {
        return new RepositorioProyectosJdbc(jdbc);
    }

    /** Consultar los cargos con sus advertencias de hoy (F03-H05, FR-006). */
    @Bean
    public ConsultarCargos consultarCargos(
            RepositorioCargos repositorioCargos, RepositorioDocentes repositorio) {
        return new ConsultarCargosServicio(repositorioCargos, repositorio, RELOJ_LOCAL);
    }

    /** Registrar, corregir y finalizar los cargos, con su registro (F03-H05, FR-003, FR-012). */
    @Bean
    public MantenerCargos mantenerCargos(
            RepositorioCargos repositorioCargos,
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registroDeCambiosDocentes,
            Transaccion transaccionDocentes) {
        return new MantenerCargosServicio(
                repositorioCargos,
                repositorio,
                referencias,
                registroDeCambiosDocentes,
                transaccionDocentes,
                RELOJ_LOCAL);
    }

    /** Consultar los proyectos y si son asignables en un periodo (F03-H05, FR-009). */
    @Bean
    public ConsultarProyectos consultarProyectos(
            RepositorioProyectos repositorioProyectos,
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias) {
        return new ConsultarProyectosServicio(repositorioProyectos, repositorio, referencias);
    }

    /** Si un periodo tiene proyectos asignables, para «sin proyectos» de Cargas (F01-H05). */
    @Bean
    public ConsultarAsignablesDelPeriodo consultarAsignablesDelPeriodo(
            RepositorioProyectos repositorioProyectos, ReferenciasAcademicas referencias) {
        return new ConsultarAsignablesServicio(repositorioProyectos, referencias);
    }

    /** Registrar y corregir los proyectos, con su registro (F03-H05, FR-003, FR-012). */
    @Bean
    public MantenerProyectos mantenerProyectos(
            RepositorioProyectos repositorioProyectos,
            RepositorioDocentes repositorio,
            ReferenciasAcademicas referencias,
            RegistroDeCambios registroDeCambiosDocentes,
            Transaccion transaccionDocentes) {
        return new MantenerProyectosServicio(
                repositorioProyectos,
                repositorio,
                referencias,
                registroDeCambiosDocentes,
                transaccionDocentes,
                Clock.systemUTC());
    }
}
