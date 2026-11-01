package ec.mdd.plataforma.errores.nucleo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Quién ve todas las carreras y quién solo la suya (D-64, RF-03). */
class AlcanceDeCarreraTest {

    private static final String CARRERA = "CARRERA_DS-01";
    private static final String ESCRITURA = "OFERTA:ESCRITURA";
    private static final List<String> ESCRIBE_LO_PROPIO_CON_CARRERA =
            List.of("PERMISO_OFERTA:ESCRITURA", "ALCANCE_PROPIO_OFERTA:ESCRITURA", CARRERA);

    @Test
    void dadoUnPermisoDeLaOfertaSinAlcancePropio_cuandoSePregunta_entoncesVeTodas() {
        assertThat(AlcanceDeCarrera.propia(List.of("PERMISO_DISTRIBUTIVO:LECTURA"))).isEmpty();
    }

    @Test
    void dadosLosPermisosConAlcancePropioYCarrera_cuandoSePregunta_entoncesSoloEsa() {
        assertThat(
                        AlcanceDeCarrera.propia(
                                List.of(
                                        "PERMISO_OFERTA:LECTURA",
                                        "ALCANCE_PROPIO_OFERTA:LECTURA",
                                        "PERMISO_DISTRIBUTIVO:LECTURA",
                                        "ALCANCE_PROPIO_DISTRIBUTIVO:LECTURA",
                                        CARRERA)))
                .contains("DS-01");
    }

    @Test
    void dadoAlcancePropioSinCarrera_cuandoSePregunta_entoncesCarreraSinIndicar() {
        assertThatThrownBy(
                        () ->
                                AlcanceDeCarrera.propia(
                                        List.of(
                                                "PERMISO_OFERTA:LECTURA",
                                                "ALCANCE_PROPIO_OFERTA:LECTURA")))
                .isInstanceOf(CarreraSinIndicar.class);
    }

    @Test
    void dadoUnPermisoPropioYOtroConAlcanceTotal_cuandoSePregunta_entoncesVeTodas() {
        assertThat(
                        AlcanceDeCarrera.propia(
                                List.of(
                                        "PERMISO_OFERTA:LECTURA",
                                        "ALCANCE_PROPIO_OFERTA:LECTURA",
                                        "PERMISO_CATALOGOS:LECTURA",
                                        CARRERA)))
                .isEmpty();
    }

    @Test
    void dadoQuienNoTienePermisosDeLaOferta_cuandoSePregunta_entoncesVeTodas() {
        assertThat(AlcanceDeCarrera.propia(List.of("PERMISO_DOCENTES:LECTURA"))).isEmpty();
    }

    @Test
    void dadoDocentesConAlcanceTotalYLaOfertaPropia_cuandoSePregunta_entoncesNoAbreTodo() {
        assertThat(
                        AlcanceDeCarrera.propia(
                                List.of(
                                        "PERMISO_DOCENTES:LECTURA",
                                        "PERMISO_OFERTA:LECTURA",
                                        "ALCANCE_PROPIO_OFERTA:LECTURA",
                                        CARRERA)))
                .contains("DS-01");
    }

    @Test
    void dadoQueNoTieneAutoridades_cuandoSePregunta_entoncesVeTodas() {
        assertThat(AlcanceDeCarrera.propia(List.of())).isEmpty();
    }

    @Test
    void dadoUnPermisoSobreTodo_cuandoSePreguntaPorEl_entoncesVeTodas() {
        assertThat(AlcanceDeCarrera.propia(List.of("PERMISO_OFERTA:ESCRITURA"), ESCRITURA))
                .isEmpty();
    }

    @Test
    void dadoUnPermisoPropioConCarrera_cuandoSePreguntaPorEl_entoncesSoloEsa() {
        assertThat(AlcanceDeCarrera.propia(ESCRIBE_LO_PROPIO_CON_CARRERA, ESCRITURA))
                .contains("DS-01");
    }

    @Test
    void dadoUnRolSinLecturaConEscrituraPropia_cuandoSePreguntaPorLaLectura_entoncesVeTodas() {
        assertThat(AlcanceDeCarrera.propia(ESCRIBE_LO_PROPIO_CON_CARRERA)).isEmpty();
    }

    @Test
    void dadoUnPermisoPropioSinCarrera_cuandoSePreguntaPorEl_entoncesCarreraSinIndicar() {
        assertThatThrownBy(
                        () ->
                                AlcanceDeCarrera.propia(
                                        List.of(
                                                "PERMISO_OFERTA:ESCRITURA",
                                                "ALCANCE_PROPIO_OFERTA:ESCRITURA"),
                                        ESCRITURA))
                .isInstanceOf(CarreraSinIndicar.class);
    }

    @Test
    void dadoQueNoTieneElPermiso_cuandoSePreguntaPorEl_entoncesVacio() {
        assertThat(AlcanceDeCarrera.propia(List.of("PERMISO_OFERTA:LECTURA"), ESCRITURA)).isEmpty();
    }
}
