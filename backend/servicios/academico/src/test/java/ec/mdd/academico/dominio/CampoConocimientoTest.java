package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** El campo de conocimiento CINE de una carrera o una asignatura (glosario, RN-24), sin dobles. */
class CampoConocimientoTest {

    @Test
    void dadoUnEspecificoQueEmpiezaPorElAmplio_cuandoSeCrea_entoncesSeAcepta() {
        assertThat(new CampoConocimiento("06", "061").especifico()).isEqualTo("061");
    }

    @Test
    void dadoUnAmplioOUnEspecificoConOtraForma_cuandoSeCrea_entoncesDatoInvalido() {
        assertThatThrownBy(() -> new CampoConocimiento("6", "061"))
                .isInstanceOfSatisfying(
                        DatoInvalido.class, e -> assertThat(e.campo()).isEqualTo("campoAmplio"));
        assertThatThrownBy(() -> new CampoConocimiento("06", "61"))
                .isInstanceOfSatisfying(
                        DatoInvalido.class,
                        e -> assertThat(e.campo()).isEqualTo("campoEspecifico"));
        assertThatThrownBy(() -> new CampoConocimiento("06", "071"))
                .isInstanceOfSatisfying(
                        DatoInvalido.class,
                        e ->
                                assertThat(e.motivo())
                                        .isEqualTo(
                                                "El campo específico tiene que empezar por el campo amplio"));
    }
}
