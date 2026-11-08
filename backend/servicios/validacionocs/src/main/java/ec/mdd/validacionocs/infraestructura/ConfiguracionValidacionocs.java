package ec.mdd.validacionocs.infraestructura;

import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.validacionocs.adaptadores.salida.DatosDesdeProcesos;
import ec.mdd.validacionocs.adaptadores.salida.DistributivoDesdeGeneracion;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.PublicadorDeCambios;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.CargarConfiguracionServicio;
import ec.mdd.validacionocs.aplicacion.ConsultarConfiguracionServicio;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.PlantillaDelFlujo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

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

    /** Cada carga, como una entrada de la bitácora de Usuarios (F17-H01). */
    @Bean
    public RegistroDeCambios registroDeCambiosValidacionocs(ApplicationEventPublisher eventos) {
        return new PublicadorDeCambios(eventos, Clock.systemUTC());
    }

    /** Las transacciones de Validación OCS (F17-H01). */
    @Bean
    public Transaccion transaccionValidacionocs(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Cargar. */
    @Bean
    public CargarConfiguracion cargarConfiguracion(
            DatosDelPeriodo datos,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs) {
        return new CargarConfiguracionServicio(
                datos,
                lector,
                repositorio,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                Clock.systemUTC());
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

    /** Los procesos de aprobación (F12-H02). */
    @Bean
    public RepositorioProceso repositorioProceso(JdbcClient jdbc) {
        return new RepositorioProcesoJdbc(jdbc);
    }

    /** El distributivo, por la interfaz publicada de Generación en el mismo proceso (MD-04). */
    @Bean
    public Distributivo distributivoDeGeneracion(ParcialesDelDistributivo parciales) {
        return new DistributivoDesdeGeneracion(parciales);
    }

    /** Consultar el flujo, enviar el parcial y el avance genérico (F12-H02). */
    @Bean
    public FlujoDelDistributivoServicio flujoDelDistributivo(
            Distributivo distributivoDeGeneracion,
            RepositorioProceso repositorioProceso,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            PlantillaDelFlujo plantilla,
            DatosDelPeriodo datos,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs) {
        return new FlujoDelDistributivoServicio(
                distributivoDeGeneracion,
                repositorioProceso,
                new EtapasAlCrearse(repositorio, lector, plantilla),
                datos,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                Clock.systemUTC());
    }
}
