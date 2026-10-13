package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.SesionActual;
import ec.mdd.generacion.adaptadores.entrada.api.SesionApi;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión actual de generacion ({@code contratos/generacion/openapi.yaml}): el usuario y los roles
 * del token que la seguridad del proceso ya validó (D-39). Sirve para probar la autenticación de
 * punta a punta; las historias de generacion pueden reutilizarla o retirarla.
 */
@RestController
class SesionControlador implements SesionApi {

    @Override
    public ResponseEntity<SesionActual> consultarSesion() {
        JwtAuthenticationToken autenticacion =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = autenticacion.getToken().getClaimAsStringList("roles");
        return ResponseEntity.ok(
                new SesionActual(
                        autenticacion.getToken().getSubject(),
                        roles == null ? List.of() : roles.stream().sorted().toList()));
    }
}
