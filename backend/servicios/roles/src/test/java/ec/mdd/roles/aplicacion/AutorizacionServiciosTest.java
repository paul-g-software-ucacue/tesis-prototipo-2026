package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.entrada.Autorizacion;
import ec.mdd.roles.puertos.entrada.RolDesconocido;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Consultar la autorización y dar los roles iniciales (ADR-021, research §8), con Mockito. */
class AutorizacionServiciosTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");

    private static final Rol DOCENTE =
            new Rol(
                    "DOCENTE",
                    "Docente",
                    "Consulta su distributivo.",
                    true,
                    List.of(
                            new Permiso("MI_DISTRIBUTIVO", TipoDePermiso.LECTURA, Alcance.PROPIO),
                            new Permiso(
                                    "MI_DISTRIBUTIVO", TipoDePermiso.ESCRITURA, Alcance.PROPIO)));
    private static final Rol COORDINADOR =
            new Rol(
                    "COORDINADOR_CARRERA",
                    "Coordinador de carrera",
                    "Planifica la oferta.",
                    true,
                    List.of(new Permiso("DOCENTES", TipoDePermiso.LECTURA, Alcance.TODO)));

    private final RepositorioRoles roles = mock(RepositorioRoles.class);
    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final ConsultarAutorizacionServicio consultar =
            new ConsultarAutorizacionServicio(roles, asignaciones);
    private final AsignarRolesInicialesServicio iniciales =
            new AsignarRolesInicialesServicio(roles, asignaciones, registro, reloj);

    @Test
    void dadoUnUsuarioConDosRoles_cuandoSeConsulta_entoncesTieneLaUnionDeSusPermisos() {
        when(asignaciones.rolesDe("docente.coordinador.demo"))
                .thenReturn(List.of("COORDINADOR_CARRERA", "DOCENTE"));
        when(roles.todos()).thenReturn(List.of(COORDINADOR, DOCENTE));

        Autorizacion autorizacion = consultar.consultar(" Docente.Coordinador.Demo ");

        assertThat(autorizacion.usuario()).isEqualTo("docente.coordinador.demo");
        assertThat(autorizacion.roles()).containsExactly("COORDINADOR_CARRERA", "DOCENTE");
        assertThat(autorizacion.permisos())
                .containsExactly(
                        "DOCENTES:LECTURA",
                        "MI_DISTRIBUTIVO:ESCRITURA:PROPIO",
                        "MI_DISTRIBUTIVO:LECTURA:PROPIO");
    }

    @Test
    void dadoUnUsuarioSinRoles_cuandoSeConsulta_entoncesNoTieneNada() {
        when(asignaciones.rolesDe("nadie.demo")).thenReturn(List.of());

        Autorizacion autorizacion = consultar.consultar("nadie.demo");

        assertThat(autorizacion.roles()).isEmpty();
        assertThat(autorizacion.permisos()).isEmpty();
    }

    @Test
    void dadoUnUsuarioNuevo_cuandoSePidenSusRolesIniciales_entoncesSeAsignanYSeRegistran() {
        when(roles.todos()).thenReturn(List.of(COORDINADOR, DOCENTE));
        when(asignaciones.inicializar("docente.demo", List.of("DOCENTE"), AHORA)).thenReturn(true);

        assertThat(iniciales.asignar("Docente.Demo", List.of("DOCENTE"))).isTrue();

        ArgumentCaptor<CambioDeRoles> cambio = ArgumentCaptor.forClass(CambioDeRoles.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioDeRoles.Accion.INICIALIZAR);
        assertThat(cambio.getValue().usuario()).isEqualTo("sistema");
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle())
                .isEqualTo(Map.of("usuario", "docente.demo", "roles", List.of("DOCENTE")));
    }

    @Test
    void dadoUnUsuarioYaInicializado_cuandoSePidenSusRolesIniciales_entoncesNoHaceNada() {
        when(roles.todos()).thenReturn(List.of(DOCENTE));
        when(asignaciones.inicializar("docente.demo", List.of("DOCENTE"), AHORA)).thenReturn(false);

        assertThat(iniciales.asignar("docente.demo", List.of("DOCENTE"))).isFalse();

        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnRolQueNoExiste_cuandoSePidenLosRolesIniciales_entoncesFallaYDiceCual() {
        when(roles.todos()).thenReturn(List.of(DOCENTE));

        assertThatThrownBy(() -> iniciales.asignar("docente.demo", List.of("DOCENTE", "BRUJO")))
                .isInstanceOf(RolDesconocido.class)
                .hasMessageContaining("BRUJO")
                .hasMessageNotContaining("DOCENTE,");
        verify(asignaciones, never()).inicializar(anyString(), anyCollection(), any());
    }
}
