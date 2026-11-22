package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Una descarga de un reporte, individual o general, para la bitácora (F20-H01, F20-H02, D-117). */
class DescargaDeReporteTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");

    @Test
    void dadaUnaDescargaIndividual_cuandoSeCrea_entoncesLlevaAlDocente() {
        DescargaDeReporte descarga =
                DescargaDeReporte.individual(
                        "rectorado.demo", AHORA, "2026-I", 1, "0109990010", Formato.PDF);

        assertThat(descarga.tipo()).isEqualTo(TipoDeReporte.INDIVIDUAL);
        assertThat(descarga.identificacion()).isEqualTo("0109990010");
    }

    @Test
    void dadaUnaDescargaGeneral_cuandoSeCrea_entoncesNoLlevaDocente() {
        DescargaDeReporte descarga =
                DescargaDeReporte.general("rectorado.demo", AHORA, "2026-I", 1, Formato.CSV);

        assertThat(descarga.tipo()).isEqualTo(TipoDeReporte.GENERAL);
        assertThat(descarga.identificacion()).isNull();
    }

    @Test
    void dadasLasCombinacionesInvalidas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(
                        () ->
                                new DescargaDeReporte(
                                        TipoDeReporte.INDIVIDUAL,
                                        "rectorado.demo",
                                        AHORA,
                                        "2026-I",
                                        1,
                                        null,
                                        Formato.PDF))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new DescargaDeReporte(
                                        TipoDeReporte.GENERAL,
                                        "rectorado.demo",
                                        AHORA,
                                        "2026-I",
                                        1,
                                        "0109990010",
                                        Formato.PDF))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoCadaTipo_cuandoSePideSuAccionYSuArchivo_entoncesSonLosDelContrato() {
        assertThat(TipoDeReporte.INDIVIDUAL.accion()).isEqualTo("REPORTE_INDIVIDUAL_DESCARGADO");
        assertThat(TipoDeReporte.GENERAL.accion()).isEqualTo("REPORTE_GENERAL_DESCARGADO");
        assertThat(
                        DescargaDeReporte.individual(
                                        "rectorado.demo",
                                        AHORA,
                                        "2026-I",
                                        1,
                                        "0109990010",
                                        Formato.PDF)
                                .nombreDelArchivo())
                .isEqualTo("reporte-individual-2026-I-0109990010.pdf");
        assertThat(
                        DescargaDeReporte.general("rectorado.demo", AHORA, "2026-I", 1, Formato.CSV)
                                .nombreDelArchivo())
                .isEqualTo("reporte-general-2026-I.csv");
    }
}
