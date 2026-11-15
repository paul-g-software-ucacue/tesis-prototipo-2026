package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.pruebas.FirmasDePrueba;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo.FirmanteDelPeriodo;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
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
import java.util.Optional;
import java.util.Set;
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
 * El contenido de Generación, los nombres de los firmantes y la identificación de las cuentas van
 * con dobles: los procesos datos e identidad no están, y el contenido real necesita un distributivo
 * completo. Desde F14-H02, las tres firmas se suben en orden sobre el PDF/A real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({EsquemasIntegrationTest.Contenedores.class, DocumentoFirmableIntegrationTest.Dobles.class})
class DocumentoFirmableIntegrationTest {

    private static final Path ARCHIVOS = carpetaTemporal();
    // F14-H02: una CA en memoria, la única raíz de la lista de confianza de esta prueba. Así se
    // prueba también que mdd.firma.raices reemplaza la lista por defecto (D-103).
    private static final FirmasDePrueba.Identidad CA = caDePrueba();
    private static final Path RAICES = raices();
    private static final String HASH = "e".repeat(64);
    private static final Duration DEMORA_DE_LOS_FIRMANTES = Duration.ofSeconds(1);

    @Autowired private ApplicationEventPublisher eventos;
    @Autowired private TransactionTemplate transacciones;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SubirFirma subir;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry propiedades) {
        propiedades.add("mdd.archivos.raiz", ARCHIVOS::toString);
        propiedades.add("mdd.firma.raices", () -> RAICES.toUri().toString());
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
        // F14-H01: el firmante esperado de cada paso, registrado con el documento. Con el rol del
        // proceso lo prueba DocumentoFirmableEsquemaIntegrationTest de Firma.
        assertThat(
                        jdbc.queryForList(
                                "SELECT orden || ':' || coalesce(usuario, identificacion)"
                                        + " FROM firma.firmante_esperado WHERE periodo = ?"
                                        + " ORDER BY orden",
                                String.class,
                                periodo))
                .containsExactly("1:docente.demo", "2:0109990028", "3:0109990010");
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

    @Test
    void dadoElDocumentoGenerado_cuandoSeSubenLasTresFirmasEnOrden_entoncesCadaUnaEsLaVigente()
            throws Exception {
        String periodo = periodo();
        transacciones.executeWithoutResult(estado -> eventos.publishEvent(aviso(periodo)));
        esperarGenerado(periodo);
        Map<String, Object> documento =
                jdbc.queryForMap(
                        "SELECT id, archivo FROM firma.documento_firmable"
                                + " WHERE periodo = ? AND version = 1",
                        periodo);
        UUID id = (UUID) documento.get("id");
        byte[] vigente = Files.readAllBytes(ARCHIVOS.resolve((String) documento.get("archivo")));
        List<Path> archivos = new java.util.ArrayList<>();
        archivos.add(ARCHIVOS.resolve((String) documento.get("archivo")));
        List<Solicitante> firmantes =
                List.of(
                        new Solicitante("docente.demo", Set.of("DOCENTE")),
                        new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR")),
                        new Solicitante("rectorado.demo", Set.of("RECTOR")));
        // F14-H03: el CN de cada firma es el nombre que el PDF imprime en su paso (los de los
        // dobles).
        List<String> nombres = List.of("PÉREZ ANA", "LÓPEZ LUIS", "MORA EVA");
        List<String> campos =
                List.of(
                        "firma-1-RESPONSABLE_CONSOLIDACION",
                        "firma-2-VICERRECTOR",
                        "firma-3-RECTOR");

        for (int i = 0; i < 3; i++) {
            byte[] firmado =
                    FirmasDePrueba.firmar(
                            vigente, FirmasDePrueba.firmante(CA, nombres.get(i)), campos.get(i));

            ResultadoValidacion resultado = subir.subir(id, firmado, firmantes.get(i));

            assertThat(resultado.aceptada()).isTrue();
            assertThat(resultado.orden()).isEqualTo(i + 1);
            Path nuevo = ARCHIVOS.resolve("documentos/" + resultado.sha256Subido() + ".pdf");
            assertThat(nuevo).exists();
            assertThat(Files.readAllBytes(nuevo)).isEqualTo(firmado);
            archivos.add(nuevo);
            vigente = firmado;
        }

        // F14-H03: después de confirmar la tercera, Aprobación recibe el aviso de cadena completa
        // una sola vez, con el periodo y la versión.
        esperar(() -> !Dobles.CADENAS_COMPLETAS.isEmpty());
        Thread.sleep(1000);
        assertThat(Dobles.CADENAS_COMPLETAS).containsExactly(periodo + "/1");
        // Cada archivo anterior se conserva sin cambios (D-101), y nadie está en turno.
        assertThat(archivos).hasSize(4).allSatisfy(archivo -> assertThat(archivo).exists());
        assertThat(
                        jdbc.queryForList(
                                "SELECT orden FROM firma.validacion_firma WHERE periodo = ?"
                                        + " AND aceptada ORDER BY orden",
                                Integer.class,
                                periodo))
                .containsExactly(1, 2, 3);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM eventos_distributivo.event_publication"
                                        + " WHERE serialized_event LIKE ?"
                                        + " AND serialized_event LIKE ?",
                                Integer.class,
                                "%FIRMA_ACEPTADA%",
                                "%" + periodo + "%"))
                .isEqualTo(3);
    }

    private static void esperar(java.util.function.BooleanSupplier condicion)
            throws InterruptedException {
        Instant limite = Instant.now().plusSeconds(30);
        while (!condicion.getAsBoolean()) {
            if (Instant.now().isAfter(limite)) {
                throw new AssertionError("No se cumplió en 30 s");
            }
            Thread.sleep(200);
        }
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

    private static FirmasDePrueba.Identidad caDePrueba() {
        try {
            return FirmasDePrueba.ca("CA de Prueba del proceso distributivo");
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Path raices() {
        try {
            Path archivo = Files.createTempFile("mdd-raices-", ".crt");
            Files.writeString(archivo, FirmasDePrueba.pem(CA.certificado()));
            return archivo;
        } catch (IOException | java.security.GeneralSecurityException error) {
            throw new IllegalStateException(error);
        }
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

        /** Los avisos de cadena completa que recibió Aprobación, como «periodo/versión». */
        static final List<String> CADENAS_COMPLETAS =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        /**
         * Aprobación, sin un proceso de aprobación real del periodo: anota el aviso. El paso a
         * EN_OCS lo prueban CompletarFirmasServicioTest y el script de punta a punta.
         */
        @Bean
        @Primary
        CompletarFirmas completarFirmasDePrueba() {
            return (periodo, version, momento) -> CADENAS_COMPLETAS.add(periodo + "/" + version);
        }

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

        /** La identificación de las cuentas, sin el proceso identidad (F14-H01). */
        @Bean
        @Primary
        IdentificacionDeUsuarios identificacionDePrueba() {
            return usuario ->
                    switch (usuario) {
                        case "vicerrectorado.demo" -> Optional.of("0109990028");
                        case "rectorado.demo" -> Optional.of("0109990010");
                        default -> Optional.empty();
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
                        "RESPONSABLE_CONSOLIDACION",
                        FirmanteDelPeriodo.porUsuario("PÉREZ ANA", "docente.demo"),
                        "VICERRECTOR",
                        FirmanteDelPeriodo.porIdentificacion("LÓPEZ LUIS", "0109990028"),
                        "RECTOR",
                        FirmanteDelPeriodo.porIdentificacion("MORA EVA", "0109990010"));
            };
        }
    }
}
