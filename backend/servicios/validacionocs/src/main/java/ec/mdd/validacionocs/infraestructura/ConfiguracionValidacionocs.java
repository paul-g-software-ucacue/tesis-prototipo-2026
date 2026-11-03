package ec.mdd.validacionocs.infraestructura;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.validacionocs.adaptadores.salida.DatosDesdeProcesos;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.aplicacion.CargarConfiguracionServicio;
import ec.mdd.validacionocs.aplicacion.ConsultarConfiguracionServicio;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.PlantillaDelFlujo;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Ensambla la configuración del flujo (F12-H01). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionValidacionocs {

    /** Académico en el proceso datos y Roles en el proceso identidad (ADR-023). */
    @Bean
    public DatosDelPeriodo datosDelPeriodo(
            ConsultasEntreProcesos consultas,
            @Value("${mdd.procesos.datos}") URI datos,
            @Value("${mdd.procesos.identidad}") URI identidad) {
        return new DatosDesdeProcesos(consultas, datos, identidad);
    }

    /** El lector de los archivos. */
    @Bean
    public LectorDeConfiguracion lectorDeConfiguracion() {
        return new LectorYaml();
    }

    /** La plantilla, que es la configuración por defecto (D-89). */
    @Bean
    public PlantillaDelFlujo plantillaDelFlujo() {
        return new PlantillaEnRecursos();
    }

    /** Las cargas de cada periodo. */
    @Bean
    public RepositorioConfiguracion repositorioConfiguracion(JdbcClient jdbc) {
        return new RepositorioConfiguracionJdbc(jdbc);
    }

    /** Cargar. */
    @Bean
    public CargarConfiguracion cargarConfiguracion(
            DatosDelPeriodo datos,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio) {
        return new CargarConfiguracionServicio(datos, lector, repositorio, Clock.systemUTC());
    }

    /** Consultar. */
    @Bean
    public ConsultarConfiguracion consultarConfiguracion(
            DatosDelPeriodo datos,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            PlantillaDelFlujo plantilla) {
        return new ConsultarConfiguracionServicio(datos, lector, repositorio, plantilla);
    }
}
