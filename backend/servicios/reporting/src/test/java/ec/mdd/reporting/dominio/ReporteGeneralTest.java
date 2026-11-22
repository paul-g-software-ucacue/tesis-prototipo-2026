package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El reporte general consolidado (F20-H02, RF-46, D-117), con los docentes de los datos de prueba.
 */
class ReporteGeneralTest {

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

    @Test
    void dadosLosDocentesDeLaVersion_cuandoSeArma_entoncesLosOrdenaPorNombreEIdentificacion() {
        DocentesDeLaVersion.Docente primero = DocentesDePrueba.docente(0);
        DocentesDeLaVersion.Docente homonimo =
                new DocentesDeLaVersion.Docente(
                        "0109995999", primero.nombre(), null, horas("1", "0", "0", "0", "0"));
        List<DocentesDeLaVersion.Docente> desordenados =
                List.of(
                        DocentesDePrueba.docente(2),
                        homonimo,
                        primero,
                        DocentesDePrueba.docente(1));

        ReporteGeneral reporte = armar(desordenados);

        assertThat(reporte.docentes()).isSortedAccordingTo(ReporteGeneral.porNombre());
        assertThat(reporte.docentes())
                .filteredOn(una -> una.nombre().equals(primero.nombre()))
                .extracting(FilaDelDocente::identificacion)
                .containsExactly(primero.identificacion(), "0109995999");
        assertThat(reporte.cantidadDeDocentes()).isEqualTo(4);
        assertThat(reporte.publicacion()).isEqualTo(PUBLICACION);
        assertThat(reporte.generadoPor()).isEqualTo("rectorado.demo");
        assertThat(reporte.generadoEn()).isEqualTo(AHORA);
    }

    @Test
    void dadosNombresConTilde_cuandoSeArma_entoncesVanEnOrdenAlfabeticoDelEspanol() {
        HorasPorComponente horas = horas("1", "0", "0", "0", "0");
        ReporteGeneral reporte =
                armar(
                        List.of(
                                new DocentesDeLaVersion.Docente(
                                        "0109990010", "PERALTA MALDONADO JUAN ANDRES", null, horas),
                                new DocentesDeLaVersion.Docente(
                                        "0109990020", "ÁLVAREZ PEÑA ROSA", null, horas),
                                new DocentesDeLaVersion.Docente(
                                        "0109990030", "ÑAUPARI LOJA ANA", null, horas),
                                new DocentesDeLaVersion.Docente(
                                        "0109990040", "NUÑEZ VEGA LUIS", null, horas)));

        assertThat(reporte.docentes())
                .extracting(FilaDelDocente::nombre)
                .containsExactly(
                        "ÁLVAREZ PEÑA ROSA",
                        "NUÑEZ VEGA LUIS",
                        "ÑAUPARI LOJA ANA",
                        "PERALTA MALDONADO JUAN ANDRES");
    }

    @Test
    void dadosTodosLosDocentesDePrueba_cuandoSeArma_entoncesLosTotalesSonLaSumaExactaDeLasFilas() {
        ReporteGeneral reporte = armar(DocentesDePrueba.todos());

        for (Componente componente : Componente.values()) {
            BigDecimal suma =
                    reporte.docentes().stream()
                            .map(una -> una.horas().de(componente))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(reporte.totales().de(componente))
                    .as(componente.name())
                    .isEqualByComparingTo(suma);
        }
        BigDecimal sumaDeTotales =
                reporte.docentes().stream()
                        .map(una -> una.horas().total())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(reporte.totales().total()).isEqualByComparingTo(sumaDeTotales);
        assertThat(reporte.cantidadDeDocentes()).isEqualTo(47);
    }

    @Test
    void dadoUnComponenteSinHorasEnLaInstitucion_cuandoSeArma_entoncesSuTotalEsCero() {
        ReporteGeneral reporte =
                armar(
                        List.of(
                                conHoras(0, horas("8", "2.5", "0", "0", "0")),
                                conHoras(1, horas("12", "0", "3", "0", "0"))));

        assertThat(reporte.totales().vinculacion()).isEqualByComparingTo("0");
        assertThat(reporte.totales().gestion()).isEqualByComparingTo("0");
        assertThat(reporte.totales().porComponente()).hasSize(5);
    }

    @Test
    void dadasHorasConDosDecimales_cuandoSeSuman_entoncesNoSeRedondean() {
        ReporteGeneral reporte =
                armar(
                        List.of(
                                conHoras(0, horas("0.33", "0", "0", "0", "0")),
                                conHoras(1, horas("0.33", "0", "0", "0", "0")),
                                conHoras(2, horas("0.33", "0", "0", "0", "0"))));

        assertThat(reporte.totales().clase()).isEqualByComparingTo("0.99");
        assertThat(reporte.totales().total()).isEqualByComparingTo("0.99");
    }

    @Test
    void dadoUnHashDistintoAlDeLaPublicacion_cuandoSeArma_entoncesEsInconsistente() {
        DocentesDeLaVersion otraVersion =
                new DocentesDeLaVersion("b".repeat(64), DocentesDePrueba.todos());

        assertThatThrownBy(
                        () -> ReporteGeneral.de(PUBLICACION, otraVersion, "rectorado.demo", AHORA))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    @Test
    void dadoUnDocenteRepetido_cuandoSeArma_entoncesEsInconsistente() {
        DocentesDeLaVersion.Docente juan = DocentesDePrueba.docente(0);

        assertThatThrownBy(() -> armar(List.of(juan, DocentesDePrueba.docente(1), juan)))
                .isInstanceOf(ContenidoInconsistente.class)
                .hasMessageContaining(juan.identificacion());
    }

    @Test
    void dadaUnaVersionSinDocentes_cuandoSeArma_entoncesLosTotalesSonCero() {
        ReporteGeneral reporte = armar(List.of());

        assertThat(reporte.cantidadDeDocentes()).isZero();
        assertThat(reporte.totales().total()).isEqualByComparingTo("0");
    }

    @Test
    void dadoUnDocenteSinDedicacion_cuandoSeArma_entoncesSuFilaNoLaTiene() {
        DocentesDeLaVersion.Docente sinDedicacion =
                DocentesDePrueba.todos().stream()
                        .filter(una -> una.dedicacion().isEmpty())
                        .findFirst()
                        .orElseThrow();

        ReporteGeneral reporte = armar(List.of(sinDedicacion, DocentesDePrueba.docente(0)));

        assertThat(reporte.docentes())
                .filteredOn(una -> una.identificacion().equals(sinDedicacion.identificacion()))
                .singleElement()
                .satisfies(una -> assertThat(una.dedicacion()).isEmpty());
        assertThat(reporte.docentes())
                .filteredOn(una -> una.identificacion().equals("0109990010"))
                .singleElement()
                .satisfies(una -> assertThat(una.dedicacion()).contains(Dedicacion.TC));
    }

    @Test
    void dadosTotalesQueNoSonLaSumaDeLasFilas_cuandoSeCreaDirecto_entoncesSeRechaza() {
        FilaDelDocente fila =
                new FilaDelDocente("0109990010", "PERALTA", null, horas("8", "0", "0", "0", "0"));

        assertThatThrownBy(
                        () ->
                                new ReporteGeneral(
                                        PUBLICACION,
                                        List.of(fila),
                                        horas("9", "0", "0", "0", "0"),
                                        "rectorado.demo",
                                        AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReporteGeneral armar(List<DocentesDeLaVersion.Docente> docentes) {
        return ReporteGeneral.de(
                PUBLICACION, new DocentesDeLaVersion(HASH, docentes), "rectorado.demo", AHORA);
    }

    private static DocentesDeLaVersion.Docente conHoras(int indice, HorasPorComponente horas) {
        DocentesDeLaVersion.Docente base = DocentesDePrueba.docente(indice);
        return new DocentesDeLaVersion.Docente(
                base.identificacion(), base.nombre(), base.dedicacion().orElse(null), horas);
    }

    private static HorasPorComponente horas(
            String clase, String otras, String investigacion, String vinculacion, String gestion) {
        return new HorasPorComponente(
                new BigDecimal(clase),
                new BigDecimal(otras),
                new BigDecimal(investigacion),
                new BigDecimal(vinculacion),
                new BigDecimal(gestion));
    }
}
