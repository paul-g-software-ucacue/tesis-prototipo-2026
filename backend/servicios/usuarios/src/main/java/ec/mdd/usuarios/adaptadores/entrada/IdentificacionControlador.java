package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.Identificacion;
import ec.mdd.usuarios.adaptadores.entrada.api.IdentificacionApi;
import ec.mdd.usuarios.puertos.entrada.ConsultarIdentificacion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La identificación de una cuenta, solo entre procesos ({@code contratos/usuarios/openapi.yaml},
 * F14-H01). {@code /interno/} exige el token de proceso (ADR-023); el API Gateway no lo enruta.
 */
@RestController
class IdentificacionControlador implements IdentificacionApi {

    private final ConsultarIdentificacion consultar;

    IdentificacionControlador(ConsultarIdentificacion consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<Identificacion> consultarIdentificacion(String usuario) {
        return ResponseEntity.ok(new Identificacion(consultar.de(usuario)));
    }
}
