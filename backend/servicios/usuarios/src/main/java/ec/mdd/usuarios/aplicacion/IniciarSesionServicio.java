package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.EstadoDeIntentos;
import ec.mdd.usuarios.dominio.PoliticaDeIntentos;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import ec.mdd.usuarios.puertos.salida.RepositorioIntentos;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Inicio de sesión de Usuarios (D-37), con bloqueo por intentos fallidos (D-60).
 *
 * <p>Un usuario inexistente, inactivo, una contraseña incorrecta o datos vacíos terminan igual: en
 * {@link CredencialesInvalidas}. Cuando el usuario no existe, igual se verifica la contraseña
 * contra un hash de relleno, para que el tiempo de respuesta no revele qué usuarios existen.
 *
 * <p>Los fallos se cuentan por nombre intentado, exista o no. Un nombre bloqueado responde {@link
 * InicioBloqueado} sin verificar la contraseña ni contar el intento.
 *
 * <p>Los roles y permisos del token los da Roles en ese momento (ADR-021).
 */
public final class IniciarSesionServicio implements IniciarSesion {

    /** Tras cuánto tiempo sin intentos se borra la cuenta de un nombre sin bloqueo. */
    private static final Duration LIMPIEZA = Duration.ofHours(24);

    private final RepositorioUsuarios repositorio;
    private final VerificadorDeClaves verificador;
    private final EmisorDeTokens emisor;
    private final RepositorioIntentos intentos;
    private final ProveedorDeAutorizacion autorizacion;
    private final PoliticaDeIntentos politica;
    private final Clock reloj;
    private final String hashDeRelleno;

    /** Crea el servicio y calcula una vez el hash de relleno. */
    public IniciarSesionServicio(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            EmisorDeTokens emisor,
            RepositorioIntentos intentos,
            ProveedorDeAutorizacion autorizacion,
            PoliticaDeIntentos politica,
            Clock reloj) {
        this.repositorio = repositorio;
        this.verificador = verificador;
        this.emisor = emisor;
        this.intentos = intentos;
        this.autorizacion = autorizacion;
        this.politica = politica;
        this.reloj = reloj;
        this.hashDeRelleno = verificador.calcularHash("hash-de-relleno-sin-usuario");
    }

    @Override
    public TokenEmitido iniciar(String usuario, String clave) {
        String claveRecibida = clave == null ? "" : clave;
        String nombre = Usuario.normalizar(usuario);
        if (nombre.isEmpty()) {
            verificador.coincide(claveRecibida, hashDeRelleno);
            throw new CredencialesInvalidas();
        }
        Instant ahora = reloj.instant();
        EstadoDeIntentos estado = intentos.leer(nombre);
        if (politica.estaBloqueado(estado, ahora)) {
            throw new InicioBloqueado(politica.segundosRestantes(estado, ahora));
        }
        Optional<Usuario> encontrado = repositorio.buscarPorNombre(nombre);
        if (encontrado.isEmpty()) {
            verificador.coincide(claveRecibida, hashDeRelleno);
            throw fallo(nombre, estado, ahora);
        }
        Usuario existente = encontrado.get();
        // Siempre se verifica, también con la contraseña vacía, para no cambiar el tiempo de
        // respuesta.
        boolean coincide =
                verificador.coincide(claveRecibida, existente.claveHash())
                        && !claveRecibida.isEmpty();
        if (!coincide || !existente.puedeIniciarSesion()) {
            throw fallo(nombre, estado, ahora);
        }
        intentos.borrar(nombre);
        return emisor.emitir(existente, autorizacion.consultar(nombre), ahora);
    }

    /** Cuenta el fallo, bloquea el nombre si llegó al máximo y devuelve el error uniforme. */
    private CredencialesInvalidas fallo(String nombre, EstadoDeIntentos estado, Instant ahora) {
        if (politica.bloqueoVencido(estado, ahora)) {
            intentos.borrar(nombre);
        }
        int fallidos = intentos.sumarFallo(nombre, ahora);
        if (politica.alcanzaElMaximo(fallidos)) {
            intentos.bloquear(nombre, politica.bloqueadoHasta(ahora), ahora);
        }
        intentos.limpiar(ahora.minus(LIMPIEZA), ahora);
        return new CredencialesInvalidas();
    }
}
