package ec.mdd.docentes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** La cédula y el pasaporte del docente (research §3), sin dobles. */
class IdentificacionTest {

    @Test
    void dadaUnaCedulaValida_cuandoSeCrea_entoncesSeConservaConSuCeroInicial() {
        Identificacion cedula =
                new Identificacion(
                        TipoDeIdentificacion.CEDULA, " " + DocentesDePrueba.CEDULA + " ");

        assertThat(cedula.numero()).isEqualTo("0109990010");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "010999001", // 9 dígitos
                "01099900101", // 11 dígitos
                "010999001A", // con letras
                "0009990010", // provincia 00
                "2509990010", // provincia 25
                "0169990010", // tercer dígito 6: no es persona natural
                "0109990011" // el dígito verificador no cuadra
            })
    void dadaUnaCedulaInvalida_cuandoSeCrea_entoncesDatoInvalidoSinRepetirElNumero(String numero) {
        assertThatThrownBy(() -> new Identificacion(TipoDeIdentificacion.CEDULA, numero))
                .isInstanceOfSatisfying(
                        DatoInvalido.class,
                        error -> {
                            assertThat(error.campo()).isEqualTo("identificacion");
                            assertThat(error.getMessage()).doesNotContain(numero);
                        });
    }

    @Test
    void dadaUnaCedulaDelExterior_cuandoSeCrea_entoncesEsValida() {
        assertThat(new Identificacion(TipoDeIdentificacion.CEDULA, "3050000003").numero())
                .isEqualTo("3050000003");
    }

    @Test
    void dadoUnPasaporte_cuandoSeCrea_entoncesAdmiteLetrasYDigitosHasta13() {
        assertThat(new Identificacion(TipoDeIdentificacion.PASAPORTE, "AB1234567").numero())
                .isEqualTo("AB1234567");
        for (String numero : new String[] {"", "ABCDEFGHIJ1234", "AB-1234"}) {
            assertThatThrownBy(() -> new Identificacion(TipoDeIdentificacion.PASAPORTE, numero))
                    .isInstanceOf(DatoInvalido.class)
                    .extracting("campo")
                    .isEqualTo("identificacion");
        }
    }
}
