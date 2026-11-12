package ec.mdd.firma.infraestructura;

import ec.mdd.firma.adaptadores.salida.AlmacenDeDocumentosEnDisco;
import ec.mdd.firma.adaptadores.salida.ContenidoDesdeGeneracion;
import ec.mdd.firma.adaptadores.salida.FirmantesDesdeProcesos;
import ec.mdd.firma.adaptadores.salida.GeneradorPdfa;
import ec.mdd.firma.adaptadores.salida.PublicadorDeCambios;
import ec.mdd.firma.adaptadores.salida.RepositorioDocumentosJdbc;
import ec.mdd.firma.adaptadores.salida.TransaccionSpring;
import ec.mdd.firma.aplicacion.DocumentoFirmableServicio;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.GeneradorDePdf;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.generacion.puertos.entrada.ContenidoCongelado;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/** Ensambla el documento firmable (F13-H02, D-101). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionFirma {

    /** Las generaciones y los documentos, en el esquema firma. */
    @Bean
    public RepositorioDocumentos repositorioDocumentos(JdbcClient jdbc) {
        return new RepositorioDocumentosJdbc(jdbc);
    }

    /** Los PDF, en la carpeta documentos/ del volumen de D-88. */
    @Bean
    public AlmacenDeDocumentos almacenDeDocumentos(@Value("${mdd.archivos.raiz}") Path raiz) {
        return new AlmacenDeDocumentosEnDisco(raiz);
    }

    /** El contenido de la versión congelada, por la interfaz publicada de Generación. */
    @Bean
    public ContenidoDelDistributivo contenidoDelDistributivo(ContenidoCongelado generacion) {
        return new ContenidoDesdeGeneracion(generacion);
    }

    /** Los firmantes: Docentes en el proceso datos y Roles en el proceso identidad (ADR-023). */
    @Bean
    public FirmantesDelPeriodo firmantesDelPeriodo(
            ConsultasEntreProcesos consultas,
            @Value("${mdd.procesos.datos}") URI datos,
            @Value("${mdd.procesos.identidad}") URI identidad) {
        return new FirmantesDesdeProcesos(consultas, datos, identidad);
    }

    /** El PDF/A-2b con OpenHTMLtoPDF y PDFBox. */
    @Bean
    public GeneradorDePdf generadorDePdf() {
        return new GeneradorPdfa();
    }

    /** Cada documento generado, como una entrada de la bitácora de Usuarios (F17-H01). */
    @Bean
    public RegistroDeCambios registroDeCambiosFirma(ApplicationEventPublisher eventos) {
        return new PublicadorDeCambios(eventos, Clock.systemUTC());
    }

    /** Las transacciones de Firma. */
    @Bean
    public Transaccion transaccionFirma(TransactionOperations transactionTemplate) {
        return new TransaccionSpring(transactionTemplate);
    }

    /**
     * El documento firmable: lo prepara Aprobación al entrar en firma (MD-05), y desde F13-H02 lo
     * consulta y lo reintenta la pantalla.
     */
    @Bean
    public DocumentoFirmableServicio documentoFirmable(
            RepositorioDocumentos repositorioDocumentos,
            ContenidoDelDistributivo contenidoDelDistributivo,
            FirmantesDelPeriodo firmantesDelPeriodo,
            GeneradorDePdf generadorDePdf,
            AlmacenDeDocumentos almacenDeDocumentos,
            RegistroDeCambios registroDeCambiosFirma,
            Transaccion transaccionFirma) {
        return new DocumentoFirmableServicio(
                repositorioDocumentos,
                contenidoDelDistributivo,
                firmantesDelPeriodo,
                generadorDePdf,
                almacenDeDocumentos,
                registroDeCambiosFirma,
                transaccionFirma,
                Clock.systemUTC());
    }
}
