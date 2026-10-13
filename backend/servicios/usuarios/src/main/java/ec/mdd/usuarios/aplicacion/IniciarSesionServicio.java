package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.util.Optional;

/**
 * Inicio de sesión mínimo de Usuarios (D-37).
 *
 * <p>Un usuario inexistente, inactivo, una contraseña incorrecta o datos vacíos terminan igual: en
 * {@link CredencialesInvalidas}. Cuando el usuario no existe, igual se verifica la contraseña
 * contra un hash de relleno, para que el tiempo de respuesta no revele qué usuarios existen.
 */
public final class IniciarSesionServicio implements IniciarSesion {

    private final RepositorioUsuarios repositorio;
    private final VerificadorDeClaves verificador;
    private final EmisorDeTokens emisor;
    private final String hashDeRelleno;

    /** Crea el servicio y calcula una vez el hash de relleno. */
    public IniciarSesionServicio(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            EmisorDeTokens emisor) {
        this.repositorio = repositorio;
        this.verificador = verificador;
        this.emisor = emisor;
        this.hashDeRelleno = verificador.calcularHash("hash-de-relleno-sin-usuario");
    }

    @Override
    public TokenEmitido iniciar(String usuario, String clave) {
        String claveRecibida = clave == null ? "" : clave;
        Optional<Usuario> encontrado =
                usuario == null || usuario.isBlank()
                        ? Optional.empty()
                        : repositorio.buscarPorNombre(usuario);
        if (encontrado.isEmpty()) {
            verificador.coincide(claveRecibida, hashDeRelleno);
            throw new CredencialesInvalidas();
        }
        Usuario existente = encontrado.get();
        // Siempre se verifica, también con la contraseña vacía, para no cambiar el tiempo de
        // respuesta.
        boolean coincide =
                verificador.coincide(claveRecibida, existente.claveHash())
                        && !claveRecibida.isEmpty();
        if (!coincide || !existente.puedeIniciarSesion()) {
            throw new CredencialesInvalidas();
        }
        return emisor.emitir(existente);
    }
}
