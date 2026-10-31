package ec.mdd.academico.infraestructura;

import ec.mdd.academico.adaptadores.salida.NormativaDesdePuertos;
import ec.mdd.academico.adaptadores.salida.PublicadorDeAvisosSpring;
import ec.mdd.academico.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioCarrerasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioEstructuraJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioMatriculasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioOfertaJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioPeriodosJdbc;
import ec.mdd.academico.adaptadores.salida.TransaccionSpring;
import ec.mdd.academico.aplicacion.AbrirPeriodoServicio;
import ec.mdd.academico.aplicacion.CambiarEstadoPeriodoServicio;
import ec.mdd.academico.aplicacion.ConsultarAsignaturasOfertadasServicio;
import ec.mdd.academico.aplicacion.ConsultarCarrerasServicio;
import ec.mdd.academico.aplicacion.ConsultarEstructuraServicio;
import ec.mdd.academico.aplicacion.ConsultarOfertaServicio;
import ec.mdd.academico.aplicacion.ConsultarParaleloDeLaOfertaServicio;
import ec.mdd.academico.aplicacion.ConsultarPeriodosServicio;
import ec.mdd.academico.aplicacion.ConsultarReferenciasServicio;
import ec.mdd.academico.aplicacion.CorregirPeriodoServicio;
import ec.mdd.academico.aplicacion.MantenerCarrerasServicio;
import ec.mdd.academico.aplicacion.MantenerEstructuraServicio;
import ec.mdd.academico.aplicacion.MantenerMatriculasServicio;
import ec.mdd.academico.aplicacion.MantenerOfertaServicio;
import ec.mdd.academico.aplicacion.RegistrarPeriodoServicio;
import ec.mdd.academico.aplicacion.cargas.CargasDeAcademico;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.CambiarEstadoPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.entrada.ConsultarEstructura;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.ConsultarParaleloDeLaOferta;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.entrada.MantenerMatriculas;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.AvisosDelPeriodo;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla Académico: el repositorio del esquema {@code academico}, su registro de cambios, la
 * transacción, la versión de normativa de cada periodo, pedida a Normativa por su interfaz
 * publicada (research §4), los repositorios de la estructura orgánica (F03-H01) y de las carreras
 * (F03-H02), de la oferta (F03-H03), las referencias que publica para Docentes (F03-H04) y los
 * casos de uso. Los nombres de los beans no se repiten con los de Normativa, que vive en el mismo
 * proceso.
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

    /** Los avisos a Roles de cada periodo registrado o con un estado nuevo (F05-H03, ADR-018). */
    @Bean
    public AvisosDelPeriodo avisosDelPeriodo(ApplicationEventPublisher eventos) {
        return new PublicadorDeAvisosSpring(eventos, Clock.systemUTC());
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
            AvisosDelPeriodo avisos,
            Transaccion transaccionAcademico) {
        return new RegistrarPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                avisos,
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
            AvisosDelPeriodo avisos,
            Transaccion transaccionAcademico) {
        return new CambiarEstadoPeriodoServicio(
                repositorio,
                normativa,
                registroDeCambiosAcademico,
                avisos,
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

    @Bean
    @DependsOn("flywayAcademico")
    public RepositorioEstructura repositorioEstructura(JdbcClient jdbc) {
        return new RepositorioEstructuraJdbc(jdbc);
    }

    /** La consulta de la estructura orgánica y del catálogo de gestiones (F03-H01). */
    @Bean
    public ConsultarEstructura consultarEstructura(RepositorioEstructura repositorio) {
        return new ConsultarEstructuraServicio(repositorio);
    }

    /** Mantener la estructura orgánica, con su registro (F03-H01). */
    @Bean
    public MantenerEstructura mantenerEstructura(
            RepositorioEstructura repositorio,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new MantenerEstructuraServicio(
                repositorio, registroDeCambiosAcademico, transaccionAcademico, Clock.systemUTC());
    }

    @Bean
    @DependsOn("flywayAcademico")
    public RepositorioCarreras repositorioCarreras(JdbcClient jdbc) {
        return new RepositorioCarrerasJdbc(jdbc);
    }

    /** La consulta de las carreras, sus mallas y sus asignaturas (F03-H02). */
    @Bean
    public ConsultarCarreras consultarCarreras(RepositorioCarreras repositorio) {
        return new ConsultarCarrerasServicio(repositorio);
    }

    /** Las asignaturas ofertadas de un periodo, para otros procesos (F06-H01, ADR-023). */
    @Bean
    public ConsultarAsignaturasOfertadas consultarAsignaturasOfertadas(
            RepositorioPeriodos periodos, RepositorioOferta oferta, RepositorioCarreras carreras) {
        return new ConsultarAsignaturasOfertadasServicio(periodos, oferta, carreras);
    }

    /** Una asignatura-paralelo de la oferta, para otros procesos (F06-H02, ADR-023). */
    @Bean
    public ConsultarParaleloDeLaOferta consultarParaleloDeLaOferta(ConsultarOferta consultar) {
        return new ConsultarParaleloDeLaOfertaServicio(consultar);
    }

    /** Las referencias que Docentes valida y muestra (F03-H04): su API publicada en el proceso. */
    @Bean
    public ConsultarReferencias consultarReferencias(
            RepositorioCarreras repositorio,
            RepositorioEstructura estructura,
            RepositorioPeriodos periodos,
            NormativaDelPeriodo normativa) {
        return new ConsultarReferenciasServicio(repositorio, estructura, periodos, normativa);
    }

    /** Mantener las carreras, sus mallas y sus asignaturas, con su registro (F03-H02). */
    @Bean
    public MantenerCarreras mantenerCarreras(
            RepositorioCarreras repositorio,
            RepositorioEstructura repositorioEstructura,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new MantenerCarrerasServicio(
                repositorio,
                repositorioEstructura,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }

    @Bean
    @DependsOn("flywayAcademico")
    public RepositorioMatriculas repositorioMatriculas(JdbcClient jdbc) {
        return new RepositorioMatriculasJdbc(jdbc);
    }

    @Bean
    public MantenerMatriculas mantenerMatriculas(
            RepositorioMatriculas repositorioMatriculas,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new MantenerMatriculasServicio(
                repositorioMatriculas,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }

    /**
     * Las cargas de Académico (F01-H04, MD-13, ADR-022): la vista previa y la aplicación completa o
     * nada de cada lote de las cargas 01 a 05 y 10, con los casos de uso de siempre.
     */
    @Bean
    public AplicadorDeCargas aplicadorAcademico(
            Transaccion transaccionAcademico,
            RepositorioEstructura repositorioEstructura,
            MantenerEstructura mantenerEstructura,
            RepositorioCarreras repositorioCarreras,
            MantenerCarreras mantenerCarreras,
            RepositorioPeriodos repositorioPeriodos,
            RegistrarPeriodo registrarPeriodo,
            CorregirPeriodo corregirPeriodo,
            RepositorioOferta repositorioOferta,
            MantenerOferta mantenerOferta,
            RepositorioMatriculas repositorioMatriculas,
            MantenerMatriculas mantenerMatriculas) {
        return CargasDeAcademico.aplicador(
                new CargasDeAcademico.Dependencias(
                        transaccionAcademico,
                        repositorioEstructura,
                        mantenerEstructura,
                        repositorioCarreras,
                        mantenerCarreras,
                        repositorioPeriodos,
                        registrarPeriodo,
                        corregirPeriodo,
                        repositorioOferta,
                        mantenerOferta,
                        repositorioMatriculas,
                        mantenerMatriculas));
    }

    @Bean
    @DependsOn("flywayAcademico")
    public RepositorioOferta repositorioOferta(JdbcClient jdbc) {
        return new RepositorioOfertaJdbc(jdbc);
    }

    /** La consulta de la oferta del periodo con sus horas a cubrir (F03-H03). */
    @Bean
    public ConsultarOferta consultarOferta(
            RepositorioPeriodos repositorioPeriodos,
            RepositorioOferta repositorio,
            RepositorioCarreras repositorioCarreras,
            RepositorioEstructura repositorioEstructura) {
        return new ConsultarOfertaServicio(
                repositorioPeriodos, repositorio, repositorioCarreras, repositorioEstructura);
    }

    /** Mantener la oferta del periodo, con su registro (F03-H03). */
    @Bean
    public MantenerOferta mantenerOferta(
            RepositorioPeriodos repositorioPeriodos,
            RepositorioOferta repositorio,
            RepositorioCarreras repositorioCarreras,
            RepositorioEstructura repositorioEstructura,
            RegistroDeCambios registroDeCambiosAcademico,
            Transaccion transaccionAcademico) {
        return new MantenerOfertaServicio(
                repositorioPeriodos,
                repositorio,
                repositorioCarreras,
                repositorioEstructura,
                registroDeCambiosAcademico,
                transaccionAcademico,
                Clock.systemUTC());
    }
}
