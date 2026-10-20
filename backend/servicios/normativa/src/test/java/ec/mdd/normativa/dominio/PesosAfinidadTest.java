package ec.mdd.normativa.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Los pesos de afinidad de RN-24 y su invariante (research §4 de F04-H03), sin dobles. */
class PesosAfinidadTest {

    static Map<String, BigDecimal> pesos(
            int cuartoEspecifico, int cuartoAmplio, int tercerEspecifico, int tercerAmplio) {
        Map<String, BigDecimal> pesos = new LinkedHashMap<>();
        pesos.put("cuarto_nivel_campo_especifico", BigDecimal.valueOf(cuartoEspecifico));
        pesos.put("cuarto_nivel_campo_amplio", BigDecimal.valueOf(cuartoAmplio));
        pesos.put("tercer_nivel_campo_especifico", BigDecimal.valueOf(tercerEspecifico));
        pesos.put("tercer_nivel_campo_amplio", BigDecimal.valueOf(tercerAmplio));
        pesos.put("experiencia_relacionada", BigDecimal.valueOf(2));
        pesos.put("curso_relacionado", BigDecimal.valueOf(1));
        pesos.put("tope_cursos", BigDecimal.valueOf(3));
        return pesos;
    }

    private static Map<String, BigDecimal> conTercerAmplio(String valor) {
        Map<String, BigDecimal> pesos = pesos(3, 2, 2, 1);
        pesos.put("tercer_nivel_campo_amplio", new BigDecimal(valor));
        return pesos;
    }

    @Test
    void dadoLosParametrosDeRn24_cuandoSeArman_entoncesTienenCadaPeso() {
        PesosAfinidad pesos = PesosAfinidad.de(new Regla("RN-24", null, pesos(3, 2, 2, 1)));

        assertThat(pesos.cuartoNivelCampoEspecifico()).isEqualByComparingTo("3");
        assertThat(pesos.cuartoNivelCampoAmplio()).isEqualByComparingTo("2");
        assertThat(pesos.tercerNivelCampoEspecifico()).isEqualByComparingTo("2");
        assertThat(pesos.tercerNivelCampoAmplio()).isEqualByComparingTo("1");
        assertThat(pesos.experienciaRelacionada()).isEqualByComparingTo("2");
        assertThat(pesos.cursoRelacionado()).isEqualByComparingTo("1");
        assertThat(pesos.topeCursos()).isEqualByComparingTo("3");
    }

    @Test
    void dadoElCampoAmplioIgualAlEspecifico_cuandoSeArman_entoncesEsValido() {
        assertThat(PesosAfinidad.de(new Regla("RN-24", null, pesos(3, 3, 2, 2)))).isNotNull();
    }

    @Test
    void dadoElCuartoNivelAmplioMayorQueElEspecifico_cuandoSeArman_entoncesSeRechaza() {
        assertThatThrownBy(() -> PesosAfinidad.de(new Regla("RN-24", null, pesos(3, 4, 2, 1))))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error -> {
                            assertThat(error.campo())
                                    .isEqualTo("reglas[RN-24].cuarto_nivel_campo_amplio");
                            assertThat(error.motivo())
                                    .isEqualTo(
                                            "El peso del campo amplio (4) no puede superar al del"
                                                    + " campo específico (3)");
                        });
    }

    @Test
    void dadoElTercerNivelAmplioMayorQueElEspecifico_cuandoSeArman_entoncesSeRechaza() {
        assertThatThrownBy(() -> PesosAfinidad.de(new Regla("RN-24", null, conTercerAmplio("2.5"))))
                .isInstanceOfSatisfying(
                        CambioNoPermitido.class,
                        error ->
                                assertThat(error.campo())
                                        .isEqualTo("reglas[RN-24].tercer_nivel_campo_amplio"));
    }
}
