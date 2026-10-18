package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Las reglas de la administración de roles (D-63, casos límite de la especificación). */
class ReglaDeAdministracionTest {

    private static final Permiso ADMINISTRAR =
            new Permiso("USUARIOS_ROLES", TipoDePermiso.ESCRITURA, Alcance.TODO);
    private static final Permiso LEER_CARGAS =
            new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO);

    private static final Rol ADMINISTRADOR =
            new Rol(
                    "ADMINISTRADOR",
                    "Administrador del sistema",
                    "Todo, incluida la bitácora.",
                    true,
                    List.of(ADMINISTRAR));
    private static final Rol AUDITOR =
            Rol.nuevo("Auditor de cargas", "Revisa cargas.", List.of(LEER_CARGAS));
    private static final Rol GESTOR =
            Rol.nuevo("Gestor de roles", "Administra.", List.of(ADMINISTRAR));

    @Test
    void dadoUnRolPredefinido_cuandoSeEditaOSeBorra_entoncesSeRechaza() {
        assertThatThrownBy(() -> ReglaDeAdministracion.editar(ADMINISTRADOR))
                .isInstanceOf(RolPredefinido.class);
        assertThatThrownBy(() -> ReglaDeAdministracion.borrar(ADMINISTRADOR, 0))
                .isInstanceOf(RolPredefinido.class);
        assertThatCode(() -> ReglaDeAdministracion.editar(AUDITOR)).doesNotThrowAnyException();
    }

    @Test
    void dadoUnRolAsignado_cuandoSeBorra_entoncesSeRechazaConCuantos() {
        assertThatThrownBy(() -> ReglaDeAdministracion.borrar(AUDITOR, 3))
                .isInstanceOfSatisfying(
                        RolAsignado.class,
                        error ->
                                org.assertj.core.api.Assertions.assertThat(error.usuarios())
                                        .isEqualTo(3));
        assertThatCode(() -> ReglaDeAdministracion.borrar(AUDITOR, 0)).doesNotThrowAnyException();
    }

    @Test
    void dadoUnNombreQueDaUnIdentificadorExistente_cuandoSeUsa_entoncesEsRepetido() {
        List<Rol> roles = List.of(ADMINISTRADOR, AUDITOR);

        assertThatThrownBy(
                        () ->
                                ReglaDeAdministracion.nombreDisponible(
                                        "Auditor de Cargas.", null, roles))
                .isInstanceOf(NombreRepetido.class);
        // «Administrador» no es el nombre de ningún rol, pero da el identificador ADMINISTRADOR.
        assertThatThrownBy(
                        () -> ReglaDeAdministracion.nombreDisponible("Administrador", null, roles))
                .isInstanceOf(NombreRepetido.class);
        assertThatThrownBy(
                        () ->
                                ReglaDeAdministracion.nombreDisponible(
                                        "administrador del sistema", null, roles))
                .isInstanceOf(NombreRepetido.class);
        assertThatCode(() -> ReglaDeAdministracion.nombreDisponible("Revisor", null, roles))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnRolQueConservaSuNombre_cuandoSeEdita_entoncesNoChocaConsigoMismo() {
        Rol renombrado = AUDITOR.editado("Revisor de cargas", "Revisa.", List.of(LEER_CARGAS));
        List<Rol> roles = List.of(ADMINISTRADOR, renombrado);

        assertThatCode(
                        () ->
                                ReglaDeAdministracion.nombreDisponible(
                                        "Auditor de cargas", "AUDITOR_DE_CARGAS", roles))
                .doesNotThrowAnyException();
        // Otro rol no puede tomar el identificador AUDITOR_DE_CARGAS aunque ya no sea un nombre.
        assertThatThrownBy(
                        () ->
                                ReglaDeAdministracion.nombreDisponible(
                                        "Auditor de cargas", null, roles))
                .isInstanceOf(NombreRepetido.class);
    }

    @Test
    void dadoUnCambioQueDejaAlSistemaSinAdministrador_cuandoSeAplica_entoncesSeRechaza() {
        Map<String, Set<Rol>> despues =
                Map.of("admin.demo", Set.of(AUDITOR), "docente.demo", Set.of());

        assertThatThrownBy(() -> ReglaDeAdministracion.conservaAdministrador(despues, "admin.demo"))
                .isInstanceOf(SinAdministrador.class);
    }

    @Test
    void dadoUnCambioQueLeQuitaElPermisoAQuienLoHace_cuandoSeAplica_entoncesSeRechaza() {
        Map<String, Set<Rol>> despues =
                Map.of("admin.demo", Set.of(AUDITOR), "otro.admin", Set.of(ADMINISTRADOR));

        assertThatThrownBy(() -> ReglaDeAdministracion.conservaAdministrador(despues, "admin.demo"))
                .isInstanceOf(SinAdministrador.class);
        assertThatCode(() -> ReglaDeAdministracion.conservaAdministrador(despues, "otro.admin"))
                .doesNotThrowAnyException();
    }

    @Test
    void
            dadoLaEdicionDeUnRolPropioQueEsLaUnicaFuenteDelPermiso_cuandoSeLoQuita_entoncesSeRechaza() {
        Rol gestorSinPermiso = GESTOR.editado("Gestor de roles", "Ya no.", List.of(LEER_CARGAS));
        Map<String, Set<Rol>> despues = Map.of("gestor.demo", Set.of(gestorSinPermiso));

        assertThatThrownBy(
                        () -> ReglaDeAdministracion.conservaAdministrador(despues, "gestor.demo"))
                .isInstanceOf(SinAdministrador.class);
        assertThatCode(
                        () ->
                                ReglaDeAdministracion.conservaAdministrador(
                                        Map.of("gestor.demo", Set.of(GESTOR)), "gestor.demo"))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnCambioSinQuienLoHace_cuandoQuedaAlgunAdministrador_entoncesPasa() {
        assertThatCode(
                        () ->
                                ReglaDeAdministracion.conservaAdministrador(
                                        Map.of("admin.demo", Set.of(ADMINISTRADOR)), null))
                .doesNotThrowAnyException();
    }
}
