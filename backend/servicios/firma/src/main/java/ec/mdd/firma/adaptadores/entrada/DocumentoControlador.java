package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.adaptadores.entrada.api.DocumentoFirmable;
import ec.mdd.firma.adaptadores.entrada.api.DocumentoFirmableApi;
import ec.mdd.firma.adaptadores.entrada.api.EstadoDelDocumento;
import ec.mdd.firma.dominio.SinRolDeFirma;
import ec.mdd.firma.puertos.entrada.ConsultarDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.GenerarDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.Solicitante;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * El documento firmable ({@code contratos/firma/openapi.yaml}, F13-H02). El permiso de cada
 * operación lo exige su {@code x-mdd-permiso} (ADR-021). El rol de la etapa lo exige el caso de uso
 * con los roles del token; si no corresponde, el mismo 403 común que sin permiso, sin decir qué
 * falta, como en el flujo de Validación OCS.
 */
@RestController
class DocumentoControlador implements DocumentoFirmableApi {

    private static final String ROL = "ROLE_";

    private final ConsultarDocumentoFirmable consultar;
    private final GenerarDocumentoFirmable generar;

    DocumentoControlador(ConsultarDocumentoFirmable consultar, GenerarDocumentoFirmable generar) {
        this.consultar = consultar;
        this.generar = generar;
    }

    @Override
    @PreAuthorize(
            "hasAnyAuthority('PERMISO_CONSOLIDACION:LECTURA', 'PERMISO_DISTRIBUTIVO:ESCRITURA')")
    public ResponseEntity<EstadoDelDocumento> consultarDocumentoFirmable(String periodo) {
        return ResponseEntity.ok(respuesta(consultar.estado(periodo, solicitante())));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CONSOLIDACION:APROBACION')")
    public ResponseEntity<EstadoDelDocumento> generarDocumentoFirmable(String periodo) {
        Solicitante quien = solicitante();
        try {
            return ResponseEntity.ok(respuesta(generar.generar(periodo, quien)));
        } catch (SinRolDeFirma sinRol) {
            throw new AccessDeniedException("No tiene el rol de la etapa", sinRol);
        }
    }

    // Quién es y sus roles, del token.
    private static Solicitante solicitante() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles =
                autenticacion.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(una -> una.startsWith(ROL))
                        .map(una -> una.substring(ROL.length()))
                        .collect(Collectors.toSet());
        return new Solicitante(autenticacion.getName(), roles);
    }

    private static EstadoDelDocumento respuesta(
            ec.mdd.firma.puertos.entrada.EstadoDelDocumento estado) {
        EstadoDelDocumento respuesta =
                new EstadoDelDocumento(
                        estado.periodo(),
                        EstadoDelDocumento.EstadoEnum.fromValue(estado.estado().name()),
                        estado.acciones().stream()
                                .map(EstadoDelDocumento.AccionesEnum::fromValue)
                                .toList());
        estado.version().ifPresent(respuesta::version);
        estado.motivo().ifPresent(respuesta::motivo);
        estado.documento()
                .ifPresent(
                        documento ->
                                respuesta.documento(
                                        new DocumentoFirmable(
                                                documento.sha256(),
                                                documento.hashContenido(),
                                                documento.generadoPor(),
                                                documento.generadoEn().atOffset(ZoneOffset.UTC))));
        return respuesta;
    }
}
