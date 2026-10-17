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
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Renovación del token de sesión (ADR-020), con los puertos de salida simulados. */
class RenovarSesionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final Usuario docente =
            new Usuario("docente.demo", "Docente (demo)", "hash", true, Set.of("DOCENTE"));
    private final TokenEmitido token = new TokenEmitido("nuevo", AHORA.plusSeconds(1800));
    private final RenovarSesionServicio servicio =
            new RenovarSesionServicio(
                    repositorio, emisor, Duration.ofHours(8), Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnaSesionDeMenosDeOchoHoras_cuandoSeRenueva_entoncesEmiteOtroConElMismoInicio() {
        Instant inicio = AHORA.minus(Duration.ofHours(7));
        when(repositorio.buscarPorNombre("docente.demo")).thenReturn(Optional.of(docente));
        when(emisor.emitir(docente, inicio)).thenReturn(token);

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
        verify(emisor, never()).emitir(any(), any());
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
        Usuario desactivado =
                new Usuario("docente.demo", "Docente (demo)", "hash", false, Set.of("DOCENTE"));
        when(repositorio.buscarPorNombre("docente.demo")).thenReturn(Optional.of(desactivado));
        when(repositorio.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.renovar("docente.demo", List.of("DOCENTE"), AHORA))
                .isInstanceOf(SesionNoRenovable.class);
        assertThatThrownBy(() -> servicio.renovar("nadie", List.of("DOCENTE"), AHORA))
                .isInstanceOf(SesionNoRenovable.class);
    }
}
