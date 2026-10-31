package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.AsignaturaOfertada;
import ec.mdd.academico.adaptadores.entrada.api.InternoApi;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Académico entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta.
 */
@RestController
class InternoAcademicoControlador implements InternoApi {

    private final ConsultarAsignaturasOfertadas consultar;

    InternoAcademicoControlador(ConsultarAsignaturasOfertadas consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<List<AsignaturaOfertada>> consultarAsignaturasOfertadas(String codigo) {
        try {
            return ResponseEntity.ok(
                    consultar.delPeriodo(codigo).stream()
                            .map(
                                    asignatura ->
                                            new AsignaturaOfertada(
                                                    asignatura.codigo(),
                                                    asignatura.nombre(),
                                                    asignatura.campoAmplio(),
                                                    asignatura.campoEspecifico()))
                            .toList());
        } catch (PeriodoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
