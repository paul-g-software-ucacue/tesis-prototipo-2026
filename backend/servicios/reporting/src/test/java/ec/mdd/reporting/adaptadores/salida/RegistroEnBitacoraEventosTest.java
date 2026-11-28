package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.reporting.dominio.CondicionesDeExportacion;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.InconsistenciasAbiertas;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.RechazoDeExportacion;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    void
            dadoUnCsvParaSiga_cuandoSeRegistra_entoncesLlevaLaVersionElLayoutYLaSumaSinDatosPersonales() {
        // F19-H02 (D-120): la misma entrada, desde la exportación, con su suma y su registro.
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000007");
        ExportacionSiga exportacion =
                new ExportacionSiga(
                        id,
                        "secretaria.demo",
                        AHORA,
                        "2026-I",
                        1,
                        "a".repeat(64),
                        3,
                        "siga-2026-I-v1.csv",
                        165,
                        new SumaDeVerificacion("b".repeat(64)));
        Map<String, Object> entrada = RegistroEnBitacoraEventos.entrada(exportacion);

        assertThat(entrada)
                .containsEntry("accion", "CSV_SIGA_GENERADO")
                .containsEntry("usuario", "secretaria.demo")
                .containsEntry("momento", "2026-11-21T15:00:00Z")
                .containsEntry("periodo", "2026-I")
                .containsEntry("objeto", Map.of("periodo", "2026-I", "version", 1))
                .containsEntry(
                        "nuevo",
                        Map.of(
                                "layout",
                                3,
                                "exportacion",
                                id.toString(),
                                "filas",
                                165,
                                "sha256",
                                "b".repeat(64)))
                .doesNotContainKey("anterior");
        assertThat(entrada.toString()).doesNotContain("a".repeat(64));
    }

    // F19-H02 (D-120): el rechazo por firmas o inconsistencias, sin hashes ni datos personales.
    @Test
    void dadoUnRechazoDeExportacion_cuandoSeRegistra_entoncesLlevaLosMotivosYSusNumeros() {
        CondicionesDeExportacion condiciones =
                CondicionesDeExportacion.de(
                        new Publicacion("2026-I", 1, "a".repeat(64), AHORA, "secretaria", false),
                        Optional.of(
                                new FirmasDeLaVersion(
                                        "a".repeat(64),
                                        List.of(
                                                new FirmasDeLaVersion.PasoFirmado(1, true),
                                                new FirmasDeLaVersion.PasoFirmado(2, false)),
                                        false,
                                        false)),
                        Optional.of(new InconsistenciasAbiertas(1, 0)));

        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        new RechazoDeExportacion(
                                "secretaria.demo", AHORA, "2026-I", 1, condiciones));

        assertThat(entrada)
                .containsEntry("accion", "EXPORTACION_SIGA_RECHAZADA")
                .containsEntry("usuario", "secretaria.demo")
                .containsEntry("momento", "2026-11-21T15:00:00Z")
                .containsEntry("periodo", "2026-I")
                .containsEntry("objeto", Map.of("periodo", "2026-I", "version", 1))
                .containsEntry(
                        "nuevo",
                        Map.of(
                                "motivos",
                                List.of("FIRMAS_INCOMPLETAS", "INCONSISTENCIAS_ABIERTAS"),
                                "firmados",
                                1,
                                "pasos",
                                2,
                                "duras",
                                1,
                                "blandas",
                                0))
                .doesNotContainKey("anterior");
        assertThat(entrada.toString()).doesNotContain("a".repeat(64));
    }

    @Test
    void dadoUnRechazoSinDocumentoNiRegistro_cuandoSeRegistra_entoncesSoloLosMotivos() {
        CondicionesDeExportacion condiciones =
                CondicionesDeExportacion.de(
                        new Publicacion("2026-I", 1, "a".repeat(64), AHORA, "secretaria", false),
                        Optional.empty(),
                        Optional.empty());

        Map<String, Object> entrada =
                RegistroEnBitacoraEventos.entrada(
                        new RechazoDeExportacion(
                                "secretaria.demo", AHORA, "2026-I", 1, condiciones));

        assertThat(entrada)
                .containsEntry(
                        "nuevo",
                        Map.of(
                                "motivos",
                                List.of("SIN_DOCUMENTO_FIRMADO", "INCONSISTENCIAS_SIN_REGISTRO")));
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
