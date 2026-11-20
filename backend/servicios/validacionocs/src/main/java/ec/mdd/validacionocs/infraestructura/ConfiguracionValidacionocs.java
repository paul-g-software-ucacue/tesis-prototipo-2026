package ec.mdd.validacionocs.infraestructura;

import ec.mdd.firma.puertos.entrada.AnularFirmas;
import ec.mdd.firma.puertos.entrada.PrepararDocumentoFirmable;
import ec.mdd.generacion.puertos.entrada.ParcialesDelDistributivo;
import ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.validacionocs.adaptadores.entrada.CierreDeLaFirma;
import ec.mdd.validacionocs.adaptadores.entrada.EtapaParaGeneracion;
import ec.mdd.validacionocs.adaptadores.salida.AlmacenDeResolucionesEnDisco;
import ec.mdd.validacionocs.adaptadores.salida.AnulacionDeLasFirmas;
import ec.mdd.validacionocs.adaptadores.salida.AvisoDeFirmaSpring;
import ec.mdd.validacionocs.adaptadores.salida.DatosDesdeProcesos;
import ec.mdd.validacionocs.adaptadores.salida.DistributivoDesdeGeneracion;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.PreparacionDelDocumento;
import ec.mdd.validacionocs.adaptadores.salida.PublicadorDeCambios;
import ec.mdd.validacionocs.adaptadores.salida.PublicadorDeNotificaciones;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.CargarConfiguracionServicio;
import ec.mdd.validacionocs.aplicacion.CompletarFirmasServicio;
import ec.mdd.validacionocs.aplicacion.ConsultarConfiguracionServicio;
import ec.mdd.validacionocs.aplicacion.DescargarResolucionServicio;
import ec.mdd.validacionocs.aplicacion.EtapaDelConsolidadoServicio;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.aplicacion.IniciarReformaServicio;
import ec.mdd.validacionocs.aplicacion.PublicarDistributivoServicio;
import ec.mdd.validacionocs.aplicacion.RegistrarRechazoEnFirmaServicio;
import ec.mdd.validacionocs.aplicacion.RegistrarResolucionServicio;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.RegistrarRechazoEnFirma;
import ec.mdd.validacionocs.puertos.salida.AlmacenDeResoluciones;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
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

    /** Cada transición, como una solicitud de notificaciones hacia Notificaciones (F16-H01). */
    @Bean
    public NotificacionDeTransiciones notificacionDeTransiciones(
            ApplicationEventPublisher eventos,
            ConsultasEntreProcesos consultas,
            @Value("${mdd.procesos.datos}") URI datos,
            @Value("${mdd.procesos.identidad}") URI identidad) {
        return new PublicadorDeNotificaciones(
                eventos, new DatosDesdeProcesos(consultas, datos, identidad), Clock.systemUTC());
    }

    /** Al entrar en firma, le entrega la cadena a Firma después de confirmar (MD-05, F13-H02). */
    @Bean
    public PreparacionDelDocumento preparacionDelDocumento(PrepararDocumentoFirmable firma) {
        return new PreparacionDelDocumento(firma);
    }

    /**
     * Con la versión nueva, le ordena a Firma anular las firmas de la rechazada después de
     * confirmar (MD-05, F12-H04).
     */
    @Bean
    public AnulacionDeLasFirmas anulacionDeLasFirmas(AnularFirmas anularFirmas) {
        return new AnulacionDeLasFirmas(anularFirmas);
    }

    /** Registrar la resolución del OCS y aprobar el distributivo (F15-H01, D-107). */
    @Bean
    public RegistrarResolucionServicio registrarResolucion(
            RepositorioProceso repositorioProceso,
            Distributivo distributivoDeGeneracion,
            DatosDelPeriodo datos,
            AlmacenDeResoluciones almacenDeResoluciones,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            FlujoDelDistributivoServicio flujoDelDistributivo,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new RegistrarResolucionServicio(
                repositorioProceso,
                distributivoDeGeneracion,
                datos,
                almacenDeResoluciones,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                flujoDelDistributivo,
                Clock.systemUTC(),
                notificacionDeTransiciones);
    }

    /** Publicar el distributivo aprobado (F18-H01, D-111). */
    @Bean
    public PublicarDistributivoServicio publicarDistributivo(
            RepositorioProceso repositorioProceso,
            Distributivo distributivoDeGeneracion,
            DatosDelPeriodo datos,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            FlujoDelDistributivoServicio flujoDelDistributivo,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new PublicarDistributivoServicio(
                repositorioProceso,
                distributivoDeGeneracion,
                datos,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                flujoDelDistributivo,
                Clock.systemUTC(),
                notificacionDeTransiciones);
    }

    /** Iniciar una reforma del distributivo publicado (F18-H02, D-113). */
    @Bean
    public IniciarReformaServicio iniciarReforma(
            RepositorioProceso repositorioProceso,
            Distributivo distributivoDeGeneracion,
            DatosDelPeriodo datos,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            FlujoDelDistributivoServicio flujoDelDistributivo,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new IniciarReformaServicio(
                repositorioProceso,
                distributivoDeGeneracion,
                datos,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                flujoDelDistributivo,
                Clock.systemUTC(),
                notificacionDeTransiciones);
    }

    /** Descargar el PDF de la resolución del OCS (F15-H01). */
    @Bean
    public DescargarResolucionServicio descargarResolucion(
            RepositorioProceso repositorioProceso, AlmacenDeResoluciones almacenDeResoluciones) {
        return new DescargarResolucionServicio(repositorioProceso, almacenDeResoluciones);
    }

    /** Los PDF de las resoluciones del OCS, en el volumen del proceso (F15-H01, D-88). */
    @Bean
    public AlmacenDeResoluciones almacenDeResoluciones(
            @org.springframework.beans.factory.annotation.Value("${mdd.archivos.raiz}")
                    java.nio.file.Path raiz) {
        return new AlmacenDeResolucionesEnDisco(raiz);
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
    /** Lo que Generación pregunta antes de cambiar el consolidado (#215, D-99). */
    @Bean
    public EtapaDelConsolidado etapaDelConsolidado(RepositorioProceso procesos) {
        return new EtapaParaGeneracion(new EtapaDelConsolidadoServicio(procesos));
    }

    @Bean
    public Distributivo distributivoDeGeneracion(ParcialesDelDistributivo parciales) {
        return new DistributivoDesdeGeneracion(parciales);
    }

    /**
     * Consultar el flujo, enviar el parcial, el avance genérico (F12-H02) y enviar a revisión
     * (F10-H03).
     */
    @Bean
    public FlujoDelDistributivoServicio flujoDelDistributivo(
            Distributivo distributivoDeGeneracion,
            RepositorioProceso repositorioProceso,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            PlantillaDelFlujo plantilla,
            DatosDelPeriodo datos,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            ApplicationEventPublisher eventos,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new FlujoDelDistributivoServicio(
                distributivoDeGeneracion,
                repositorioProceso,
                new EtapasAlCrearse(repositorio, lector, plantilla),
                datos,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                Clock.systemUTC(),
                new AvisoDeFirmaSpring(eventos),
                notificacionDeTransiciones);
    }

    /** El paso a EN_OCS con la cadena de firmas completa (F14-H03, D-104). */
    @Bean
    public CompletarFirmasServicio completarFirmas(
            RepositorioProceso repositorioProceso,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new CompletarFirmasServicio(
                repositorioProceso,
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                notificacionDeTransiciones);
    }

    /**
     * La vuelta a revisión con un rechazo en firma (F14-H04, D-105), con la versión nueva y la
     * orden de anular las firmas (F12-H04, D-106).
     */
    @Bean
    public RegistrarRechazoEnFirmaServicio registrarRechazoEnFirma(
            RepositorioProceso repositorioProceso,
            Distributivo distributivoDeGeneracion,
            RegistroDeCambios registroDeCambiosValidacionocs,
            Transaccion transaccionValidacionocs,
            ApplicationEventPublisher eventos,
            NotificacionDeTransiciones notificacionDeTransiciones) {
        return new RegistrarRechazoEnFirmaServicio(
                repositorioProceso,
                distributivoDeGeneracion,
                new AvisoDeFirmaSpring(eventos),
                registroDeCambiosValidacionocs,
                transaccionValidacionocs,
                notificacionDeTransiciones);
    }

    /**
     * Escucha los avisos de cadena completa y de rechazo en firma de Firma después de confirmar
     * (MD-05, F14-H03, F14-H04).
     */
    @Bean
    public CierreDeLaFirma cierreDeLaFirma(
            CompletarFirmas completarFirmas, RegistrarRechazoEnFirma registrarRechazoEnFirma) {
        return new CierreDeLaFirma(completarFirmas, registrarRechazoEnFirma);
    }
}
