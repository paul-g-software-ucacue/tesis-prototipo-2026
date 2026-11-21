package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.validacionocs.adaptadores.entrada.api.InternoApi;
import ec.mdd.validacionocs.adaptadores.entrada.api.PublicacionVigente;
import ec.mdd.validacionocs.puertos.entrada.ConsultarPublicaciones;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las consultas de Aprobación entre procesos (F20-H01, D-115, ADR-023). Solo con el token de
 * proceso, por la ruta {@code /interno/**}; el API Gateway no las enruta.
 */
@RestController
class InternoValidacionocsControlador implements InternoApi {

    private final ConsultarPublicaciones publicaciones;

    InternoValidacionocsControlador(ConsultarPublicaciones publicaciones) {
        this.publicaciones = publicaciones;
    }

    @Override
    public ResponseEntity<List<PublicacionVigente>> consultarPublicacionesVigentes() {
        return ResponseEntity.ok(
                publicaciones.vigentes().stream()
                        .map(
                                una ->
                                        new PublicacionVigente(
                                                una.periodo(),
                                                una.version(),
                                                una.hash(),
                                                una.publicadaPor(),
                                                una.publicadaEn().atOffset(ZoneOffset.UTC),
                                                una.enReforma()))
                        .toList());
    }
}
