package ec.mdd.firma.adaptadores.entrada;

import ec.mdd.firma.adaptadores.entrada.api.FirmasDeLaVersion;
import ec.mdd.firma.adaptadores.entrada.api.InternoApi;
import ec.mdd.firma.adaptadores.entrada.api.PasoDeLaVersion;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.PasoConEstado;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasDeLaVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Firma entre procesos (F19-H02, D-120, ADR-023). Solo con el token de proceso,
 * por la ruta {@code /interno/**}; el API Gateway no las enruta.
 *
 * <p>Informa la cadena sin rótulos, nombres ni firmantes (principio IX). El paso en turno sale como
 * PENDIENTE: a quien valida solo le importa si está firmado. La anulación y el rechazo van aparte,
 * porque {@link FirmasDelDocumento#cadenaConEstado()} no los oculta en los pasos.
 */
@RestController
class InternoFirmaControlador implements InternoApi {

    private final ConsultarFirmasDeLaVersion firmas;

    InternoFirmaControlador(ConsultarFirmasDeLaVersion firmas) {
        this.firmas = firmas;
    }

    @Override
    public ResponseEntity<FirmasDeLaVersion> consultarFirmasDeLaVersion(
            String periodo, Integer numero) {
        return firmas.firmas(periodo, numero)
                .map(InternoFirmaControlador::respuesta)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static FirmasDeLaVersion respuesta(FirmasDelDocumento documento) {
        return new FirmasDeLaVersion(
                documento.documento().periodo(),
                documento.documento().version(),
                documento.documento().hashContenido(),
                documento.cadenaConEstado().stream()
                        .map(paso -> new PasoDeLaVersion(paso.orden(), estado(paso.estado())))
                        .toList(),
                documento.anulacion().isPresent(),
                documento.rechazo().isPresent());
    }

    private static PasoDeLaVersion.EstadoEnum estado(PasoConEstado.Estado estado) {
        return switch (estado) {
            case FIRMADO -> PasoDeLaVersion.EstadoEnum.FIRMADO;
            case EN_TURNO, PENDIENTE -> PasoDeLaVersion.EstadoEnum.PENDIENTE;
            case RECHAZADO -> PasoDeLaVersion.EstadoEnum.RECHAZADO;
        };
    }
}
