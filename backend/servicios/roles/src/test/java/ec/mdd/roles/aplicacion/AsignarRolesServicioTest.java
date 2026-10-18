package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.SinAdministrador;
import ec.mdd.roles.dominio.TipoDePermiso;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Asignar y quitar roles a un usuario (RF-03, FR-004, FR-011), con Mockito. */
class AsignarRolesServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");

    private static final Rol ADMINISTRADOR =
            new Rol(
                    "ADMINISTRADOR",
                    "Administrador del sistema",
                    "Administra.",
                    true,
                    List.of(new Permiso("USUARIOS_ROLES", TipoDePermiso.ESCRITURA, Alcance.TODO)));
    private static final Rol DOCENTE =
            new Rol(
                    "DOCENTE",
                    "Docente",
                    "Consulta.",
                    true,
                    List.of(new Permiso("MI_DISTRIBUTIVO", TipoDePermiso.LECTURA, Alcance.PROPIO)));
    private static final Rol AUDITOR =
            Rol.nuevo(
                    "Auditor de roles",
                    "Lee roles.",
                    List.of(new Permiso("USUARIOS_ROLES", TipoDePermiso.LECTURA, Alcance.TODO)));

    private final RepositorioRoles roles = mock(RepositorioRoles.class);
    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AsignarRolesServicio servicio =
            new AsignarRolesServicio(
                    roles, asignaciones, registro, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
        when(roles.todos()).thenReturn(List.of(ADMINISTRADOR, AUDITOR, DOCENTE));
        when(asignaciones.todas())
                .thenReturn(
                        Map.of(
                                "admin.demo",
                                List.of("ADMINISTRADOR"),
                                "docente.demo",
                                List.of("DOCENTE")));
        when(asignaciones.rolesDe("docente.demo")).thenReturn(List.of("DOCENTE"));
        when(asignaciones.rolesDe("admin.demo")).thenReturn(List.of("ADMINISTRADOR"));
    }

    @Test
    void dadoUnRolNuevo_cuandoSeAsignaAUnUsuario_entoncesSeReemplazanSusRolesYSeRegistra() {
        List<String> asignados =
                servicio.asignar(
                        " Docente.Demo ", List.of("DOCENTE", "AUDITOR_DE_ROLES"), "admin.demo");

        assertThat(asignados).containsExactly("AUDITOR_DE_ROLES", "DOCENTE");
        verify(asignaciones)
                .reemplazar(
                        "docente.demo",
                        List.of("AUDITOR_DE_ROLES", "DOCENTE"),
                        "admin.demo",
                        AHORA);
        CambioDeRoles cambio = registrado(1).getFirst();
        assertThat(cambio.accion()).isEqualTo(CambioDeRoles.Accion.ASIGNAR_ROL);
        assertThat(cambio.usuario()).isEqualTo("admin.demo");
        assertThat(cambio.detalle())
                .isEqualTo(Map.of("usuario", "docente.demo", "roles", List.of("AUDITOR_DE_ROLES")));
    }

    @Test
    void dadoUnaListaVacia_cuandoSeAsigna_entoncesLeQuitaTodosYRegistraElRetiro() {
        assertThat(servicio.asignar("docente.demo", List.of(), "admin.demo")).isEmpty();

        verify(asignaciones).reemplazar("docente.demo", List.of(), "admin.demo", AHORA);
        CambioDeRoles cambio = registrado(1).getFirst();
        assertThat(cambio.accion()).isEqualTo(CambioDeRoles.Accion.QUITAR_ROL);
        assertThat(cambio.detalle()).containsEntry("roles", List.of("DOCENTE"));
    }

    @Test
    void dadoUnCambioQueAgregaYQuita_cuandoSeAsigna_entoncesRegistraLasDosCosas() {
        servicio.asignar("docente.demo", List.of("AUDITOR_DE_ROLES"), "admin.demo");

        assertThat(registrado(2))
                .extracting(CambioDeRoles::accion)
                .containsExactly(CambioDeRoles.Accion.ASIGNAR_ROL, CambioDeRoles.Accion.QUITAR_ROL);
    }

    @Test
    void dadoUnRolQueNoExiste_cuandoSeAsigna_entoncesRolDesconocidoSinCambiarNada() {
        assertThatThrownBy(() -> servicio.asignar("docente.demo", List.of("BRUJO"), "admin.demo"))
                .isInstanceOf(RolDesconocido.class)
                .hasMessageContaining("BRUJO");
        verify(asignaciones, never()).reemplazar(anyString(), anyCollection(), anyString(), any());
    }

    @Test
    void dadoQueElAdministradorSeQuitaSuRol_cuandoSeAsigna_entoncesSinAdministrador() {
        assertThatThrownBy(() -> servicio.asignar("admin.demo", List.of("DOCENTE"), "admin.demo"))
                .isInstanceOf(SinAdministrador.class);
        verify(asignaciones, never()).reemplazar(anyString(), anyCollection(), anyString(), any());
    }

    @Test
    void dadoLosMismosRoles_cuandoSeAsignan_entoncesNoRegistraNada() {
        servicio.asignar("docente.demo", List.of("DOCENTE"), "admin.demo");

        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoLasAsignaciones_cuandoSeListan_entoncesVienenPorUsuario() {
        assertThat(servicio.rolesDeUsuarios()).containsEntry("docente.demo", List.of("DOCENTE"));
    }

    private List<CambioDeRoles> registrado(int veces) {
        ArgumentCaptor<CambioDeRoles> cambio = ArgumentCaptor.forClass(CambioDeRoles.class);
        verify(registro, times(veces)).registrar(cambio.capture());
        return cambio.getAllValues();
    }
}
