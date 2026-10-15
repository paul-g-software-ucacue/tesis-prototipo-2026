package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.CredencialesProceso;
import ec.mdd.usuarios.adaptadores.entrada.api.TokenProcesoApi;
import ec.mdd.usuarios.adaptadores.entrada.api.TokenSesion;
import ec.mdd.usuarios.puertos.entrada.EmitirTokenDeProceso;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token de proceso de Usuarios ({@code contratos/usuarios/openapi.yaml}, D-49, ADR-018).
 *
 * <p>Es una ruta entre procesos: no pide token, el proceso la lista en sus rutas públicas, y el API
 * Gateway no la enruta. Una clave incorrecta la traduce el mecanismo común de errores con {@code
 * USUARIOS_CREDENCIALES_INVALIDAS}.
 */
@RestController
class TokenProcesoControlador implements TokenProcesoApi {

    private final EmitirTokenDeProceso emitirTokenDeProceso;

    TokenProcesoControlador(EmitirTokenDeProceso emitirTokenDeProceso) {
        this.emitirTokenDeProceso = emitirTokenDeProceso;
    }

    @Override
    public ResponseEntity<TokenSesion> emitirTokenDeProceso(CredencialesProceso credenciales) {
        TokenEmitido token =
                emitirTokenDeProceso.emitir(credenciales.getProceso(), credenciales.getClave());
        return ResponseEntity.ok(
                new TokenSesion(
                        token.valor(),
                        TokenSesion.TipoEnum.BEARER,
                        OffsetDateTime.ofInstant(token.venceEn(), ZoneOffset.UTC)));
    }
}
