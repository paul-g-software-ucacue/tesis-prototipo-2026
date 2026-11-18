package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La cuenta de una identificación, para la notificación de firma pendiente (F16-H02, D-110): el
 * resumen, sin la identificación ni el hash, activa o no.
 */
class ConsultarCuentaPorIdentificacionServicioTest {

    private static final String CEDULA = "0109990010";

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);
    private final ConsultarCuentaPorIdentificacionServicio servicio =
            new ConsultarCuentaPorIdentificacionServicio(repositorio);

    @Test
    void dadaUnaCuentaActivaConLaIdentificacion_cuandoSeConsulta_entoncesDaSuResumen() {
        when(repositorio.buscarPorIdentificacion(CEDULA))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        "rectorado.demo",
                                        "Rectorado (demo)",
                                        "hash",
                                        true,
                                        Optional.of(CEDULA))));

        assertThat(servicio.de(CEDULA))
                .contains(new ResumenDeUsuario("rectorado.demo", "Rectorado (demo)", true));
    }

    @Test
    void dadaUnaCuentaInactiva_cuandoSeConsulta_entoncesLaDaComoInactiva() {
        when(repositorio.buscarPorIdentificacion(CEDULA))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        "rectorado.demo",
                                        "Rectorado (demo)",
                                        "hash",
                                        false,
                                        Optional.of(CEDULA))));

        assertThat(servicio.de(CEDULA))
                .contains(new ResumenDeUsuario("rectorado.demo", "Rectorado (demo)", false));
    }

    @Test
    void dadoQueNingunaCuentaTieneLaIdentificacion_cuandoSeConsulta_entoncesVacio() {
        when(repositorio.buscarPorIdentificacion(CEDULA)).thenReturn(Optional.empty());

        assertThat(servicio.de(CEDULA)).isEmpty();
    }
}
