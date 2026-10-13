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

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Inicio de sesión con los puertos de salida simulados (principio VIII: Mockito solo aquí). */
class IniciarSesionServicioTest {

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
    private final VerificadorDeClaves verificador = mock(VerificadorDeClaves.class);
    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final Usuario activo = new Usuario("autor.prueba", "hash", true, Set.of("DOCENTE"));
    private final TokenEmitido token =
            new TokenEmitido("token", Instant.parse("2026-10-13T12:00:00Z"));
    private IniciarSesionServicio servicio;

    @BeforeEach
    void preparar() {
        when(verificador.calcularHash(anyString())).thenReturn("hash-de-relleno");
        servicio = new IniciarSesionServicio(repositorio, verificador, emisor);
    }

    @Test
    void dadoCredencialesCorrectas_cuandoIniciaSesion_entoncesRecibeElToken() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));
        when(verificador.coincide("correcta", "hash")).thenReturn(true);
        when(emisor.emitir(activo)).thenReturn(token);

        assertThat(servicio.iniciar("autor.prueba", "correcta")).isEqualTo(token);
    }

    @Test
    void dadoUnaContrasenaIncorrecta_cuandoIniciaSesion_entoncesCredencialesInvalidas() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", "incorrecta"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(emisor, never()).emitir(any());
    }

    @Test
    void dadoUnUsuarioInactivo_cuandoIniciaSesionConSuContrasena_entoncesCredencialesInvalidas() {
        Usuario inactivo = new Usuario("autor.prueba", "hash", false, Set.of());
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(inactivo));
        when(verificador.coincide("correcta", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", "correcta"))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    void dadoUnUsuarioInexistente_cuandoIniciaSesion_entoncesIgualVerificaLaContrasena() {
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("nadie", "cualquiera"))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(verificador).coincide(eq("cualquiera"), eq("hash-de-relleno"));
    }

    @Test
    void dadoDatosVacios_cuandoIniciaSesion_entoncesCredencialesInvalidasSinBuscar() {
        assertThatThrownBy(() -> servicio.iniciar(" ", null))
                .isInstanceOf(CredencialesInvalidas.class);
        verify(repositorio, never()).buscarPorNombre(anyString());
    }

    @Test
    void dadoUnaContrasenaVacia_cuandoIniciaSesion_entoncesCredencialesInvalidas() {
        when(repositorio.buscarPorNombre("autor.prueba")).thenReturn(Optional.of(activo));
        when(verificador.coincide("", "hash")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar("autor.prueba", ""))
                .isInstanceOf(CredencialesInvalidas.class);
    }
}
