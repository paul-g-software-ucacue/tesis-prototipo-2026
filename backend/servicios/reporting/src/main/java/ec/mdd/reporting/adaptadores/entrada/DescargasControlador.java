package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.reporting.adaptadores.entrada.api.DescargasApi;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.puertos.entrada.DescargarReporte;
import ec.mdd.reporting.puertos.entrada.DescargarReporte.ArchivoDelReporte;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las descargas del reporte individual en PDF/A y en CSV (F20-H01, RF-48, D-115). Cada una queda en
 * la bitácora antes de responder. {@code no-store}: el archivo lleva datos personales.
 */
@RestController
class DescargasControlador implements DescargasApi {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final DescargarReporte descargar;

    DescargasControlador(DescargarReporte descargar) {
        this.descargar = descargar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_REPORTES:LECTURA')")
    public ResponseEntity<Resource> descargarReporteIndividualPdf(
            String periodo, String identificacion) {
        return respuesta(
                descargar.descargar(
                        periodo, identificacion, Formato.PDF, ReportesControlador.quien()));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_REPORTES:LECTURA')")
    public ResponseEntity<Resource> descargarReporteIndividualCsv(
            String periodo, String identificacion) {
        return respuesta(
                descargar.descargar(
                        periodo, identificacion, Formato.CSV, ReportesControlador.quien()));
    }

    private static ResponseEntity<Resource> respuesta(ArchivoDelReporte archivo) {
        return ResponseEntity.ok()
                .contentType(archivo.formato() == Formato.PDF ? MediaType.APPLICATION_PDF : CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(archivo.nombre())
                                .build()
                                .toString())
                .cacheControl(CacheControl.noStore())
                .body(new ByteArrayResource(archivo.contenido()));
    }
}
