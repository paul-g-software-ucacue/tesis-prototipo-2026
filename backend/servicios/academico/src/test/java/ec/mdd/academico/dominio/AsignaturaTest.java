package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.CarreraTest.invalido;
import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.asignatura;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Una asignatura y sus reglas (RF-14, carga 4, research §3), sin dobles. */
class AsignaturaTest {

    private static final CampoConocimiento MATEMATICAS = new CampoConocimiento("05", "054");

    @Test
    void dadosDatosValidos_cuandoSeRegistra_entoncesQuedaActivaConSusPalabrasLimpias() {
        Asignatura nueva =
                nueva(new BigDecimal("2.5"), List.of(" matemática", "lógica ", "conjuntos"));

        assertThat(nueva.activa()).isTrue();
        assertThat(nueva.palabrasClave()).containsExactly("matemática", "lógica", "conjuntos");
        assertThat(nueva.horasSemanalesClase()).isEqualByComparingTo("2.5");
    }

    @Test
    void dadaUnaAsignaturaSinCarreraOSinHoras_cuandoSeRegistra_entoncesDiceQueFalta() {
        invalido(() -> asignatura("A1", null, "DS-2019", 1), "carrera");
        invalido(() -> nueva(null, List.of("a", "b")), "horasSemanalesClase");
    }

    @Test
    void dadasHorasFueraDeRango_cuandoSeRegistra_entoncesDatoInvalidoEnSuCampo() {
        invalido(() -> nueva(new BigDecimal("0.4"), List.of("a", "b")), "horasSemanalesClase");
        invalido(() -> nueva(new BigDecimal("20.5"), List.of("a", "b")), "horasSemanalesClase");
        invalido(() -> nueva(new BigDecimal("2.555"), List.of("a", "b")), "horasSemanalesClase");
        invalido(
                () ->
                        Asignatura.nueva(
                                "A1",
                                "C1",
                                "M1",
                                "N",
                                1,
                                UnidadDeOrganizacionCurricular.BASICA,
                                BigDecimal.ONE,
                                new BigDecimal("21"),
                                null,
                                false,
                                MATEMATICAS,
                                List.of("a", "b")),
                "horasPracticasSemanales");
        invalido(
                () ->
                        Asignatura.nueva(
                                "A1",
                                "C1",
                                "M1",
                                "N",
                                1,
                                UnidadDeOrganizacionCurricular.BASICA,
                                BigDecimal.ONE,
                                null,
                                BigDecimal.ZERO,
                                false,
                                MATEMATICAS,
                                List.of("a", "b")),
                "creditos");
    }

    @Test
    void dadasPalabrasClaveInsuficientesRepetidasOVacias_cuandoSeRegistra_entoncesDatoInvalido() {
        invalido(() -> nueva(BigDecimal.ONE, List.of("programación")), "palabrasClave");
        invalido(() -> nueva(BigDecimal.ONE, List.of("java", "Java")), "palabrasClave");
        invalido(() -> nueva(BigDecimal.ONE, List.of("java", " ", "objetos")), "palabrasClave");
    }

    @Test
    void dadoUnCodigoOUnCicloConOtraForma_cuandoSeRegistra_entoncesSuCampo() {
        invalido(() -> asignatura("ds-poo", "C1", "M1", 1), "codigo");
        invalido(() -> asignatura("X".repeat(31), "C1", "M1", 1), "codigo");
        invalido(() -> asignatura("A1", "C1", "M1", 0), "ciclo");
        invalido(() -> asignatura("A1", "C1", "M1", 11), "ciclo");
        invalido(() -> asignatura("A1", "C1", null, 1), "malla");
    }

    @Test
    void dadaUnaAsignatura_cuandoSeCorrige_entoncesNoCambiaCodigoCarreraNiMalla() {
        Asignatura actual = asignatura("A1", "C1", "M1", 1);

        Asignatura.Correccion correccion =
                actual.corregida(
                        "MATEMÁTICA",
                        2,
                        UnidadDeOrganizacionCurricular.BASICA,
                        new BigDecimal("4"),
                        BigDecimal.ZERO,
                        null,
                        true,
                        MATEMATICAS,
                        List.of("matemática", "lógica", "conjuntos"));

        assertThat(correccion.asignatura().codigo()).isEqualTo("A1");
        assertThat(correccion.asignatura().carrera()).isEqualTo("C1");
        assertThat(correccion.asignatura().malla()).isEqualTo("M1");
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor("nombre", "MATEMÁTICA DISCRETA", "MATEMÁTICA"),
                        new CambioDeValor("ciclo", "1", "2"),
                        new CambioDeValor("horasSemanalesClase", "3", "4"));
        assertThat(actual.conEstado(false).activa()).isFalse();
    }

    private static Asignatura nueva(BigDecimal horas, List<String> palabras) {
        return Asignatura.nueva(
                "A1",
                "C1",
                "M1",
                "MATEMÁTICA DISCRETA",
                1,
                UnidadDeOrganizacionCurricular.BASICA,
                horas,
                null,
                null,
                false,
                MATEMATICAS,
                palabras);
    }
}
