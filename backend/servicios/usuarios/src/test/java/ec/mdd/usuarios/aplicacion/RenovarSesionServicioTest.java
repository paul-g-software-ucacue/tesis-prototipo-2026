package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.AutorizacionDeUsuario;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Renovación del token de sesión (ADR-020), con los puertos de salida simulados. */
class RenovarSesionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final ProveedorDeAutorizacion autorizacion = mock(ProveedorDeAutorizacion.class);
    private final Usuario docente = new Usuario("docente.demo", "Docente (demo)", "hash", true);
    private final TokenEmitido token = new TokenEmitido("nuevo", AHORA.plusSeconds(1800));
    private final RenovarSesionServicio servicio =
            new RenovarSesionServicio(
                    repositorio,
                    emisor,
                    autorizacion,
                    Duration.ofHours(8),
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnaSesionDeMenosDeOchoHoras_cuandoSeRenueva_entoncesEmiteOtroConElMismoInicio() {
        Instant inicio = AHORA.minus(Duration.ofHours(7));
        when(repositorio.buscarPorNombre("docente.demo")).thenReturn(Optional.of(docente));
        AutorizacionDeUsuario vigente =
                new AutorizacionDeUsuario(
                        List.of("AUDITOR_DE_ROLES", "DOCENTE"), List.of("USUARIOS_ROLES:LECTURA"));
        when(autorizacion.consultar("docente.demo")).thenReturn(vigente);
        when(emisor.emitir(docente, vigente, inicio)).thenReturn(token);

        // El token viejo dice solo DOCENTE; el nuevo lleva lo que Roles dice ahora (D-62).
        assertThat(servicio.renovar("docente.demo", List.of("DOCENTE"), inicio)).isEqualTo(token);
    }

    @Test
    void dadoUnaSesionDeOchoHoras_cuandoSeRenueva_entoncesSesionNoRenovable() {
        assertThatThrownBy(
                        () ->
                                servicio.renovar(
                                        "docente.demo",
                                        List.of("DOCENTE"),
                                        AHORA.minus(Duration.ofHours(8))))
                .isInstanceOf(SesionNoRenovable.class);
        verify(emisor, never()).emitir(any(), any(), any());
    }

    @Test
    void dadoUnTokenDeProceso_cuandoSeRenueva_entoncesSesionNoRenovable() {
        assertThatThrownBy(() -> servicio.renovar("proceso:distributivo", List.of("PROCESO"), null))
                .isInstanceOf(SesionNoRenovable.class);
        assertThatThrownBy(() -> servicio.renovar("docente.demo", List.of("PROCESO"), AHORA))
                .isInstanceOf(SesionNoRenovable.class);
    }

    @Test
    void dadoUnUsuarioDesactivadoOBorrado_cuandoSeRenueva_entoncesSesionNoRenovable() {
        Usuario desactivado = new Usuario("docente.demo", "Docente (demo)", "hash", false);
        when(repositorio.buscarPorNombre("docente.demo")).thenReturn(Optional.of(desactivado));
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.renovar("docente.demo", List.of("DOCENTE"), AHORA))
                .isInstanceOf(SesionNoRenovable.class);
        assertThatThrownBy(() -> servicio.renovar("nadie", List.of("DOCENTE"), AHORA))
                .isInstanceOf(SesionNoRenovable.class);
    }
}
