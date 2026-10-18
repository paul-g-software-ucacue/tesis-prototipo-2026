package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.CatalogoDeFuncionalidades;
import ec.mdd.roles.dominio.Funcionalidad;
import ec.mdd.roles.dominio.NombreRepetido;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.PermisosInvalidos;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.RolAsignado;
import ec.mdd.roles.dominio.RolPredefinido;
import ec.mdd.roles.dominio.SinAdministrador;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.entrada.DatosDeRol;
import ec.mdd.roles.puertos.entrada.RolConUsuarios;
import ec.mdd.roles.puertos.entrada.RolNoEncontrado;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Crear, editar y borrar roles con las reglas de la administración (FR-004, FR-011), con Mockito.
 */
class GestionarRolesServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");

    private static final Permiso ADMINISTRAR =
            new Permiso("USUARIOS_ROLES", TipoDePermiso.ESCRITURA, Alcance.TODO);
    private static final Permiso LEER_CARGAS =
            new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO);
    private static final Permiso LEER_DOCENTES =
            new Permiso("DOCENTES", TipoDePermiso.LECTURA, Alcance.TODO);

    private static final Rol ADMINISTRADOR =
            new Rol(
                    "ADMINISTRADOR",
                    "Administrador del sistema",
                    "Administra.",
                    true,
                    List.of(ADMINISTRAR));
    private static final Rol AUDITOR =
            Rol.nuevo("Auditor de cargas", "Revisa cargas.", List.of(LEER_CARGAS));
    private static final Rol GESTOR =
            Rol.nuevo("Gestor de roles", "Administra.", List.of(ADMINISTRAR));

    private final RepositorioRoles roles = mock(RepositorioRoles.class);
    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final GestionarRolesServicio servicio =
            new GestionarRolesServicio(
                    roles, asignaciones, registro, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
        when(roles.funcionalidades())
                .thenReturn(
                        new CatalogoDeFuncionalidades(
                                List.of(
                                        new Funcionalidad(
                                                "USUARIOS_ROLES",
                                                "Usuarios y roles",
                                                EnumSet.of(
                                                        TipoDePermiso.LECTURA,
                                                        TipoDePermiso.ESCRITURA)),
                                        new Funcionalidad(
                                                "CARGAS",
                                                "Cargas de datos",
                                                EnumSet.of(
                                                        TipoDePermiso.LECTURA,
                                                        TipoDePermiso.ESCRITURA)),
                                        new Funcionalidad(
                                                "DOCENTES",
                                                "Docentes",
                                                EnumSet.of(TipoDePermiso.LECTURA)))));
        when(roles.todos()).thenReturn(List.of(ADMINISTRADOR, AUDITOR));
        when(roles.buscar("ADMINISTRADOR")).thenReturn(Optional.of(ADMINISTRADOR));
        when(roles.buscar("AUDITOR_DE_CARGAS")).thenReturn(Optional.of(AUDITOR));
        when(asignaciones.usuariosPorRol()).thenReturn(Map.of("ADMINISTRADOR", 1));
        when(asignaciones.todas()).thenReturn(Map.of("admin.demo", List.of("ADMINISTRADOR")));
    }

    @Test
    void dadoLosRoles_cuandoSeListan_entoncesCadaUnoTraeCuantosUsuariosLoTienen() {
        assertThat(servicio.roles())
                .containsExactly(
                        new RolConUsuarios(ADMINISTRADOR, 1), new RolConUsuarios(AUDITOR, 0));
        assertThat(servicio.funcionalidades()).hasSize(3);
    }

    @Test
    void dadoUnRolValido_cuandoSeCrea_entoncesSeGuardaYSeRegistraConQuienYCuando() {
        RolConUsuarios creado =
                servicio.crear(
                        new DatosDeRol("Revisor", "Revisa docentes.", List.of(LEER_DOCENTES)),
                        "admin.demo");

        assertThat(creado.rol().identificador()).isEqualTo("REVISOR");
        assertThat(creado.usuarios()).isZero();
        verify(roles).guardar(creado.rol(), AHORA);
        CambioDeRoles cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(CambioDeRoles.Accion.CREAR_ROL);
        assertThat(cambio.usuario()).isEqualTo("admin.demo");
        assertThat(cambio.momento()).isEqualTo(AHORA);
        assertThat(cambio.detalle())
                .containsEntry("identificador", "REVISOR")
                .containsEntry("permisos", List.of("DOCENTES:LECTURA"));
    }

    @Test
    void dadoUnNombreRepetidoOUnPermisoNoAdmitido_cuandoSeCrea_entoncesNoSeGuarda() {
        assertThatThrownBy(
                        () ->
                                servicio.crear(
                                        new DatosDeRol(
                                                "auditor de CARGAS", "Otra.", List.of(LEER_CARGAS)),
                                        "admin.demo"))
                .isInstanceOf(NombreRepetido.class);
        assertThatThrownBy(
                        () ->
                                servicio.crear(
                                        new DatosDeRol(
                                                "Nuevo",
                                                "Otra.",
                                                List.of(
                                                        new Permiso(
                                                                "DOCENTES",
                                                                TipoDePermiso.ESCRITURA,
                                                                Alcance.TODO))),
                                        "admin.demo"))
                .isInstanceOf(PermisosInvalidos.class);
        verify(roles, never()).guardar(any(), any());
    }

    @Test
    void dadoUnRolPropio_cuandoSeEdita_entoncesConservaSuIdentificadorYSeRegistra() {
        RolConUsuarios editado =
                servicio.editar(
                        "AUDITOR_DE_CARGAS",
                        new DatosDeRol(
                                "Auditor de cargas", "Ahora docentes.", List.of(LEER_DOCENTES)),
                        "admin.demo");

        assertThat(editado.rol().identificador()).isEqualTo("AUDITOR_DE_CARGAS");
        assertThat(editado.rol().permisos()).containsExactly(LEER_DOCENTES);
        verify(roles).guardar(editado.rol(), AHORA);
        assertThat(registrado().accion()).isEqualTo(CambioDeRoles.Accion.EDITAR_ROL);
    }

    @Test
    void dadoUnRolPredefinidoOInexistente_cuandoSeEdita_entoncesSeRechaza() {
        DatosDeRol datos = new DatosDeRol("Otro", "Otro.", List.of(LEER_CARGAS));

        assertThatThrownBy(() -> servicio.editar("ADMINISTRADOR", datos, "admin.demo"))
                .isInstanceOf(RolPredefinido.class);
        assertThatThrownBy(() -> servicio.editar("NO_EXISTE", datos, "admin.demo"))
                .isInstanceOf(RolNoEncontrado.class);
        verify(roles, never()).guardar(any(), any());
    }

    @Test
    void dadoUnRolPropioQueEsLaUnicaFuenteDelPermiso_cuandoSeLeQuita_entoncesSinAdministrador() {
        when(roles.todos()).thenReturn(List.of(ADMINISTRADOR, GESTOR));
        when(roles.buscar("GESTOR_DE_ROLES")).thenReturn(Optional.of(GESTOR));
        when(asignaciones.todas()).thenReturn(Map.of("gestor.demo", List.of("GESTOR_DE_ROLES")));

        assertThatThrownBy(
                        () ->
                                servicio.editar(
                                        "GESTOR_DE_ROLES",
                                        new DatosDeRol(
                                                "Gestor de roles", "Ya no.", List.of(LEER_CARGAS)),
                                        "gestor.demo"))
                .isInstanceOf(SinAdministrador.class);
        verify(roles, never()).guardar(any(), any());
    }

    @Test
    void dadoUnRolPropioSinUsuarios_cuandoSeBorra_entoncesSeBorraYSeRegistra() {
        servicio.borrar("AUDITOR_DE_CARGAS", "admin.demo");

        verify(roles).borrar("AUDITOR_DE_CARGAS");
        assertThat(registrado().accion()).isEqualTo(CambioDeRoles.Accion.BORRAR_ROL);
    }

    @Test
    void dadoUnRolAsignadoOPredefinido_cuandoSeBorra_entoncesSeRechaza() {
        when(asignaciones.usuariosPorRol())
                .thenReturn(Map.of("ADMINISTRADOR", 1, "AUDITOR_DE_CARGAS", 2));

        assertThatThrownBy(() -> servicio.borrar("AUDITOR_DE_CARGAS", "admin.demo"))
                .isInstanceOf(RolAsignado.class);
        assertThatThrownBy(() -> servicio.borrar("ADMINISTRADOR", "admin.demo"))
                .isInstanceOf(RolPredefinido.class);
        verify(roles, never()).borrar(any());
    }

    @Test
    void dadoUnRolQueNoExiste_cuandoSeConsulta_entoncesRolNoEncontrado() {
        assertThatThrownBy(() -> servicio.consultar("NO_EXISTE"))
                .isInstanceOf(RolNoEncontrado.class);
        assertThat(servicio.consultar("ADMINISTRADOR").usuarios()).isEqualTo(1);
    }

    private CambioDeRoles registrado() {
        ArgumentCaptor<CambioDeRoles> cambio = ArgumentCaptor.forClass(CambioDeRoles.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }
}
