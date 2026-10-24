package ec.mdd.docentes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Las reglas de la ficha académica (RF-17, research §3), sin dobles. */
class FichaAcademicaTest {

    private static final TituloTercerNivel TERCERO =
            new TituloTercerNivel(
                    "INGENIERO DE SISTEMAS", null, new CampoConocimiento("06", "061"));

    @Test
    void dadaLaFichaDePrueba_cuandoSeCrea_entoncesConservaSusPartesYSusCamposAmplios() {
        FichaAcademica ficha = DocentesDePrueba.ficha();

        assertThat(ficha.cuartoNivel())
                .singleElement()
                .extracting(TituloCuartoNivel::campoAmplio)
                .isEqualTo("06");
        assertThat(ficha.cursos()).extracting(Curso::horas).containsExactly(40, 60);
        assertThat(ficha.camposAmplios()).containsExactly("01", "06");
    }

    @Test
    void dadoUnTituloDeTercerNivelInvalido_cuandoSeCrea_entoncesDatoInvalidoConSuCampo() {
        invalida(() -> ficha(null), "ficha.tercerNivel");
        invalida(
                () -> ficha(new TituloTercerNivel(" ", null, new CampoConocimiento("06", "061"))),
                "ficha.tercerNivel.titulo");
        invalida(
                () ->
                        ficha(
                                new TituloTercerNivel(
                                        "T".repeat(151), null, new CampoConocimiento("06", "061"))),
                "ficha.tercerNivel.titulo");
        invalida(
                () ->
                        ficha(
                                new TituloTercerNivel(
                                        "T", "R".repeat(31), new CampoConocimiento("06", "061"))),
                "ficha.tercerNivel.registroSenescyt");
        invalida(
                () -> ficha(new TituloTercerNivel("T", null, new CampoConocimiento("061", "061"))),
                "ficha.tercerNivel.campoAmplio");
        invalida(
                () -> ficha(new TituloTercerNivel("T", null, new CampoConocimiento("06", "071"))),
                "ficha.tercerNivel.campoEspecifico");
    }

    @Test
    void dadosElementosInvalidos_cuandoSeCrea_entoncesElCampoDiceSuPosicion() {
        invalida(
                () ->
                        new FichaAcademica(
                                TERCERO,
                                List.of(
                                        new TituloCuartoNivel(
                                                "M",
                                                TipoDeTituloDeCuartoNivel.MAESTRIA,
                                                "061",
                                                1949)),
                                null,
                                null,
                                null),
                "ficha.cuartoNivel[0].anio");
        invalida(() -> conCursos(new Curso("C", "061", 0, 2020)), "ficha.cursos[1].horas");
        invalida(() -> conCursos(new Curso("C", "061", 2001, 2020)), "ficha.cursos[1].horas");
        invalida(
                () -> conCursos(new Curso("C", "06", 40, 2020)), "ficha.cursos[1].campoEspecifico");
        invalida(
                () ->
                        new FichaAcademica(
                                TERCERO,
                                null,
                                null,
                                List.of(new ExperienciaLaboral("D", "E", "061", BigDecimal.ZERO)),
                                null),
                "ficha.experiencia[0].anios");
        invalida(
                () ->
                        new FichaAcademica(
                                TERCERO,
                                null,
                                null,
                                List.of(
                                        new ExperienciaLaboral(
                                                "D", "E", "061", new BigDecimal("61"))),
                                null),
                "ficha.experiencia[0].anios");
    }

    @Test
    void dadasListasLargasOAfinesRepetidas_cuandoSeCrea_entoncesDatoInvalido() {
        List<Curso> veintiuno =
                new ArrayList<>(Collections.nCopies(21, new Curso("C", "061", 40, 2020)));
        invalida(() -> new FichaAcademica(TERCERO, null, veintiuno, null, null), "ficha.cursos");
        invalida(
                () -> new FichaAcademica(TERCERO, null, null, null, List.of("A1", "A2", "A1")),
                "ficha.asignaturasAfines[2]");
        invalida(
                () -> new FichaAcademica(TERCERO, null, null, null, List.of("a1")),
                "ficha.asignaturasAfines[0]");
    }

    @Test
    void dadoUnAnioPosteriorAlActual_cuandoSeVerifica_entoncesDatoInvalidoConSuPosicion() {
        FichaAcademica ficha = conCursos(new Curso("C", "061", 40, 2027));

        assertThatThrownBy(() -> ficha.verificarAnios(Year.of(2026)))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("ficha.cursos[1].anio");
        ficha.verificarAnios(Year.of(2027));
    }

    @Test
    void dadasListasNulas_cuandoSeCrea_entoncesQuedanVacias() {
        FichaAcademica ficha = new FichaAcademica(TERCERO, null, null, null, null);

        assertThat(ficha.cuartoNivel()).isEmpty();
        assertThat(ficha.cursos()).isEmpty();
        assertThat(ficha.experiencia()).isEmpty();
        assertThat(ficha.asignaturasAfines()).isEmpty();
    }

    private static FichaAcademica ficha(TituloTercerNivel tercero) {
        return new FichaAcademica(tercero, null, null, null, null);
    }

    private static FichaAcademica conCursos(Curso segundo) {
        return new FichaAcademica(
                TERCERO, null, List.of(new Curso("A", "061", 40, 2020), segundo), null, null);
    }

    private static void invalida(Supplier<FichaAcademica> crear, String campo) {
        assertThatThrownBy(crear::get)
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo(campo);
    }
}
