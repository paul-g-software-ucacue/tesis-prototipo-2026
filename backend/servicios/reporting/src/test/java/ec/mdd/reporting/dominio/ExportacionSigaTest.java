package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La Exportación a SIGA (RF-53, F19-H02, D-120): quién, cuándo, la versión y su hash, el layout, el
 * archivo, sus filas y la suma de los bytes entregados.
 */
class ExportacionSigaTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant AHORA = Instant.parse("2026-11-28T15:00:00Z");
    private static final Publicacion PUBLICACION =
            new Publicacion("2026-I", 1, HASH, AHORA, "secretaria.demo", false);
    private static final byte[] BYTES = "ID\r\n0109990010\r\n".getBytes(StandardCharsets.UTF_8);

    @Test
    void dadoElArchivoEntregado_cuandoSeCrea_entoncesTomaSuSumaSuNombreYLaVersion() {
        ArchivoSiga archivo = new ArchivoSiga(BYTES, ArchivoSiga.nombreDe(PUBLICACION));

        ExportacionSiga exportacion =
                ExportacionSiga.de(PUBLICACION, 3, archivo, 7, "secretaria.demo", AHORA);

        assertThat(exportacion.id()).isNotNull();
        assertThat(exportacion.usuario()).isEqualTo("secretaria.demo");
        assertThat(exportacion.momento()).isEqualTo(AHORA);
        assertThat(exportacion.periodo()).isEqualTo("2026-I");
        assertThat(exportacion.version()).isEqualTo(1);
        assertThat(exportacion.hashVersion()).isEqualTo(HASH);
        assertThat(exportacion.layout()).isEqualTo(3);
        assertThat(exportacion.nombre()).isEqualTo("siga-2026-I-v1.csv");
        assertThat(exportacion.filas()).isEqualTo(7);
        assertThat(exportacion.suma()).isEqualTo(SumaDeVerificacion.de(BYTES));
    }

    @Test
    void dadosDosArchivosIguales_cuandoSeExportan_entoncesLaMismaSumaYDistintoId() {
        ArchivoSiga archivo = new ArchivoSiga(BYTES, "siga-2026-I-v1.csv");

        ExportacionSiga una = ExportacionSiga.de(PUBLICACION, 0, archivo, 1, "secretaria", AHORA);
        ExportacionSiga otra = ExportacionSiga.de(PUBLICACION, 0, archivo, 1, "secretaria", AHORA);

        assertThat(una.suma()).isEqualTo(otra.suma());
        assertThat(una.id()).isNotEqualTo(otra.id());
    }

    @Test
    void dadosDatosFueraDeSusReglas_cuandoSeCrea_entoncesSeRechazan() {
        SumaDeVerificacion suma = SumaDeVerificacion.de(BYTES);
        UUID id = UUID.randomUUID();
        for (Runnable invalida :
                List.<Runnable>of(
                        () ->
                                new ExportacionSiga(
                                        id, "", AHORA, "2026-I", 1, HASH, 0, "a.csv", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id,
                                        "u".repeat(65),
                                        AHORA,
                                        "2026-I",
                                        1,
                                        HASH,
                                        0,
                                        "a.csv",
                                        0,
                                        suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026 I", 1, HASH, 0, "a.csv", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026-I", 0, HASH, 0, "a.csv", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026-I", 1, "x", 0, "a.csv", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026-I", 1, HASH, -1, "a.csv", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026-I", 1, HASH, 0, "", 0, suma),
                        () ->
                                new ExportacionSiga(
                                        id, "u", AHORA, "2026-I", 1, HASH, 0, "a.csv", -1, suma))) {
            assertThatThrownBy(invalida::run).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
