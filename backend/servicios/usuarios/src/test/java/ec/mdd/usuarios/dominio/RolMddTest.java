package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RolMddTest {

    @Test
    void dadoLaMatrizDeRoles_cuandoSeListan_entoncesSonLosDiezDeD16() {
        assertThat(Arrays.stream(RolMdd.values()).map(Enum::name))
                .containsExactlyInAnyOrder(
                        "ADMINISTRADOR",
                        "OPERADOR_DATOS",
                        "COORDINADOR_CARRERA",
                        "GESTOR_UNIDAD",
                        "RESPONSABLE_CONSOLIDACION",
                        "VICERRECTOR",
                        "RECTOR",
                        "MIEMBRO_OCS",
                        "SECRETARIA_GENERAL",
                        "DOCENTE");
    }

    @Test
    void dadoElIdentificadorDeUnRol_cuandoSeConvierte_entoncesDevuelveElRol() {
        assertThat(RolMdd.desde("COORDINADOR_CARRERA")).isEqualTo(RolMdd.COORDINADOR_CARRERA);
    }

    @Test
    void dadoUnTextoQueNoEsUnRol_cuandoSeConvierte_entoncesLoRechazaYDiceCual() {
        assertThatThrownBy(() -> RolMdd.desde("SUPERUSUARIO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERUSUARIO");
        assertThatThrownBy(() -> RolMdd.desde(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
