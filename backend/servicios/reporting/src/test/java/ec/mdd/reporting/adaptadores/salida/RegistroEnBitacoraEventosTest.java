package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.LayoutCargado;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La entrada de la bitácora de cada descarga, según el tipo de reporte (F20-H01, F20-H02, D-117):
 * sin nombres ni horas.
 */
class RegistroEnBitacoraEventosTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");

    @Test
    void dadaUnaDescargaIndividual_cuandoSeRegistra_entoncesLlevaAlDocenteEnElObjeto() {
        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        DescargaDeReporte.individual(
                                "rectorado.demo", AHORA, "2026-I", 1, "0109990010", Formato.PDF));

        assertThat(entrada)
                .containsEntry("accion", "REPORTE_INDIVIDUAL_DESCARGADO")
                .containsEntry("usuario", "rectorado.demo")
                .containsEntry("periodo", "2026-I")
                .containsEntry(
                        "objeto",
                        Map.of("periodo", "2026-I", "version", 1, "identificacion", "0109990010"))
                .containsEntry("nuevo", Map.of("formato", "PDF"));
    }

    @Test
    void dadaUnaDescargaGeneral_cuandoSeRegistra_entoncesElObjetoEsElPeriodoYLaVersion() {
        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        DescargaDeReporte.general(
                                "secretaria.demo", AHORA, "2026-I", 2, Formato.CSV));

        assertThat(entrada)
                .containsEntry("accion", "REPORTE_GENERAL_DESCARGADO")
                .containsEntry("usuario", "secretaria.demo")
                .containsEntry("momento", "2026-11-21T15:00:00Z")
                .containsEntry("objeto", Map.of("periodo", "2026-I", "version", 2))
                .containsEntry("nuevo", Map.of("formato", "CSV"));
    }

    @Test
    void dadoUnCsvParaSiga_cuandoSeRegistra_entoncesLlevaLaVersionYElLayoutSinDatosPersonales() {
        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        new CsvGenerado("secretaria.demo", AHORA, "2026-I", 1, 3));

        assertThat(entrada)
                .containsEntry("accion", "CSV_SIGA_GENERADO")
                .containsEntry("usuario", "secretaria.demo")
                .containsEntry("momento", "2026-11-21T15:00:00Z")
                .containsEntry("periodo", "2026-I")
                .containsEntry("objeto", Map.of("periodo", "2026-I", "version", 1))
                .containsEntry("nuevo", Map.of("layout", 3))
                .doesNotContainKey("anterior");
    }

    @Test
    void dadoUnLayoutCargado_cuandoSeRegistra_entoncesLlevaSuNumeroColumnasYHuellaSinPeriodo() {
        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        new LayoutCargado("secretaria.demo", AHORA, 2, 12, "b".repeat(64)));

        assertThat(entrada)
                .containsEntry("accion", "LAYOUT_SIGA_CARGADO")
                .containsEntry("usuario", "secretaria.demo")
                .containsEntry("objeto", Map.of("layout", 2))
                .containsEntry("nuevo", Map.of("columnas", 12, "sha256", "b".repeat(64)))
                .doesNotContainKey("periodo");
    }
}
