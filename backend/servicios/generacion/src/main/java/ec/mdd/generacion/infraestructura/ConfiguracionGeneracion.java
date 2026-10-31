package ec.mdd.generacion.infraestructura;

import ec.mdd.generacion.adaptadores.salida.DatosDesdeProcesoDatos;
import ec.mdd.generacion.adaptadores.salida.PublicadorDeEventosSpring;
import ec.mdd.generacion.adaptadores.salida.RepositorioEventosDePruebaJdbc;
import ec.mdd.generacion.adaptadores.salida.SinAsignacionesTodavia;
import ec.mdd.generacion.aplicacion.ConsultarAfinidadServicio;
import ec.mdd.generacion.aplicacion.ConsultarSugerenciaServicio;
import ec.mdd.generacion.aplicacion.PublicarEventoDePruebaServicio;
import ec.mdd.generacion.puertos.entrada.ConsultarAfinidad;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.HorasAsignadas;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

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

    /** Las horas asignadas: nadie tiene hasta que F06-H03 traiga las asignaciones (FR-009). */
    @Bean
    public HorasAsignadas horasAsignadas() {
        return new SinAsignacionesTodavia();
    }

    /** Los docentes sugeridos para una asignatura-paralelo (RF-25). */
    @Bean
    public ConsultarSugerencia consultarSugerencia(
            DatosParaSugerencia datos, DatosParaAfinidad afinidad, HorasAsignadas horas) {
        return new ConsultarSugerenciaServicio(datos, afinidad, horas);
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
