package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.CredencialesSesion;
import ec.mdd.usuarios.adaptadores.entrada.api.SesionActual;
import ec.mdd.usuarios.adaptadores.entrada.api.SesionApi;
import ec.mdd.usuarios.adaptadores.entrada.api.TokenSesion;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.RenovarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesión, sesión actual y renovación de Usuarios ({@code
 * contratos/usuarios/openapi.yaml}).
 *
 * <p>El inicio de sesión no pide token (D-37). La sesión actual devuelve el usuario, su nombre para
 * mostrar, los roles y los permisos del token, que la seguridad del proceso ya validó (D-39,
 * ADR-021). La renovación emite un token nuevo para la misma sesión (ADR-020).
 *
 * <p>Los errores los traduce el mecanismo común, con los códigos registrados en {@link
 * ErroresUsuarios} (D-38, ADR-016).
 */
@RestController
class SesionControlador implements SesionApi {

    private final IniciarSesion iniciarSesion;
    private final RenovarSesion renovarSesion;

    SesionControlador(IniciarSesion iniciarSesion, RenovarSesion renovarSesion) {
        this.iniciarSesion = iniciarSesion;
        this.renovarSesion = renovarSesion;
    }

    @Override
    public ResponseEntity<TokenSesion> iniciarSesion(CredencialesSesion credenciales) {
        return ResponseEntity.ok(
                aRespuesta(
                        iniciarSesion.iniciar(credenciales.getUsuario(), credenciales.getClave())));
    }

    @Override
    public ResponseEntity<SesionActual> consultarSesion() {
        Jwt token = tokenActual();
        SesionActual sesion = new SesionActual(token.getSubject(), roles(token));
        sesion.setNombre(token.getClaimAsString("nombre"));
        List<String> permisos = token.getClaimAsStringList("permisos");
        if (permisos != null) {
            sesion.setPermisos(permisos.stream().sorted().toList());
        }
        return ResponseEntity.ok(sesion);
    }

    @Override
    public ResponseEntity<TokenSesion> renovarSesion() {
        Jwt token = tokenActual();
        Object inicio = token.getClaim("inicio");
        return ResponseEntity.ok(
                aRespuesta(
                        renovarSesion.renovar(
                                token.getSubject(),
                                roles(token),
                                inicio instanceof Number segundos
                                        ? Instant.ofEpochSecond(segundos.longValue())
                                        : null)));
    }

    private static Jwt tokenActual() {
        return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
                .getToken();
    }

    private static List<String> roles(Jwt token) {
        List<String> roles = token.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles.stream().sorted().toList();
    }

    private static TokenSesion aRespuesta(TokenEmitido token) {
        return new TokenSesion(
                token.valor(),
                TokenSesion.TipoEnum.BEARER,
                OffsetDateTime.ofInstant(token.venceEn(), ZoneOffset.UTC));
    }
}
