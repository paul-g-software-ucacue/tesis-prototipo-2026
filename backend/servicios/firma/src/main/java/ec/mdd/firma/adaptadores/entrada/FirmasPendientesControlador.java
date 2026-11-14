package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.adaptadores.entrada.api.FirmaPendiente;
import ec.mdd.firma.adaptadores.entrada.api.FirmasPendientesApi;
import ec.mdd.firma.adaptadores.entrada.api.ResultadoValidacion;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes;
import ec.mdd.firma.puertos.entrada.DescargarArchivoVigente;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las firmas pendientes y la descarga del archivo vigente ({@code contratos/firma/openapi.yaml},
 * F14-H01). El permiso lo exige su {@code x-mdd-permiso} (ADR-021); el turno, el caso de uso. Fuera
 * de turno, el mismo 403 común que sin permiso: no dice si el documento existe ni de quién es el
 * turno (FR-010).
 */
@RestController
class FirmasPendientesControlador implements FirmasPendientesApi {

    private final ConsultarFirmasPendientes consultar;
    private final DescargarArchivoVigente descargar;

    FirmasPendientesControlador(
            ConsultarFirmasPendientes consultar, DescargarArchivoVigente descargar) {
        this.consultar = consultar;
        this.descargar = descargar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_DISTRIBUTIVO:FIRMA')")
    public ResponseEntity<List<FirmaPendiente>> listarFirmasPendientes() {
        return ResponseEntity.ok(
                consultar.pendientes(SolicitanteDelToken.actual()).stream()
                        .map(
                                una ->
                                        new FirmaPendiente(
                                                una.documento(),
                                                una.periodo(),
                                                una.version(),
                                                una.orden(),
                                                una.rotulo(),
                                                una.generadoEn().atOffset(ZoneOffset.UTC)))
                        .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_DISTRIBUTIVO:FIRMA')")
    public ResponseEntity<Resource> descargarArchivoVigente(UUID documento) {
        ArchivoVigente archivo;
        try {
            archivo = descargar.archivo(documento, SolicitanteDelToken.actual());
        } catch (SinTurnoDeFirma sinTurno) {
            throw new AccessDeniedException("No es el firmante en turno", sinTurno);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .eTag("\"" + archivo.sha256() + "\"")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(archivo.nombre())
                                .build()
                                .toString())
                .body(new ByteArrayResource(archivo.contenido()));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_DISTRIBUTIVO:FIRMA')")
    public ResponseEntity<ResultadoValidacion> subirFirma(UUID documento, MultipartFile archivo) {
        throw new UnsupportedOperationException("F14-H02, T011");
    }
}
