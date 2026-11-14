package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.entrada.ResponsableDelPeriodo;
import ec.mdd.roles.puertos.entrada.SinResponsable;
import ec.mdd.roles.puertos.requeridos.NombreDeUsuario;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El Responsable de consolidación del periodo y su nombre, para el bloque de firmas (F13-H02). */
class ConsultarResponsableServicioTest {

    private final RepositorioDesignaciones designaciones = mock(RepositorioDesignaciones.class);
    private final NombreDeUsuario nombres = mock(NombreDeUsuario.class);
    private final ConsultarResponsableServicio servicio =
            new ConsultarResponsableServicio(designaciones, nombres);

    @Test
    void dadaLaDesignacion_cuandoSeConsulta_entoncesElUsuarioYSuNombreParaMostrar() {
        designado();
        when(nombres.nombre("consolidacion.demo")).thenReturn(Optional.of("PÉREZ ANA"));

        assertThat(servicio.responsable("2026-I"))
                .isEqualTo(new ResponsableDelPeriodo("consolidacion.demo", "PÉREZ ANA"));
    }

    @Test
    void dadoUnUsuarioSinNombreParaMostrar_cuandoSeConsulta_entoncesElNombreDeUsuario() {
        designado();
        when(nombres.nombre("consolidacion.demo")).thenReturn(Optional.empty());

        assertThat(servicio.responsable("2026-I").nombre()).isEqualTo("consolidacion.demo");
    }

    @Test
    void dadoUnPeriodoSinDesignacion_cuandoSeConsulta_entoncesSinResponsable() {
        when(designaciones.dePeriodo("2026-I")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.responsable("2026-I")).isInstanceOf(SinResponsable.class);
    }

    private void designado() {
        when(designaciones.dePeriodo("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Designacion(
                                        "2026-I",
                                        "consolidacion.demo",
                                        "vicerrectorado.demo",
                                        Instant.parse("2026-11-01T15:00:00Z"))));
    }
}
