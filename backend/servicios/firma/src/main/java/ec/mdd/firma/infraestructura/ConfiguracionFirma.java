package ec.mdd.firma.infraestructura;

import ec.mdd.firma.adaptadores.salida.AlmacenDeDocumentosEnDisco;
import ec.mdd.firma.adaptadores.salida.ContenidoDesdeGeneracion;
import ec.mdd.firma.adaptadores.salida.FirmantesDesdeProcesos;
import ec.mdd.firma.adaptadores.salida.GeneradorPdfa;
import ec.mdd.firma.adaptadores.salida.IdentificacionDesdeUsuarios;
import ec.mdd.firma.adaptadores.salida.ListaConfianza;
import ec.mdd.firma.adaptadores.salida.PublicadorDeCambios;
import ec.mdd.firma.adaptadores.salida.RepositorioDocumentosJdbc;
import ec.mdd.firma.adaptadores.salida.TransaccionSpring;
import ec.mdd.firma.adaptadores.salida.ValidadorPades;
import ec.mdd.firma.aplicacion.DocumentoFirmableServicio;
import ec.mdd.firma.aplicacion.FirmasPendientesServicio;
import ec.mdd.firma.aplicacion.SubirFirmaServicio;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.GeneradorDePdf;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import ec.mdd.generacion.puertos.entrada.ContenidoCongelado;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ensambla el documento firmable (F13-H02, D-101), las firmas pendientes (F14-H01) y la subida
 * (F14-H02).
 */
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

    /** La identificación de una cuenta, de Usuarios en el proceso identidad (F14-H01, ADR-023). */
    @Bean
    public IdentificacionDeUsuarios identificacionDeUsuarios(
            ConsultasEntreProcesos consultas, @Value("${mdd.procesos.identidad}") URI identidad) {
        return new IdentificacionDesdeUsuarios(consultas, identidad);
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

    /**
     * Las firmas pendientes de cada usuario y la descarga del archivo vigente, solo para el
     * firmante en turno (F14-H01, D-102).
     */
    @Bean
    public FirmasPendientesServicio firmasPendientes(
            RepositorioDocumentos repositorioDocumentos,
            AlmacenDeDocumentos almacenDeDocumentos,
            IdentificacionDeUsuarios identificacionDeUsuarios) {
        return new FirmasPendientesServicio(
                repositorioDocumentos, almacenDeDocumentos, identificacionDeUsuarios);
    }

    /**
     * La lista de confianza del entorno (F14-H02, D-103), leída al arrancar de {@code
     * mdd.firma.raices}. Sin raíces, o con un archivo ilegible, el proceso no arranca. Por defecto,
     * la de demostración: la CA de prueba y la raíz pública del BCE.
     */
    @Bean
    public ListaConfianza listaConfianza(@Value("${mdd.firma.raices}") List<Resource> raices) {
        return ListaConfianza.desde(raices);
    }

    /** La validación de la firma nueva, con PDFBox y Bouncy Castle. */
    @Bean
    public ValidadorDeFirmas validadorDeFirmas(ListaConfianza listaConfianza) {
        return new ValidadorPades(listaConfianza);
    }

    /** La subida del PDF firmado y su validación básica (F14-H02). */
    @Bean
    public SubirFirmaServicio subirFirma(
            RepositorioDocumentos repositorioDocumentos,
            AlmacenDeDocumentos almacenDeDocumentos,
            IdentificacionDeUsuarios identificacionDeUsuarios,
            ValidadorDeFirmas validadorDeFirmas,
            RegistroDeCambios registroDeCambiosFirma,
            Transaccion transaccionFirma) {
        return new SubirFirmaServicio(
                repositorioDocumentos,
                almacenDeDocumentos,
                identificacionDeUsuarios,
                validadorDeFirmas,
                registroDeCambiosFirma,
                transaccionFirma,
                Clock.systemUTC());
    }
}
