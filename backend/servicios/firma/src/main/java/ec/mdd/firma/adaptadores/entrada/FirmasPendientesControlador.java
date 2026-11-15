package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.adaptadores.entrada.api.CertificadoDeLaFirma;
import ec.mdd.firma.adaptadores.entrada.api.FirmaPendiente;
import ec.mdd.firma.adaptadores.entrada.api.FirmasPendientesApi;
import ec.mdd.firma.adaptadores.entrada.api.RechazoEnFirma;
import ec.mdd.firma.adaptadores.entrada.api.ResultadoDeComprobacion;
import ec.mdd.firma.adaptadores.entrada.api.ResultadoValidacion;
import ec.mdd.firma.adaptadores.entrada.api.SolicitudDeRechazo;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes;
import ec.mdd.firma.puertos.entrada.DescargarArchivoVigente;
import ec.mdd.firma.puertos.entrada.RechazarEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoRegistrado;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las firmas pendientes, la descarga del archivo vigente (F14-H01), la subida del PDF firmado
 * (F14-H02) y el rechazo en firma (F14-H04), según {@code contratos/firma/openapi.yaml}. El permiso
 * lo exige su {@code x-mdd-permiso} (ADR-021); el turno, el caso de uso. Fuera de turno, el mismo
 * 403 común que sin permiso: no dice si el documento existe ni de quién es el turno (FR-010 de
 * F14-H01, FR-002 de F14-H02). Un archivo firmado rechazado es el 422 de su comprobación, con las
 * cuatro en los detalles ({@code ErroresFirma}).
 */
@RestController
class FirmasPendientesControlador implements FirmasPendientesApi {

    private final ConsultarFirmasPendientes consultar;
    private final DescargarArchivoVigente descargar;
    private final SubirFirma subir;
    private final RechazarEnFirma rechazar;

    FirmasPendientesControlador(
            ConsultarFirmasPendientes consultar,
            DescargarArchivoVigente descargar,
            SubirFirma subir,
            RechazarEnFirma rechazar) {
        this.consultar = consultar;
        this.descargar = descargar;
        this.subir = subir;
        this.rechazar = rechazar;
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
        ec.mdd.firma.dominio.ResultadoValidacion resultado;
        try {
            resultado = subir.subir(documento, archivo.getBytes(), SolicitanteDelToken.actual());
        } catch (SinTurnoDeFirma sinTurno) {
            throw new AccessDeniedException("No es el firmante en turno", sinTurno);
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", error);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(resultado));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_DISTRIBUTIVO:FIRMA')")
    public ResponseEntity<RechazoEnFirma> rechazarEnFirma(
            UUID documento, SolicitudDeRechazo solicitud) {
        RechazoRegistrado rechazo;
        try {
            rechazo =
                    rechazar.rechazar(
                            documento, solicitud.getObservacion(), SolicitanteDelToken.actual());
        } catch (SinTurnoDeFirma sinTurno) {
            throw new AccessDeniedException("No es el firmante en turno", sinTurno);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new RechazoEnFirma(
                                rechazo.documento(),
                                rechazo.periodo(),
                                rechazo.version(),
                                rechazo.orden(),
                                rechazo.rotulo(),
                                rechazo.rechazadoPor(),
                                rechazo.rechazadoEn().atOffset(ZoneOffset.UTC),
                                rechazo.observacion()));
    }

    private static ResultadoValidacion respuesta(ec.mdd.firma.dominio.ResultadoValidacion uno) {
        ResultadoValidacion respuesta =
                new ResultadoValidacion(
                        uno.id(),
                        uno.documento(),
                        uno.orden(),
                        uno.aceptada(),
                        uno.sha256Subido(),
                        uno.subidoEn().atOffset(ZoneOffset.UTC),
                        uno.comprobaciones().stream()
                                .map(
                                        una ->
                                                new ResultadoDeComprobacion(
                                                                ResultadoDeComprobacion
                                                                        .ComprobacionEnum.valueOf(
                                                                        una.comprobacion().name()),
                                                                ResultadoDeComprobacion
                                                                        .ResultadoEnum.valueOf(
                                                                        una.resultado().name()))
                                                        .motivo(una.motivo().orElse(null)))
                                .toList());
        uno.certificado()
                .ifPresent(
                        certificado ->
                                respuesta.certificado(
                                        new CertificadoDeLaFirma(
                                                certificado.nombre(),
                                                certificado.emisor(),
                                                certificado.serie(),
                                                certificado.validoDesde().atOffset(ZoneOffset.UTC),
                                                certificado
                                                        .validoHasta()
                                                        .atOffset(ZoneOffset.UTC))));
        return respuesta;
    }
}
