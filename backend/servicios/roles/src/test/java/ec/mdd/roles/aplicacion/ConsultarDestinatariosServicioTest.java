package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.entrada.Destinatario;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A quién toca un mensaje en Roles: el rol, el designado del periodo o la carrera (F16-H01). */
class ConsultarDestinatariosServicioTest {

    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RepositorioDesignaciones designaciones = mock(RepositorioDesignaciones.class);
    private final RepositorioCarrerasDeCoordinadores carreras =
            mock(RepositorioCarrerasDeCoordinadores.class);
    private final ConsultarDestinatariosServicio servicio =
            new ConsultarDestinatariosServicio(asignaciones, designaciones, carreras);

    @BeforeEach
    void preparar() {
        when(asignaciones.todas())
                .thenReturn(
                        Map.of(
                                "vicerrectorado.demo", List.of("VICERRECTOR"),
                                "vice.dos", List.of("DOCENTE", "VICERRECTOR"),
                                "coordinacion.demo", List.of("COORDINADOR_CARRERA"),
                                "coordinacion.adm", List.of("COORDINADOR_CARRERA"),
                                "coordinacion.sin", List.of("COORDINADOR_CARRERA"),
                                "consolidacion.demo", List.of("RESPONSABLE_CONSOLIDACION")));
        when(carreras.todas())
                .thenReturn(Map.of("coordinacion.demo", "TI", "coordinacion.adm", "ADM"));
    }

    @Test
    void dadoUnRolCualquiera_cuandoSeConsulta_entoncesTodosSusUsuariosOrdenados() {
        assertThat(servicio.destinatarios("VICERRECTOR", "2026-I", Optional.empty()))
                .containsExactly(
                        new Destinatario("vice.dos", Optional.empty()),
                        new Destinatario("vicerrectorado.demo", Optional.empty()));
    }

    @Test
    void dadoElResponsable_cuandoElPeriodoTieneDesignado_entoncesElRolFijoYElDesignado() {
        when(designaciones.dePeriodo("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Designacion(
                                        "2026-I",
                                        "docente.demo",
                                        "vicerrectorado.demo",
                                        Instant.parse("2026-11-02T15:00:00Z"))));

        assertThat(servicio.destinatarios("RESPONSABLE_CONSOLIDACION", "2026-I", Optional.empty()))
                .containsExactly(
                        new Destinatario("consolidacion.demo", Optional.empty()),
                        new Destinatario("docente.demo", Optional.empty()));
    }

    @Test
    void dadoElResponsable_cuandoElPeriodoNoTieneDesignado_entoncesSoloElRolFijo() {
        when(designaciones.dePeriodo("2026-I")).thenReturn(Optional.empty());

        assertThat(servicio.destinatarios("RESPONSABLE_CONSOLIDACION", "2026-I", Optional.empty()))
                .containsExactly(new Destinatario("consolidacion.demo", Optional.empty()));
    }

    @Test
    void dadoElDesignadoConElRolFijo_cuandoSeConsulta_entoncesUnaSolaVez() {
        when(designaciones.dePeriodo("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Designacion(
                                        "2026-I",
                                        "consolidacion.demo",
                                        "vicerrectorado.demo",
                                        Instant.parse("2026-11-02T15:00:00Z"))));

        assertThat(servicio.destinatarios("RESPONSABLE_CONSOLIDACION", "2026-I", Optional.empty()))
                .containsExactly(new Destinatario("consolidacion.demo", Optional.empty()));
    }

    @Test
    void dadoElCoordinadorConCarrera_cuandoSeConsulta_entoncesSoloLosDeEsaCarreraConElla() {
        assertThat(servicio.destinatarios("COORDINADOR_CARRERA", "2026-I", Optional.of("TI")))
                .containsExactly(new Destinatario("coordinacion.demo", Optional.of("TI")));
    }

    @Test
    void dadoElCoordinadorSinCarrera_cuandoSeConsulta_entoncesTodosLosCoordinadores() {
        assertThat(servicio.destinatarios("COORDINADOR_CARRERA", "2026-I", Optional.empty()))
                .extracting(Destinatario::usuario)
                .containsExactly("coordinacion.adm", "coordinacion.demo", "coordinacion.sin");
    }

    @Test
    void dadoUnRolDesconocidoOSinUsuarios_cuandoSeConsulta_entoncesNadie() {
        assertThat(servicio.destinatarios("JEFE", "2026-I", Optional.empty())).isEmpty();
        assertThat(servicio.destinatarios("RECTOR", "2026-I", Optional.empty())).isEmpty();
    }
}
