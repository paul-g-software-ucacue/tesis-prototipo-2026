package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Un rol: nombre, descripción, al menos un permiso e identificador derivado del nombre (FR-002).
 */
class RolTest {

    private static final Permiso LEER_CARGAS =
            new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO);
    private static final Permiso LEER_DOCENTES =
            new Permiso("DOCENTES", TipoDePermiso.LECTURA, Alcance.TODO);

    @Test
    void dadoUnNombre_cuandoSeCreaElRol_entoncesSuIdentificadorSeDerivaDelNombre() {
        Rol rol = Rol.nuevo("Auditor de cargas", "Revisa cargas.", List.of(LEER_CARGAS));

        assertThat(rol.identificador()).isEqualTo("AUDITOR_DE_CARGAS");
        assertThat(rol.predefinido()).isFalse();
    }

    @Test
    void dadoNombresConTildesSignosYEspacios_cuandoSeDerivaElIdentificador_entoncesQuedaLimpio() {
        assertThat(Rol.identificadorDe("Gestión  de  cargas!")).isEqualTo("GESTION_DE_CARGAS");
        assertThat(Rol.identificadorDe("  ¿Año de Ñandú?  ")).isEqualTo("ANO_DE_NANDU");
        assertThat(Rol.identificadorDe("Auditor de Cargas."))
                .isEqualTo(Rol.identificadorDe("Auditor de cargas"));
    }

    @Test
    void dadoUnNombreDe80Caracteres_cuandoSeDerivaElIdentificador_entoncesTiene64ComoMaximo() {
        String nombre = "Rol " + "a".repeat(58) + " de prueba largo x";

        assertThat(nombre).hasSize(80);
        assertThat(Rol.identificadorDe(nombre)).hasSizeLessThanOrEqualTo(64).doesNotEndWith("_");
        assertThat(Rol.identificadorDe("Rol " + "a".repeat(59) + " b"))
                .isEqualTo("ROL_" + "A".repeat(59))
                .hasSize(63);
    }

    @Test
    void dadoUnRol_cuandoSeEdita_entoncesConservaSuIdentificador() {
        Rol rol = Rol.nuevo("Auditor de cargas", "Revisa cargas.", List.of(LEER_CARGAS));

        Rol editado = rol.editado("Revisor", "Revisa docentes.", List.of(LEER_DOCENTES));

        assertThat(editado.identificador()).isEqualTo("AUDITOR_DE_CARGAS");
        assertThat(editado.nombre()).isEqualTo("Revisor");
        assertThat(editado.permisos()).containsExactly(LEER_DOCENTES);
    }

    @Test
    void dadoPermisosDesordenados_cuandoSeCreaElRol_entoncesQuedanPorFuncionalidadYTipo() {
        Permiso escribirCargas = new Permiso("CARGAS", TipoDePermiso.ESCRITURA, Alcance.TODO);

        Rol rol =
                Rol.nuevo(
                        "Operador", "Opera.", List.of(LEER_DOCENTES, escribirCargas, LEER_CARGAS));

        assertThat(rol.permisos()).containsExactly(LEER_CARGAS, escribirCargas, LEER_DOCENTES);
    }

    @Test
    void dadoUnRolSinPermisos_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> Rol.nuevo("Vacío", "Sin nada.", List.of()))
                .isInstanceOf(PermisosInvalidos.class)
                .hasMessage("El rol necesita al menos un permiso.");
    }

    @Test
    void dadoNombresYDescripcionesFueraDeLosLimites_cuandoSeCreaElRol_entoncesSeRechazan() {
        List<Permiso> permisos = List.of(LEER_CARGAS);

        assertThatThrownBy(() -> Rol.nuevo("a".repeat(81), "d", permisos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rol.nuevo("¡¿!?", "d", permisos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rol.nuevo("  ", "d", permisos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rol.nuevo("Rol", "d".repeat(301), permisos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rol.nuevo("Rol", " ", permisos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Rol.nuevo(" Rol con espacios ", " Hasta 300. ", permisos))
                .extracting(Rol::nombre, Rol::descripcion)
                .containsExactly("Rol con espacios", "Hasta 300.");
    }

    @Test
    void dadoUnRol_cuandoSePreguntaPorUnPermiso_entoncesLoDaConCualquierAlcance() {
        Rol rol =
                Rol.nuevo(
                        "Propio",
                        "Solo lo propio.",
                        List.of(new Permiso("OFERTA", TipoDePermiso.LECTURA, Alcance.PROPIO)));

        assertThat(rol.da("OFERTA", TipoDePermiso.LECTURA)).isTrue();
        assertThat(rol.da("OFERTA", TipoDePermiso.ESCRITURA)).isFalse();
    }
}
