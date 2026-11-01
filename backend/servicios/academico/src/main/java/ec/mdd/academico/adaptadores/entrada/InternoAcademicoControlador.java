package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.adaptadores.entrada.api.AsignaturaOfertada;
import ec.mdd.academico.adaptadores.entrada.api.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.adaptadores.entrada.api.InternoApi;
import ec.mdd.academico.adaptadores.entrada.api.ParaleloDeLaOferta;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.ConsultarParaleloDeLaOferta;
import ec.mdd.academico.puertos.entrada.ParaleloConCampos;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import java.util.List;
import java.util.Optional;
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
    private final ConsultarParaleloDeLaOferta paralelos;

    InternoAcademicoControlador(
            ConsultarAsignaturasOfertadas consultar, ConsultarParaleloDeLaOferta paralelos) {
        this.consultar = consultar;
        this.paralelos = paralelos;
    }

    @Override
    public ResponseEntity<List<AsignaturaOfertada>> consultarAsignaturasOfertadas(
            String codigo, String carrera) {
        try {
            return ResponseEntity.ok(
                    consultar.delPeriodo(codigo, Optional.ofNullable(carrera)).stream()
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

    @Override
    public ResponseEntity<ParaleloDeLaOferta> consultarParaleloDeLaOferta(
            String codigo, String asignatura, String paralelo) {
        try {
            return paralelos
                    .paralelo(codigo, asignatura, paralelo)
                    .map(InternoAcademicoControlador::respuesta)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } catch (PeriodoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<ParaleloDeLaOferta>> consultarOfertaDelPeriodo(String codigo) {
        try {
            return ResponseEntity.ok(
                    paralelos.delPeriodo(codigo).stream()
                            .map(InternoAcademicoControlador::respuesta)
                            .toList());
        } catch (PeriodoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static ParaleloDeLaOferta respuesta(ParaleloConCampos paralelo) {
        return new ParaleloDeLaOferta(
                paralelo.asignatura(),
                paralelo.nombre(),
                paralelo.carrera(),
                paralelo.paralelo(),
                paralelo.campoAmplio(),
                paralelo.campoEspecifico(),
                paralelo.troncoComun(),
                paralelo.horasSemanales(),
                EstadoDeAsignaturaParalelo.fromValue(paralelo.estado().name()),
                paralelo.periodoCerrado());
    }
}
