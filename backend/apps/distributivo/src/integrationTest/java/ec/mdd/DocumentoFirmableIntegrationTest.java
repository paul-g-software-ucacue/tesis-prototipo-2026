package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El documento firmable en el proceso distributivo, con PostgreSQL real (F13-H02, research §4): el
 * aviso de entrada en firma se guarda en la transacción del paso; después de confirmar, Aprobación
 * le entrega la cadena a Firma (MD-05), que genera el PDF/A, lo guarda por su hash y lo registra.
 * El contenido de Generación y los nombres de los firmantes van con dobles: los procesos datos e
 * identidad no están, y el contenido real necesita un distributivo completo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({EsquemasIntegrationTest.Contenedores.class, DocumentoFirmableIntegrationTest.Dobles.class})
class DocumentoFirmableIntegrationTest {

    private static final Path ARCHIVOS = carpetaTemporal();
    private static final String HASH = "e".repeat(64);
    private static final Duration DEMORA_DE_LOS_FIRMANTES = Duration.ofSeconds(1);

    @Autowired private ApplicationEventPublisher eventos;
    @Autowired private TransactionTemplate transacciones;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry propiedades) {
        propiedades.add("mdd.archivos.raiz", ARCHIVOS::toString);
    }

    @Test
    void dadoElPasoAFirmaConfirmado_cuandoTermina_entoncesQuedaElDocumentoGeneradoYSuArchivo()
            throws Exception {
        String periodo = periodo();
        Instant antes = Instant.now();

        transacciones.executeWithoutResult(estado -> eventos.publishEvent(aviso(periodo)));

        // La transacción del paso no espera a la generación (los firmantes tardan 1 s).
        assertThat(Duration.between(antes, Instant.now())).isLessThan(DEMORA_DE_LOS_FIRMANTES);
        esperarGenerado(periodo);
        Map<String, Object> documento =
                jdbc.queryForMap(
                        "SELECT hash_contenido, sha256_original, archivo, generado_por"
                                + " FROM firma.documento_firmable WHERE periodo = ? AND version = 1",
                        periodo);
        assertThat(documento.get("hash_contenido")).isEqualTo(HASH);
        assertThat(documento.get("generado_por")).isEqualTo("sistema");
        String sha = (String) documento.get("sha256_original");
        assertThat(documento.get("archivo")).isEqualTo("documentos/" + sha + ".pdf");
        assertThat(ARCHIVOS.resolve("documentos/" + sha + ".pdf")).exists();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM eventos_distributivo.event_publication"
                                        + " WHERE serialized_event LIKE ?"
                                        + " AND serialized_event LIKE ?",
                                Integer.class,
                                "%firma.EntradaBitacora%",
                                "%" + periodo + "%"))
                .as("la entrada de la bitácora, pendiente de entregar a identidad")
                .isOne();
    }

    @Test
    void dadoElPasoAFirmaRevertido_cuandoTermina_entoncesNoSeGeneraNada() throws Exception {
        String periodo = periodo();

        assertThatThrownBy(
                        () ->
                                transacciones.executeWithoutResult(
                                        estado -> {
                                            eventos.publishEvent(aviso(periodo));
                                            throw new IllegalStateException("se revierte");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        Thread.sleep(DEMORA_DE_LOS_FIRMANTES.plusSeconds(1).toMillis());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM firma.generacion_documento WHERE periodo = ?",
                                Integer.class,
                                periodo))
                .isZero();
    }

    private void esperarGenerado(String periodo) throws InterruptedException {
        Instant limite = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(limite)) {
            List<String> estados =
                    jdbc.queryForList(
                            "SELECT estado FROM firma.generacion_documento WHERE periodo = ?",
                            String.class,
                            periodo);
            if (estados.contains("GENERADA") || estados.contains("FALLIDA")) {
                assertThat(estados).containsExactly("GENERADA");
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("El documento no se generó en 30 s");
    }

    private static AvisoDeFirma.DistributivoEnFirma aviso(String periodo) {
        return new AvisoDeFirma.DistributivoEnFirma(
                periodo,
                1,
                HASH,
                List.of(
                        new AvisoDeFirma.Paso(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                        new AvisoDeFirma.Paso(2, "Revisado", "VICERRECTOR"),
                        new AvisoDeFirma.Paso(3, "Aprobado", "RECTOR")),
                "RESPONSABLE_CONSOLIDACION",
                Instant.now());
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Path carpetaTemporal() {
        try {
            return Files.createTempDirectory("mdd-archivos-");
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /** El contenido y los firmantes, sin los procesos datos e identidad. */
    @TestConfiguration
    static class Dobles {

        @Bean
        @Primary
        ContenidoDelDistributivo contenidoDePrueba() {
            return periodo -> {
                ContenidoDelDocumento.Horas horas =
                        new ContenidoDelDocumento.Horas(
                                new BigDecimal("4"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO);
                return new ContenidoDelDocumento(
                        periodo,
                        1,
                        HASH,
                        List.of(new ContenidoDelDocumento.Carrera("TDS", "ENVIADO", 1, 1, horas)),
                        horas,
                        List.of(),
                        List.of());
            };
        }

        @Bean
        @Primary
        FirmantesDelPeriodo firmantesDePrueba() {
            return (periodo, roles) -> {
                try {
                    Thread.sleep(DEMORA_DE_LOS_FIRMANTES.toMillis());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
                return Map.of(
                        "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                        "VICERRECTOR", "LÓPEZ LUIS",
                        "RECTOR", "MORA EVA");
            };
        }
    }
}
