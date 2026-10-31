package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.adaptadores.entrada.api.InternoApi;
import ec.mdd.normativa.adaptadores.entrada.api.PesosDeAfinidad;
import ec.mdd.normativa.adaptadores.entrada.api.PesosDelPeriodo;
import ec.mdd.normativa.dominio.PesosAfinidad;
import ec.mdd.normativa.puertos.entrada.ConsultarPesosDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.PesosDeLaVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Normativa entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta.
 */
@RestController
class InternoNormativaControlador implements InternoApi {

    private final ConsultarPesosDelPeriodo consultar;

    InternoNormativaControlador(ConsultarPesosDelPeriodo consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<PesosDelPeriodo> consultarPesosDelPeriodo(String periodo) {
        try {
            PesosDeLaVersion pesos = consultar.pesos(periodo);
            return ResponseEntity.ok(
                    new PesosDelPeriodo(periodo, pesos.version(), respuesta(pesos.pesos())));
        } catch (PeriodoSinVersion error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static PesosDeAfinidad respuesta(PesosAfinidad pesos) {
        return new PesosDeAfinidad(
                pesos.cuartoNivelCampoEspecifico(),
                pesos.cuartoNivelCampoAmplio(),
                pesos.tercerNivelCampoEspecifico(),
                pesos.tercerNivelCampoAmplio(),
                pesos.experienciaRelacionada(),
                pesos.cursoRelacionado(),
                pesos.topeCursos());
    }
}
