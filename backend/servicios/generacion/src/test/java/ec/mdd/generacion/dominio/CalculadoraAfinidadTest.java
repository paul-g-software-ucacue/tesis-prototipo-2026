package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El puntaje de afinidad y su desglose (RN-19, RN-24, RF-17). Los pesos no son los de por defecto,
 * para que un peso escrito en el código no pase la prueba (principio VI).
 */
class CalculadoraAfinidadTest {

    private static final String ESPECIFICO = "061";
    private static final String HERMANO = "062";
    private static final String AJENO = "091";

    private static final PesosAfinidad PESOS =
            new PesosAfinidad(
                    new BigDecimal("7"),
                    new BigDecimal("4"),
                    new BigDecimal("5"),
                    new BigDecimal("3"),
                    new BigDecimal("2.5"),
                    new BigDecimal("1.5"),
                    new BigDecimal("4"));

    private static final AsignaturaOfertada ASIGNATURA =
            new AsignaturaOfertada("POO-204", "Programación orientada a objetos", "06", ESPECIFICO);

    private static final TituloDeTercerNivel TERCER_NIVEL_AJENO =
            new TituloDeTercerNivel("Medicina", "09", AJENO);

    private final CalculadoraAfinidad calculadora = new CalculadoraAfinidad();

    @Test
    void dadoUnCuartoNivelEnElCampoEspecifico_cuandoSeCalcula_entoncesSumaSuPesoYLoNombra() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                List.of(elemento("Maestría en Software", ESPECIFICO)),
                                nada(),
                                nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("7");
        assertThat(puntaje.desglose())
                .singleElement()
                .satisfies(
                        aporte -> {
                            assertThat(aporte.tipo()).isEqualTo(TipoDeAporte.CUARTO_NIVEL);
                            assertThat(aporte.nombre()).isEqualTo("Maestría en Software");
                            assertThat(aporte.coincidencia())
                                    .isEqualTo(CoincidenciaDeCampo.CAMPO_ESPECIFICO);
                            assertThat(aporte.aporte()).isEqualByComparingTo("7");
                        });
    }

    @Test
    void dadoUnCuartoNivelSoloEnElCampoAmplio_cuandoSeCalcula_entoncesSumaMenos() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(List.of(elemento("Maestría en Redes", HERMANO)), nada(), nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("4");
        assertThat(puntaje.desglose().getFirst().coincidencia())
                .isEqualTo(CoincidenciaDeCampo.CAMPO_AMPLIO);
    }

    @Test
    void dadoUnTercerNivelEnElCampoEspecifico_cuandoSeCalcula_entoncesSumaSuPeso() {
        PuntajeAfinidad puntaje =
                calcular(
                        new FormacionDelDocente(
                                new TituloDeTercerNivel("Ingeniería", "06", ESPECIFICO),
                                nada(),
                                nada(),
                                nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("5");
        assertThat(puntaje.desglose().getFirst().tipo()).isEqualTo(TipoDeAporte.TERCER_NIVEL);
    }

    @Test
    void dadoUnTercerNivelSoloEnElCampoAmplio_cuandoSeCalcula_entoncesSumaMenos() {
        PuntajeAfinidad puntaje =
                calcular(
                        new FormacionDelDocente(
                                new TituloDeTercerNivel("Ingeniería", "06", HERMANO),
                                nada(),
                                nada(),
                                nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("3");
        assertThat(puntaje.desglose().getFirst().coincidencia())
                .isEqualTo(CoincidenciaDeCampo.CAMPO_AMPLIO);
    }

    @Test
    void dadoDosTitulosQueCoinciden_cuandoSeCalcula_entoncesSumanLosDos() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                List.of(
                                        elemento("Maestría en Software", ESPECIFICO),
                                        elemento("Doctorado en Redes", HERMANO)),
                                nada(),
                                nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("11");
        assertThat(puntaje.desglose()).hasSize(2);
    }

    @Test
    void dadaUnaExperienciaYUnCursoEnElCampoEspecifico_cuandoSeCalcula_entoncesCadaUnoSuma() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                nada(),
                                List.of(elemento("Desarrollador", ESPECIFICO)),
                                List.of(elemento("Curso de Java", ESPECIFICO))));

        assertThat(puntaje.total()).isEqualByComparingTo("4");
        assertThat(puntaje.desglose())
                .extracting(AporteDeAfinidad::tipo)
                .containsExactly(TipoDeAporte.EXPERIENCIA, TipoDeAporte.CURSO);
    }

    @Test
    void dadaUnaExperienciaYUnCursoSoloEnElCampoAmplio_cuandoSeCalcula_entoncesNoSuman() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                nada(),
                                List.of(elemento("Administrador de redes", HERMANO)),
                                List.of(elemento("Curso de redes", HERMANO))));

        assertThat(puntaje.total()).isEqualByComparingTo("0");
        assertThat(puntaje.desglose()).isEmpty();
    }

    @Test
    void dadosMasCursosQueElTope_cuandoSeCalcula_entoncesNoPasanDelTopeYLoDice() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                nada(),
                                nada(),
                                List.of(
                                        elemento("Curso 1", ESPECIFICO),
                                        elemento("Curso 2", ESPECIFICO),
                                        elemento("Curso 3", ESPECIFICO),
                                        elemento("Curso 4", ESPECIFICO))));

        assertThat(puntaje.total()).isEqualByComparingTo("4");
        assertThat(puntaje.topeAplicado()).isTrue();
        assertThat(puntaje.desglose())
                .extracting(AporteDeAfinidad::nombre)
                .containsExactly("Curso 1", "Curso 2", "Curso 3");
    }

    @Test
    void dadoElTopeQueCortaUnCurso_cuandoSeCalcula_entoncesEseCursoLlevaLoQueCabe() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                nada(),
                                nada(),
                                List.of(
                                        elemento("Curso 1", ESPECIFICO),
                                        elemento("Curso 2", ESPECIFICO),
                                        elemento("Curso 3", ESPECIFICO))));

        assertThat(puntaje.desglose().getLast().aporte()).isEqualByComparingTo("1");
        assertThat(puntaje.desglose()).allMatch(aporte -> aporte.aporte().signum() > 0);
        assertThat(puntaje.topeAplicado()).isTrue();
    }

    @Test
    void dadosCursosQueNoLleganAlTope_cuandoSeCalcula_entoncesNoSeDiceQueSeAplico() {
        PuntajeAfinidad puntaje =
                calcular(formacion(nada(), nada(), List.of(elemento("Curso 1", ESPECIFICO))));

        assertThat(puntaje.topeAplicado()).isFalse();
    }

    @Test
    void dadosCursosQueSumanJustoElTope_cuandoSeCalcula_entoncesNoSeDiceQueSeAplico() {
        PesosAfinidad tope = conTope(new BigDecimal("3"));

        PuntajeAfinidad puntaje =
                calculadora.calcular(
                        formacion(
                                nada(),
                                nada(),
                                List.of(
                                        elemento("Curso 1", ESPECIFICO),
                                        elemento("Curso 2", ESPECIFICO))),
                        ASIGNATURA,
                        tope);

        assertThat(puntaje.total()).isEqualByComparingTo("3");
        assertThat(puntaje.topeAplicado()).isFalse();
    }

    @Test
    void dadosCursosAjenosAlTope_cuandoSeCalcula_entoncesNoConsumenElTope() {
        PuntajeAfinidad puntaje =
                calcular(
                        formacion(
                                nada(),
                                nada(),
                                List.of(
                                        elemento("Curso ajeno", AJENO),
                                        elemento("Curso ajeno 2", AJENO),
                                        elemento("Curso 1", ESPECIFICO))));

        assertThat(puntaje.total()).isEqualByComparingTo("1.5");
        assertThat(puntaje.topeAplicado()).isFalse();
    }

    @Test
    void dadoUnPesoCero_cuandoSeCalcula_entoncesNoAparecenAportesEnCero() {
        PesosAfinidad sinExperiencia =
                new PesosAfinidad(
                        PESOS.cuartoNivelCampoEspecifico(),
                        PESOS.cuartoNivelCampoAmplio(),
                        PESOS.tercerNivelCampoEspecifico(),
                        PESOS.tercerNivelCampoAmplio(),
                        BigDecimal.ZERO,
                        PESOS.cursoRelacionado(),
                        PESOS.topeCursos());

        PuntajeAfinidad puntaje =
                calculadora.calcular(
                        formacion(nada(), List.of(elemento("Desarrollador", ESPECIFICO)), nada()),
                        ASIGNATURA,
                        sinExperiencia);

        assertThat(puntaje.desglose()).isEmpty();
    }

    @Test
    void dadaUnaFormacionQueNoCoincide_cuandoSeCalcula_entoncesCeroYSinDesglose() {
        PuntajeAfinidad puntaje = calcular(formacion(nada(), nada(), nada()));

        assertThat(puntaje.total()).isEqualByComparingTo("0");
        assertThat(puntaje.desglose()).isEmpty();
        assertThat(puntaje.topeAplicado()).isFalse();
    }

    @Test
    void dadosPesosConDecimales_cuandoSeCalcula_entoncesLaSumaEsExacta() {
        PesosAfinidad decimales =
                new PesosAfinidad(
                        new BigDecimal("0.10"),
                        new BigDecimal("0.05"),
                        new BigDecimal("0.20"),
                        new BigDecimal("0.10"),
                        new BigDecimal("0.10"),
                        new BigDecimal("0.10"),
                        new BigDecimal("0.15"));

        PuntajeAfinidad puntaje =
                calculadora.calcular(
                        new FormacionDelDocente(
                                new TituloDeTercerNivel("Ingeniería", "06", ESPECIFICO),
                                List.of(elemento("Maestría", ESPECIFICO)),
                                List.of(elemento("Desarrollador", ESPECIFICO)),
                                List.of(
                                        elemento("Curso 1", ESPECIFICO),
                                        elemento("Curso 2", ESPECIFICO))),
                        ASIGNATURA,
                        decimales);

        // Con double, 0,1 + 0,2 no da 0,3.
        assertThat(puntaje.total()).isEqualByComparingTo("0.55");
        assertThat(puntaje.desglose().getLast().aporte()).isEqualByComparingTo("0.05");
        assertThat(puntaje.topeAplicado()).isTrue();
    }

    @Test
    void dadaLaFormacionCompleta_cuandoSeCalcula_entoncesElDesgloseVaEnElOrdenDeLaFicha() {
        PuntajeAfinidad puntaje =
                calcular(
                        new FormacionDelDocente(
                                new TituloDeTercerNivel("Ingeniería", "06", ESPECIFICO),
                                List.of(elemento("Maestría", ESPECIFICO)),
                                List.of(elemento("Desarrollador", ESPECIFICO)),
                                List.of(elemento("Curso", ESPECIFICO))));

        assertThat(puntaje.total()).isEqualByComparingTo("16");
        assertThat(puntaje.desglose())
                .extracting(AporteDeAfinidad::tipo)
                .containsExactly(
                        TipoDeAporte.TERCER_NIVEL,
                        TipoDeAporte.CUARTO_NIVEL,
                        TipoDeAporte.EXPERIENCIA,
                        TipoDeAporte.CURSO);
    }

    @Test
    void dadasVariasAsignaturas_cuandoSeCalcula_entoncesDelMayorAlMenorYPorNombreAlEmpatar() {
        List<PuntajeAfinidad> puntajes =
                calculadora.calcular(
                        formacion(List.of(elemento("Maestría", ESPECIFICO)), nada(), nada()),
                        List.of(
                                new AsignaturaOfertada("Z-1", "Zoología", "09", AJENO),
                                new AsignaturaOfertada("A-1", "Álgebra", "09", AJENO),
                                ASIGNATURA),
                        PESOS);

        assertThat(puntajes)
                .extracting(PuntajeAfinidad::codigo)
                .containsExactly("POO-204", "A-1", "Z-1");
    }

    @Test
    void dadoUnAporteEnCero_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new AporteDeAfinidad(
                                        TipoDeAporte.CURSO,
                                        "Curso",
                                        CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                                        BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PuntajeAfinidad calcular(FormacionDelDocente formacion) {
        return calculadora.calcular(formacion, ASIGNATURA, PESOS);
    }

    private static FormacionDelDocente formacion(
            List<ElementoDeFormacion> cuartoNivel,
            List<ElementoDeFormacion> experiencia,
            List<ElementoDeFormacion> cursos) {
        return new FormacionDelDocente(TERCER_NIVEL_AJENO, cuartoNivel, experiencia, cursos);
    }

    private static ElementoDeFormacion elemento(String nombre, String campoEspecifico) {
        return new ElementoDeFormacion(nombre, campoEspecifico);
    }

    private static List<ElementoDeFormacion> nada() {
        return List.of();
    }

    private static PesosAfinidad conTope(BigDecimal tope) {
        return new PesosAfinidad(
                PESOS.cuartoNivelCampoEspecifico(),
                PESOS.cuartoNivelCampoAmplio(),
                PESOS.tercerNivelCampoEspecifico(),
                PESOS.tercerNivelCampoAmplio(),
                PESOS.experienciaRelacionada(),
                PESOS.cursoRelacionado(),
                tope);
    }
}
