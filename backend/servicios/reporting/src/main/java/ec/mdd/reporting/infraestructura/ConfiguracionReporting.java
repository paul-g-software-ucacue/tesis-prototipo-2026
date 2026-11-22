package ec.mdd.reporting.infraestructura;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.reporting.adaptadores.salida.ContenidoDesdeGeneracion;
import ec.mdd.reporting.adaptadores.salida.ExportadorSigaCsv;
import ec.mdd.reporting.adaptadores.salida.GeneradorDeReportePdfa;
import ec.mdd.reporting.adaptadores.salida.LayoutsGuardadosJdbc;
import ec.mdd.reporting.adaptadores.salida.LectorDeLayoutYaml;
import ec.mdd.reporting.adaptadores.salida.PublicacionesDesdeAprobacion;
import ec.mdd.reporting.adaptadores.salida.RegistroEnBitacoraEventos;
import ec.mdd.reporting.adaptadores.salida.TransaccionSpring;
import ec.mdd.reporting.aplicacion.ConsultarReportesServicio;
import ec.mdd.reporting.aplicacion.DescargarReporteServicio;
import ec.mdd.reporting.aplicacion.GenerarCsvSigaServicio;
import ec.mdd.reporting.aplicacion.GestionarLayoutSigaServicio;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import ec.mdd.reporting.puertos.entrada.DescargarReporte;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
import ec.mdd.reporting.puertos.salida.GeneradorDePdf;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
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
    public RegistroEnBitacora registroEnBitacora(ApplicationEventPublisher eventos) {
        return new RegistroEnBitacoraEventos(eventos, Clock.systemUTC());
    }

    /** Las versiones del layout de exportación y la plantilla (F19-H01, D-119). */
    @Bean
    public LayoutsGuardados layoutsGuardados(JdbcClient jdbc) {
        return new LayoutsGuardadosJdbc(jdbc);
    }

    /** El lector del archivo de mapeo, sin anclas, alias ni etiquetas (D-119). */
    @Bean
    public LectorDeLayout lectorDeLayout() {
        return new LectorDeLayoutYaml();
    }

    /** El puerto «Exportación a SIGA»: el CSV con el layout de exportación (D-119). */
    @Bean
    public ExportadorSiga exportadorSiga() {
        return new ExportadorSigaCsv();
    }

    /** El CSV para SIGA de la versión publicada vigente, registrado antes de entregarse (RF-52). */
    @Bean
    public GenerarCsvSiga generarCsvSiga(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            LayoutsGuardados guardados,
            LectorDeLayout lector,
            ExportadorSiga exportador,
            RegistroEnBitacora registro,
            Transaccion transaccion) {
        return GenerarCsvSigaServicio.de(
                publicaciones,
                contenido,
                guardados,
                lector,
                exportador,
                registro,
                transaccion,
                Clock.systemUTC());
    }

    /** El layout de exportación: consultarlo, su plantilla y cargar uno nuevo (D-119). */
    @Bean
    public GestionarLayoutSiga gestionarLayoutSiga(
            LayoutsGuardados guardados,
            LectorDeLayout lector,
            RegistroEnBitacora registro,
            Transaccion transaccion) {
        return GestionarLayoutSigaServicio.de(
                guardados, lector, registro, transaccion, Clock.systemUTC());
    }

    /** La transacción del registro de una descarga, de un CSV para SIGA o de un layout. */
    @Bean
    public Transaccion transaccionReporting(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /** Las descargas del reporte, cada una registrada antes de entregarse (RF-48, D-115). */
    @Bean
    public DescargarReporte descargarReporte(
            ConsultarReportes consultar,
            GeneradorDePdf pdf,
            RegistroEnBitacora registro,
            Transaccion transaccion) {
        return new DescargarReporteServicio(
                consultar, pdf, registro, transaccion, Clock.systemUTC());
    }
}
