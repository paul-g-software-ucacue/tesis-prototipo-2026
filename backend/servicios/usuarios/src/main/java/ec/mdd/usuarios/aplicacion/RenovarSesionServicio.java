package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.RenovarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Renovación del token mientras el usuario trabaja (ADR-020).
 *
 * <p>Vuelve a leer el usuario, así que un usuario desactivado no renueva y los roles del token
 * nuevo son los vigentes.
 */
public final class RenovarSesionServicio implements RenovarSesion {

    private static final String ROL_PROCESO = "PROCESO";

    private final RepositorioUsuarios repositorio;
    private final EmisorDeTokens emisor;
    private final Duration maximo;
    private final Clock reloj;

    /**
     * @param repositorio los usuarios
     * @param emisor firma el token nuevo
     * @param maximo cuánto puede durar una sesión desde su inicio; 8 horas en el MDD
     * @param reloj el instante actual
     */
    public RenovarSesionServicio(
            RepositorioUsuarios repositorio, EmisorDeTokens emisor, Duration maximo, Clock reloj) {
        this.repositorio = repositorio;
        this.emisor = emisor;
        this.maximo = maximo;
        this.reloj = reloj;
    }

    @Override
    public TokenEmitido renovar(String nombreUsuario, List<String> roles, Instant inicio) {
        if (inicio == null || roles.contains(ROL_PROCESO)) {
            throw new SesionNoRenovable();
        }
        if (!reloj.instant().isBefore(inicio.plus(maximo))) {
            throw new SesionNoRenovable();
        }
        Usuario usuario =
                repositorio
                        .buscarPorNombre(nombreUsuario)
                        .filter(Usuario::puedeIniciarSesion)
                        .orElseThrow(SesionNoRenovable::new);
        return emisor.emitir(usuario, inicio);
    }
}
