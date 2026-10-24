package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.DatosMaestrosDePrueba.carrera;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** Una carrera y sus reglas (RF-12, carga 2, research §3), sin dobles. */
class CarreraTest {

    private static final CampoConocimiento TIC = new CampoConocimiento("06", "061");

    @Test
    void dadosDatosValidos_cuandoSeRegistra_entoncesQuedaVigenteSinEspacios() {
        Carrera nueva =
                Carrera.nueva(
                        "550613A01-P-0301",
                        "  DESARROLLO DE SOFTWARE ",
                        "DS",
                        "MATRIZ",
                        NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                        Modalidad.PRESENCIAL,
                        List.of(Jornada.NOCTURNA, Jornada.MATUTINA),
                        TIC,
                        null,
                        5);

        assertThat(nueva.estado()).isEqualTo(EstadoDeCarrera.VIGENTE);
        assertThat(nueva.nombre()).isEqualTo("DESARROLLO DE SOFTWARE");
        assertThat(nueva.jornadas()).containsExactly(Jornada.NOCTURNA, Jornada.MATUTINA);
        assertThat(nueva.campoDetallado()).isNull();
    }

    @Test
    void dadoUnCodigoUnNombreOUnasSiglasConOtraForma_cuandoSeRegistra_entoncesSuCampo() {
        invalido(() -> carrera("550613a01-p", "DS"), "codigo");
        invalido(() -> carrera("X".repeat(21), "DS"), "codigo");
        invalido(() -> carrera("C1", "ds"), "siglas");
        invalido(() -> carrera("C1", "ABCDEFGHIJK"), "siglas");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                " ",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                null,
                                5),
                "nombre");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N".repeat(151),
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                null,
                                5),
                "nombre");
    }

    @Test
    void dadasJornadasVaciasORepetidas_cuandoSeRegistra_entoncesDatoInvalidoEnJornadas() {
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(),
                                TIC,
                                null,
                                5),
                "jornadas");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA, Jornada.MATUTINA),
                                TIC,
                                null,
                                5),
                "jornadas");
    }

    @Test
    void dadoUnDetalladoQueNoEmpiezaPorElEspecificoOCiclosFueraDeRango_entoncesSuCampo() {
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                "0713",
                                5),
                "campoDetallado");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                null,
                                0),
                "numeroCiclos");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                null,
                                11),
                "numeroCiclos");
        invalido(
                () ->
                        Carrera.nueva(
                                "C1",
                                "N",
                                "DS",
                                null,
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.MATUTINA),
                                TIC,
                                null,
                                5),
                "sede");
    }

    @Test
    void dadaUnaCarrera_cuandoSeCorrige_entoncesCambiaSusDatosYNoSuCodigo() {
        Carrera actual = carrera("C1", "DS");

        Carrera.Correccion correccion =
                actual.corregida(
                        "SOFTWARE",
                        "SW",
                        "MATRIZ",
                        NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                        Modalidad.PRESENCIAL,
                        List.of(Jornada.NOCTURNA),
                        TIC,
                        "0613",
                        6);

        assertThat(correccion.carrera().codigo()).isEqualTo("C1");
        assertThat(correccion.cambios())
                .containsExactly(
                        new CambioDeValor("nombre", "DESARROLLO DE SOFTWARE", "SOFTWARE"),
                        new CambioDeValor("siglas", "DS", "SW"),
                        new CambioDeValor("numeroCiclos", "5", "6"));
        assertThat(actual.conEstado(EstadoDeCarrera.EN_CIERRE).estado())
                .isEqualTo(EstadoDeCarrera.EN_CIERRE);
    }

    static void invalido(ThrowingCallable accion, String campo) {
        assertThatThrownBy(accion)
                .isInstanceOfSatisfying(
                        DatoInvalido.class, error -> assertThat(error.campo()).isEqualTo(campo));
    }
}
