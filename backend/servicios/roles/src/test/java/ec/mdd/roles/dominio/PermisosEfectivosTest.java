package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** La unión de los permisos de varios roles (RF-03, FR-005). */
class PermisosEfectivosTest {

    private static Rol rol(String nombre, Permiso... permisos) {
        return Rol.nuevo(nombre, "Descripción.", List.of(permisos));
    }

    @Test
    void dadoDosRoles_cuandoSeUnen_entoncesTieneLosPermisosDeLosDosSinRepetir() {
        Rol docente =
                rol(
                        "Docente",
                        new Permiso("MI_DISTRIBUTIVO", TipoDePermiso.LECTURA, Alcance.PROPIO),
                        new Permiso("MI_DISTRIBUTIVO", TipoDePermiso.ESCRITURA, Alcance.PROPIO));
        Rol coordinador =
                rol(
                        "Coordinador",
                        new Permiso("DOCENTES", TipoDePermiso.LECTURA, Alcance.TODO),
                        new Permiso("MI_DISTRIBUTIVO", TipoDePermiso.LECTURA, Alcance.PROPIO));

        PermisosEfectivos efectivos = PermisosEfectivos.de(List.of(coordinador, docente));

        assertThat(efectivos.roles()).containsExactly("COORDINADOR", "DOCENTE");
        assertThat(efectivos.textos())
                .containsExactly(
                        "DOCENTES:LECTURA",
                        "MI_DISTRIBUTIVO:ESCRITURA:PROPIO",
                        "MI_DISTRIBUTIVO:LECTURA:PROPIO");
    }

    @Test
    void dadoElMismoPermisoConDosAlcances_cuandoSeUnen_entoncesGanaTodo() {
        PermisosEfectivos efectivos =
                PermisosEfectivos.de(
                        List.of(
                                rol(
                                        "Propio",
                                        new Permiso(
                                                "OFERTA", TipoDePermiso.LECTURA, Alcance.PROPIO)),
                                rol(
                                        "Todo",
                                        new Permiso(
                                                "OFERTA", TipoDePermiso.LECTURA, Alcance.TODO))));

        assertThat(efectivos.textos()).containsExactly("OFERTA:LECTURA");
    }

    @Test
    void dadoLecturaEnUnRolYEscrituraEnOtro_cuandoSeUnen_entoncesTieneLosDos() {
        PermisosEfectivos efectivos =
                PermisosEfectivos.de(
                        List.of(
                                rol(
                                        "Lee",
                                        new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO)),
                                rol(
                                        "Escribe",
                                        new Permiso(
                                                "CARGAS", TipoDePermiso.ESCRITURA, Alcance.TODO))));

        assertThat(efectivos.incluye("CARGAS", TipoDePermiso.LECTURA)).isTrue();
        assertThat(efectivos.incluye("CARGAS", TipoDePermiso.ESCRITURA)).isTrue();
        assertThat(efectivos.incluye("CARGAS", TipoDePermiso.APROBACION)).isFalse();
    }

    @Test
    void dadoUnUsuarioSinRoles_cuandoSeUnen_entoncesNoTieneNada() {
        PermisosEfectivos efectivos = PermisosEfectivos.de(List.of());

        assertThat(efectivos.roles()).isEmpty();
        assertThat(efectivos.textos()).isEmpty();
    }
}
