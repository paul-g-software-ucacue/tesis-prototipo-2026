package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.CredencialesSesion;
import ec.mdd.usuarios.adaptadores.entrada.api.SesionActual;
import ec.mdd.usuarios.adaptadores.entrada.api.SesionApi;
import ec.mdd.usuarios.adaptadores.entrada.api.TokenSesion;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesión y sesión actual de Usuarios ({@code contratos/usuarios/openapi.yaml}).
 *
 * <p>El inicio de sesión no pide token (D-37). La sesión actual devuelve el usuario y los roles del
 * token, que la seguridad del proceso ya validó (D-39).
 *
 * <p>Las credenciales inválidas las traduce el mecanismo común de errores, con {@code
 * USUARIOS_CREDENCIALES_INVALIDAS} registrado en {@link ErroresUsuarios} (D-38, ADR-016).
 */
@RestController
class SesionControlador implements SesionApi {

    private final IniciarSesion iniciarSesion;

    SesionControlador(IniciarSesion iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @Override
    public ResponseEntity<TokenSesion> iniciarSesion(CredencialesSesion credenciales) {
        TokenEmitido token =
                iniciarSesion.iniciar(credenciales.getUsuario(), credenciales.getClave());
        return ResponseEntity.ok(
                new TokenSesion(
                        token.valor(),
                        TokenSesion.TipoEnum.BEARER,
                        OffsetDateTime.ofInstant(token.venceEn(), ZoneOffset.UTC)));
    }

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
