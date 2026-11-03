package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Quién puede firmar en un periodo, sin decir qué usuarios (RF-58, principio IX). */
class ConsultarFirmantesServicioTest {

    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RepositorioDesignaciones designaciones = mock(RepositorioDesignaciones.class);
    private final ConsultarFirmantesServicio servicio =
            new ConsultarFirmantesServicio(asignaciones, designaciones);

    @Test
    void dadosRolesConYSinUsuarios_cuandoSeConsulta_entoncesSoloLosQueTienen() {
        when(asignaciones.usuariosPorRol())
                .thenReturn(Map.of("RECTOR", 1, "VICERRECTOR", 2, "SECRETARIA_GENERAL", 0));
        when(designaciones.dePeriodo("2026-I")).thenReturn(Optional.empty());

        assertThat(servicio.firmantes("2026-I").rolesConUsuario())
                .containsExactlyInAnyOrder("RECTOR", "VICERRECTOR");
    }

    @Test
    void sinDesignacion_cuandoSeConsulta_entoncesNoHayResponsable() {
        when(asignaciones.usuariosPorRol()).thenReturn(Map.of());
        when(designaciones.dePeriodo("2026-I")).thenReturn(Optional.empty());

        assertThat(servicio.firmantes("2026-I").responsableDesignado()).isFalse();
    }

    @Test
    void conDesignacion_cuandoSeConsulta_entoncesHayResponsable() {
        when(asignaciones.usuariosPorRol()).thenReturn(Map.of());
        when(designaciones.dePeriodo("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Designacion(
                                        "2026-I",
                                        "consolidacion.demo",
                                        "vicerrectorado.demo",
                                        Instant.parse("2026-11-02T15:00:00Z"))));

        assertThat(servicio.firmantes("2026-I").responsableDesignado()).isTrue();
    }
}
