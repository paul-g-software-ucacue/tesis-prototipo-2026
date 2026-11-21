package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El reporte individual del docente (F20-H01, RF-45, D-115). */
class ReporteIndividualTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    false);
    private static final DatosDelDocente ANA =
            new DatosDelDocente("0109990010", "PEREZ ANA", Dedicacion.TC, 40);

    private static final FilaDeAsignatura BD_A =
            new FilaDeAsignatura("Software", "BD-301", "Bases de datos", "A", horas("8"));
    private static final FilaDeAsignatura SOC_B =
            new FilaDeAsignatura("Agronomía", "SOC-101", "Sociología", "B", horas("3.25"));
    private static final FilaDeActividad PROYECTO =
            new FilaDeActividad(
                    Componente.INVESTIGACION, "141-a", "Proyecto", "Suelos andinos", horas("3"));
    private static final FilaDeActividad TUTORIAS =
            new FilaDeActividad(Componente.OTRAS_DOCENCIA, "140-b", "Tutorías", null, horas("2.5"));

    @Test
    void dadoElContenidoDeLaVersionPublicada_cuandoSeArma_entoncesOrdenaYSumaPorComponente() {
        ReporteIndividual reporte =
                ReporteIndividual.de(PUBLICACION, contenido(HASH), "rectorado.demo", AHORA);

        assertThat(reporte.asignaturas()).containsExactly(SOC_B, BD_A);
        assertThat(reporte.actividades()).containsExactly(TUTORIAS, PROYECTO);
        assertThat(reporte.horas().clase()).isEqualByComparingTo("11.25");
        assertThat(reporte.horas().otrasDocencia()).isEqualByComparingTo("2.5");
        assertThat(reporte.horas().investigacion()).isEqualByComparingTo("3");
        assertThat(reporte.horas().vinculacion()).isEqualByComparingTo("0");
        assertThat(reporte.horas().gestion()).isEqualByComparingTo("0");
        assertThat(reporte.horas().total()).isEqualByComparingTo("16.75");
        assertThat(reporte.publicacion()).isEqualTo(PUBLICACION);
        assertThat(reporte.docente()).isEqualTo(ANA);
        assertThat(reporte.generadoPor()).isEqualTo("rectorado.demo");
        assertThat(reporte.generadoEn()).isEqualTo(AHORA);
    }

    @Test
    void dadoUnHashDistintoAlDeLaPublicacion_cuandoSeArma_entoncesEsInconsistente() {
        assertThatThrownBy(
                        () ->
                                ReporteIndividual.de(
                                        PUBLICACION,
                                        contenido("b".repeat(64)),
                                        "rectorado.demo",
                                        AHORA))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    @Test
    void dadasHorasQueNoSumanSuComponente_cuandoSeArma_entoncesEsInconsistente() {
        ContenidoDelDocente contenido =
                new ContenidoDelDocente(
                        HASH,
                        ANA,
                        List.of(BD_A),
                        List.of(),
                        new HorasPorComponente(
                                horas("9"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO));

        assertThatThrownBy(
                        () -> ReporteIndividual.de(PUBLICACION, contenido, "rectorado.demo", AHORA))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    @Test
    void
            dadoUnDocenteSinAsignaturasNiActividades_cuandoSeArma_entoncesTieneLosCincoComponentesEnCero() {
        ContenidoDelDocente contenido =
                new ContenidoDelDocente(
                        HASH,
                        new DatosDelDocente("0109990010", "PEREZ ANA", null, null),
                        List.of(),
                        List.of(),
                        HorasPorComponente.NINGUNA);

        ReporteIndividual reporte =
                ReporteIndividual.de(PUBLICACION, contenido, "rectorado.demo", AHORA);

        assertThat(reporte.asignaturas()).isEmpty();
        assertThat(reporte.actividades()).isEmpty();
        assertThat(reporte.horas().porComponente())
                .hasSize(5)
                .allSatisfy((componente, valor) -> assertThat(valor).isEqualByComparingTo("0"));
        assertThat(reporte.docente().dedicacion()).isEmpty();
        assertThat(reporte.docente().horasContratadas()).isEmpty();
    }

    @Test
    void dadasHorasConMasDeDosDecimalesONegativas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> new FilaDeAsignatura("S", "C", "N", "A", horas("1.234")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new FilaDeActividad(
                                        Componente.GESTION, "143", "G", null, horas("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaActividadDeClase_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new FilaDeActividad(Componente.CLASE, "1", "X", null, horas("1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ContenidoDelDocente contenido(String hash) {
        return new ContenidoDelDocente(
                hash,
                ANA,
                List.of(BD_A, SOC_B),
                List.of(PROYECTO, TUTORIAS),
                new HorasPorComponente(
                        horas("11.25"),
                        horas("2.5"),
                        horas("3"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO));
    }

    private static BigDecimal horas(String valor) {
        return new BigDecimal(valor);
    }
}
