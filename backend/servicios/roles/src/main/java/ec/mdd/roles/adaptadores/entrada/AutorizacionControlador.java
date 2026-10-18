package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.roles.adaptadores.entrada.api.Autorizacion;
import ec.mdd.roles.adaptadores.entrada.api.AutorizacionApi;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Roles y permisos efectivos de un usuario para otro proceso ({@code contratos/roles/openapi.yaml},
 * ADR-018, ADR-021). Solo lo alcanza un token de proceso ({@code /interno/**}); dentro de {@code
 * identidad}, Usuarios usa directamente {@link ConsultarAutorizacion}.
 */
@RestController
class AutorizacionControlador implements AutorizacionApi {

    private final ConsultarAutorizacion consultar;

    AutorizacionControlador(ConsultarAutorizacion consultar) {
        this.consultar = consultar;
    }

    @Override
    public ResponseEntity<Autorizacion> consultarAutorizacion(String usuario) {
        ec.mdd.roles.puertos.entrada.Autorizacion autorizacion = consultar.consultar(usuario);
        return ResponseEntity.ok(
                new Autorizacion(
                        autorizacion.usuario(), autorizacion.roles(), autorizacion.permisos()));
    }
}
