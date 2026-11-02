package ec.mdd.generacion.infraestructura;

import ec.mdd.generacion.adaptadores.salida.DatosDesdeProcesoDatos;
import ec.mdd.generacion.adaptadores.salida.PublicadorDeEventosSpring;
import ec.mdd.generacion.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioEventosDePruebaJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.aplicacion.AsignarActividadServicio;
import ec.mdd.generacion.aplicacion.ConsultarActividadesPosiblesServicio;
import ec.mdd.generacion.aplicacion.ConsultarActividadesServicio;
import ec.mdd.generacion.aplicacion.ConsultarAfinidadServicio;
import ec.mdd.generacion.aplicacion.ConsultarCargaHorariaServicio;
import ec.mdd.generacion.aplicacion.ConsultarParcialesServicio;
import ec.mdd.generacion.aplicacion.ConsultarSugerenciaServicio;
import ec.mdd.generacion.aplicacion.CrearDistributivoServicio;
import ec.mdd.generacion.aplicacion.ProponerDocenteServicio;
import ec.mdd.generacion.aplicacion.PublicarEventoDePruebaServicio;
import ec.mdd.generacion.puertos.entrada.AsignarActividad;
import ec.mdd.generacion.puertos.entrada.ConsultarActividades;
import ec.mdd.generacion.puertos.entrada.ConsultarActividadesPosibles;
import ec.mdd.generacion.puertos.entrada.ConsultarAfinidad;
import ec.mdd.generacion.puertos.entrada.ConsultarCargaHoraria;
import ec.mdd.generacion.puertos.entrada.ConsultarParciales;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.ProponerDocente;
import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import ec.mdd.generacion.puertos.salida.Transaccion;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla el evento de prueba de generacion (D-48), la afinidad (F06-H01) y la sugerencia
 * (F06-H02).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionGeneracion {

    /**
     * Los datos de la afinidad y la sugerencia, por las rutas internas del proceso {@code datos}
     * (ADR-023).
     */
    @Bean
    public DatosDesdeProcesoDatos datosDesdeProcesoDatos(
            ConsultasEntreProcesos consultas, @Value("${mdd.procesos.datos}") URI datos) {
        return new DatosDesdeProcesoDatos(consultas, datos);
    }

    /** La afinidad de un docente en la oferta de un periodo (RF-17). */
    @Bean
    public ConsultarAfinidad consultarAfinidad(DatosParaAfinidad datos) {
        return new ConsultarAfinidadServicio(datos);
    }

    /** Los distributivos, en su esquema (F06-H03). */
    @Bean
    public RepositorioDistributivos repositorioDistributivos(JdbcClient jdbc) {
        return new RepositorioDistributivosJdbc(jdbc);
    }

    /** La bitácora de los cambios del distributivo (F06-H03). */
    @Bean
    public RegistroDeCambios registroDeCambiosGeneracion(JdbcClient jdbc) {
        return new RegistroDeCambiosJdbc(jdbc);
    }

    /** Las transacciones del distributivo (F06-H03). */
    @Bean
    public Transaccion transaccionGeneracion(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Los docentes sugeridos para una asignatura-paralelo (RF-25). */
    @Bean
    public ConsultarSugerencia consultarSugerencia(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos) {
        return new ConsultarSugerenciaServicio(datos, afinidad, actividades, distributivos);
    }

    /** Crear el distributivo y consultarlo (RF-56). */
    @Bean
    public CrearDistributivoServicio crearDistributivo(
            DatosParaSugerencia datos,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion) {
        return new CrearDistributivoServicio(
                datos, distributivos, registro, transaccion, Clock.systemUTC());
    }

    /** Proponer y quitar docentes (RF-24). */
    @Bean
    public ProponerDocente proponerDocente(
            DatosParaSugerencia datos,
            DatosParaAfinidad afinidad,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion) {
        return new ProponerDocenteServicio(
                datos,
                afinidad,
                actividades,
                distributivos,
                registro,
                transaccion,
                Clock.systemUTC());
    }

    /** Asignar y quitar actividades no lectivas (RF-26). */
    @Bean
    public AsignarActividad asignarActividad(
            DatosParaSugerencia datos,
            DatosParaActividades actividades,
            RepositorioDistributivos distributivos,
            RegistroDeCambios registro,
            Transaccion transaccion) {
        return new AsignarActividadServicio(
                datos, actividades, distributivos, registro, transaccion, Clock.systemUTC());
    }

    /** Lo que el formulario puede ofrecer para un docente (RN-22, RN-25). */
    @Bean
    public ConsultarActividadesPosibles consultarActividadesPosibles(
            DatosParaSugerencia datos, DatosParaActividades actividades) {
        return new ConsultarActividadesPosiblesServicio(datos, actividades);
    }

    /** La carga horaria de un docente (RF-27, D-82). */
    @Bean
    public ConsultarCargaHoraria consultarCargaHoraria(
            DatosParaSugerencia datos,
            DatosParaActividades normativa,
            RepositorioDistributivos distributivos) {
        return new ConsultarCargaHorariaServicio(datos, normativa, distributivos);
    }

    /** Las actividades del periodo, por carrera y docente (RF-26). */
    @Bean
    public ConsultarActividades consultarActividades(
            DatosParaSugerencia datos, RepositorioDistributivos distributivos) {
        return new ConsultarActividadesServicio(datos, distributivos);
    }

    /** Los parciales del periodo (RF-24). */
    @Bean
    public ConsultarParciales consultarParciales(
            DatosParaSugerencia datos, RepositorioDistributivos distributivos) {
        return new ConsultarParcialesServicio(datos, distributivos);
    }

    @Bean
    public RepositorioEventosDePrueba repositorioEventosDePrueba(JdbcClient jdbc) {
        return new RepositorioEventosDePruebaJdbc(jdbc);
    }

    @Bean
    public PublicadorDeEventos publicadorDeEventos(ApplicationEventPublisher eventos) {
        return new PublicadorDeEventosSpring(eventos);
    }

    @Bean
    public PublicarEventoDePrueba publicarEventoDePrueba(
            RepositorioEventosDePrueba repositorio, PublicadorDeEventos publicador) {
        return new PublicarEventoDePruebaServicio(repositorio, publicador, Clock.systemUTC());
    }
}
