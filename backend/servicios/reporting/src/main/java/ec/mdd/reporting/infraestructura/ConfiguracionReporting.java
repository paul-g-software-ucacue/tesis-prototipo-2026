package ec.mdd.reporting.infraestructura;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.reporting.adaptadores.salida.ContenidoDesdeGeneracion;
import ec.mdd.reporting.adaptadores.salida.GeneradorDeReportePdfa;
import ec.mdd.reporting.adaptadores.salida.PublicacionesDesdeAprobacion;
import ec.mdd.reporting.adaptadores.salida.RegistroDeDescargasEventos;
import ec.mdd.reporting.adaptadores.salida.TransaccionSpring;
import ec.mdd.reporting.aplicacion.ConsultarReportesServicio;
import ec.mdd.reporting.aplicacion.DescargarReporteServicio;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import ec.mdd.reporting.puertos.entrada.DescargarReporte;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.GeneradorDePdf;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroDeDescargas;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

/** Ensambla Reporting en el proceso salida (F20-H01, D-115, ADR-006). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionReporting {

    /** La publicación vigente de cada periodo, de Aprobación por su ruta interna (ADR-023). */
    @Bean
    public PublicacionesDelDistributivo publicacionesDelDistributivo(
            ConsultasEntreProcesos consultas,
            @Value("${mdd.procesos.distributivo}") URI distributivo) {
        return new PublicacionesDesdeAprobacion(consultas, distributivo);
    }

    /** El contenido congelado de cada versión, de Generación por su ruta interna (ADR-023). */
    @Bean
    public ContenidoDeLaVersion contenidoDeLaVersion(
            ConsultasEntreProcesos consultas,
            @Value("${mdd.procesos.distributivo}") URI distributivo) {
        return new ContenidoDesdeGeneracion(consultas, distributivo);
    }

    /** Los reportes de la versión publicada vigente (RF-45). */
    @Bean
    public ConsultarReportes consultarReportes(
            PublicacionesDelDistributivo publicaciones, ContenidoDeLaVersion contenido) {
        return new ConsultarReportesServicio(publicaciones, contenido, Clock.systemUTC());
    }

    /** El PDF/A-2b del reporte (D-101, D-115). */
    @Bean
    public GeneradorDePdf generadorDeReportePdfa() {
        return new GeneradorDeReportePdfa();
    }

    /** La bitácora de las descargas, por el registro de publicaciones de salida (ADR-018). */
    @Bean
    public RegistroDeDescargas registroDeDescargas(ApplicationEventPublisher eventos) {
        return new RegistroDeDescargasEventos(eventos, Clock.systemUTC());
    }

    /** La transacción del registro de una descarga. */
    @Bean
    public Transaccion transaccionReporting(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Las descargas del reporte, cada una registrada antes de entregarse (RF-48, D-115). */
    @Bean
    public DescargarReporte descargarReporte(
            ConsultarReportes consultar,
            GeneradorDePdf pdf,
            RegistroDeDescargas registro,
            Transaccion transaccion) {
        return new DescargarReporteServicio(
                consultar, pdf, registro, transaccion, Clock.systemUTC());
    }
}
