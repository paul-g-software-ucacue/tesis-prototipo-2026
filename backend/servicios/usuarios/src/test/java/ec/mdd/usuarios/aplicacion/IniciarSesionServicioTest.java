package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.EstadoDeIntentos;
import ec.mdd.usuarios.dominio.PoliticaDeIntentos;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioIntentos;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Inicio de sesión con los puertos de salida simulados (principio VIII: Mockito solo aquí). */
class IniciarSesionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
    private final RepositorioIntentos intentos = mock(RepositorioIntentos.class);
    private final VerificadorDeClaves verificador = mock(VerificadorDeClaves.class);
    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final Usuario activo =
            new Usuario("autor.prueba", "Autor", "hash", true, Set.of("DOCENTE"));
    private final TokenEmitido token =
            new TokenEmitido("token", Instant.parse("2026-10-13T12:00:00Z"));
    private IniciarSesionServicio servicio;

    @BeforeEach
    void preparar() {
        when(verificador.calcularHash(anyString())).thenReturn("hash-de-relleno");
        when(intentos.leer(anyString())).thenReturn(EstadoDeIntentos.SIN_INTENTOS);
        when(intentos.sumarFallo(anyString(), any())).thenReturn(1);
        servicio =
                new IniciarSesionServicio(
                        repositorio,
                        verificador,
                        emisor,
                        intentos,
                        new PoliticaDeIntentos(5, Duration.ofMinutes(15)),
                        Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void dadoCredencialesCorrectas_cuandoIniciaSesion_entoncesRecibeElTokenYBorraLosIntentos() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));
        when(verificador.coincide("correcta", "hash")).thenReturn(true);
        when(emisor.emitir(activo, AHORA)).thenReturn(token);

        assertThat(servicio.iniciar("autor.prueba", "correcta")).isEqualTo(token);
        verify(intentos).borrar("autor.prueba");
    }

    @Test
    void dadoElNombreConMayusculas_cuandoIniciaSesion_entoncesBuscaYCuentaElNombreNormalizado() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));
        when(verificador.coincide("correcta", "hash")).thenReturn(true);
        when(emisor.emitir(activo, AHORA)).thenReturn(token);

        assertThat(servicio.iniciar(" Autor.Prueba ", "correcta")).isEqualTo(token);
        verify(intentos).leer("autor.prueba");
    }

    @Test
    void dadoUnaContrasenaIncorrecta_cuandoIniciaSesion_entoncesCredencialesInvalidasYCuenta() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", "incorrecta"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(emisor, never()).emitir(any(), any());
        verify(intentos).sumarFallo("autor.prueba", AHORA);
        verify(intentos, never()).bloquear(anyString(), any(), any());
    }

    @Test
    void dadoUnUsuarioInactivo_cuandoIniciaSesionConSuContrasena_entoncesCredencialesInvalidas() {
        Usuario inactivo = new Usuario("autor.prueba", "Autor", "hash", false, Set.of());
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(inactivo));
        when(verificador.coincide("correcta", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", "correcta"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(intentos).sumarFallo("autor.prueba", AHORA);
    }

    @Test
    void dadoUnUsuarioInexistente_cuandoIniciaSesion_entoncesVerificaElRellenoYCuentaElNombre() {
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("nadie", "cualquiera"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(verificador).coincide(eq("cualquiera"), eq("hash-de-relleno"));
        verify(intentos).sumarFallo("nadie", AHORA);
    }

    @Test
    void dadoElQuintoFalloSeguido_cuandoIniciaSesion_entoncesBloqueaElNombreQuinceMinutos() {
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());
        when(intentos.sumarFallo("nadie", AHORA)).thenReturn(5);

        assertThatThrownBy(() -> servicio.iniciar("nadie", "cualquiera"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(intentos).bloquear("nadie", AHORA.plus(Duration.ofMinutes(15)), AHORA);
    }

    @Test
    void dadoUnNombreBloqueado_cuandoIniciaSesion_entoncesInicioBloqueadoSinVerificarNiContar() {
        when(intentos.leer("autor.prueba"))
                .thenReturn(new EstadoDeIntentos(5, AHORA.plusSeconds(600)));

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", "correcta"))
                .isInstanceOf(InicioBloqueado.class)
                .extracting(error -> ((InicioBloqueado) error).segundosRestantes())
                .isEqualTo(600L);
        verify(repositorio, never()).buscarPorNombre(anyString());
        verify(verificador, never()).coincide(anyString(), anyString());
        verify(intentos, never()).sumarFallo(anyString(), any());
    }

    @Test
    void dadoUnBloqueoVencido_cuandoFallaOtraVez_entoncesLaCuentaVuelveAEmpezar() {
        when(intentos.leer("nadie")).thenReturn(new EstadoDeIntentos(5, AHORA.minusSeconds(1)));
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("nadie", "cualquiera"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(intentos).borrar("nadie");
        verify(intentos).sumarFallo("nadie", AHORA);
    }

    @Test
    void dadoUnFallo_cuandoSeCuenta_entoncesLimpiaLosNombresInactivosPorUnDia() {
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("nadie", "cualquiera"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(intentos).limpiar(AHORA.minus(Duration.ofHours(24)), AHORA);
    }

    @Test
    void dadoDatosVacios_cuandoIniciaSesion_entoncesCredencialesInvalidasSinBuscarNiContar() {
        assertThatThrownBy(() -> servicio.iniciar(" ", null))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(repositorio, never()).buscarPorNombre(anyString());
        verify(intentos, never()).sumarFallo(anyString(), any());
    }

    @Test
    void dadoUnaContrasenaVacia_cuandoIniciaSesion_entoncesCredencialesInvalidas() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));
        when(verificador.coincide("", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", ""))
                .isInstanceOf(CredencialesInvalidas.class);
    }
}
