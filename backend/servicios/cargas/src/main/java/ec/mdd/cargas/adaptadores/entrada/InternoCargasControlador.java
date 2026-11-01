package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.InternoApi;
import ec.mdd.cargas.adaptadores.entrada.api.PeriodoListo;
import ec.mdd.cargas.dominio.PreparacionDelPeriodo;
import ec.mdd.cargas.puertos.entrada.ConsultarPreparacion;
import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las consultas de Cargas entre procesos (ADR-023). Solo con el token de proceso, por la ruta
 * {@code /interno/**}; el API Gateway no las enruta. Devuelve lo mínimo del tablero: la misma
 * regla, sin sus listas (principio IX).
 */
@RestController
class InternoCargasControlador implements InternoApi {

    private final ConsultarPreparacion consultar;

    InternoCargasControlador(ConsultarPreparacion consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<PeriodoListo> consultarPeriodoListo(String periodo) {
        try {
            PreparacionDelPeriodo tablero = consultar.preparacion(Optional.of(periodo)).tablero();
            return ResponseEntity.ok(
                    new PeriodoListo(tablero.abierto(), tablero.cerrado(), tablero.listo()));
        } catch (PeriodoNoEncontrado error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
