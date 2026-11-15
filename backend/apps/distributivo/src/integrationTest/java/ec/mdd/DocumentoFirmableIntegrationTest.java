package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.pruebas.FirmasDePrueba;
import ec.mdd.firma.puertos.entrada.RechazarEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoRegistrado;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo.FirmanteDelPeriodo;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import ec.mdd.validacionocs.puertos.entrada.RegistrarRechazoEnFirma;
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
 * completo. Desde F14-H02, las tres firmas se suben en orden sobre el PDF/A real; desde F14-H04, un
 * rechazo en firma cierra el documento y avisa a Aprobación.
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
    @Autowired private RechazarEnFirma rechazar;
    @Autowired private ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes pendientes;

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

    @Test
    void dadaF1Aceptada_cuandoElVicerrectorRechazaEnF2_entoncesSeCierraYAprobacionRecibeElAviso()
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
        byte[] original = Files.readAllBytes(ARCHIVOS.resolve((String) documento.get("archivo")));
        byte[] conF1 =
                FirmasDePrueba.firmar(
                        original,
                        FirmasDePrueba.firmante(CA, "PÉREZ ANA"),
                        "firma-1-RESPONSABLE_CONSOLIDACION");
        ResultadoValidacion f1 =
                subir.subir(id, conF1, new Solicitante("docente.demo", Set.of("DOCENTE")));
        Solicitante vicerrector = new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"));

        RechazoRegistrado rechazo =
                rechazar.rechazar(id, "  Falta la carga de un docente de Software.  ", vicerrector);

        assertThat(rechazo.orden()).isEqualTo(2);
        assertThat(rechazo.rotulo()).isEqualTo("Revisado por");
        assertThat(rechazo.observacion()).isEqualTo("Falta la carga de un docente de Software.");
        // F14-H04: después de confirmar, Aprobación recibe el aviso una sola vez, con el paso.
        esperar(() -> Dobles.RECHAZOS.stream().anyMatch(uno -> uno.startsWith(periodo)));
        Thread.sleep(1000);
        assertThat(Dobles.RECHAZOS)
                .filteredOn(uno -> uno.startsWith(periodo))
                .containsExactly(periodo + "/1/2/VICERRECTOR/vicerrectorado.demo");
        // El documento queda cerrado: nadie firma ni rechaza otra vez (D-105).
        byte[] conF2 =
                FirmasDePrueba.firmar(
                        conF1, FirmasDePrueba.firmante(CA, "LÓPEZ LUIS"), "firma-2-VICERRECTOR");
        assertThatThrownBy(() -> subir.subir(id, conF2, vicerrector))
                .isInstanceOf(SinTurnoDeFirma.class);
        assertThatThrownBy(() -> rechazar.rechazar(id, "Otra observación cualquiera.", vicerrector))
                .isInstanceOf(SinTurnoDeFirma.class);
        // FR-007: la firma F1 y su archivo vigente se conservan.
        assertThat(
                        jdbc.queryForList(
                                "SELECT sha256_subido FROM firma.validacion_firma"
                                        + " WHERE periodo = ? AND aceptada",
                                String.class,
                                periodo))
                .containsExactly(f1.sha256Subido());
        assertThat(ARCHIVOS.resolve("documentos/" + f1.sha256Subido() + ".pdf"))
                .hasBinaryContent(conF1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM eventos_distributivo.event_publication"
                                        + " WHERE serialized_event LIKE ?"
                                        + " AND serialized_event LIKE ?",
                                Integer.class,
                                "%RECHAZO_EN_FIRMA%",
                                "%" + periodo + "%"))
                .isEqualTo(1);
    }

    // F12-H04: con la versión nueva, Aprobación ordena anular las firmas de la rechazada (D-106).
    // La vuelta a revisión y la versión nueva usan un proceso de aprobación y un distributivo
    // reales: las prueban RegistrarRechazoEnFirmaServicioTest, VersionNuevaIntegrationTest y el
    // script de punta a punta. Aquí, la orden real llega a Firma después de confirmar, y la versión
    // 2 empieza su cadena en F1.
    @Test
    void dadoElRechazoEnF2_cuandoAprobacionOrdenaAnular_entoncesFirmaAnulaYLaVersionDosEmpiezaEnF1()
            throws Exception {
        String periodo = periodo();
        transacciones.executeWithoutResult(estado -> eventos.publishEvent(aviso(periodo)));
        esperarGenerado(periodo);
        UUID id =
                jdbc.queryForObject(
                        "SELECT id FROM firma.documento_firmable WHERE periodo = ? AND version = 1",
                        UUID.class,
                        periodo);
        byte[] original =
                Files.readAllBytes(
                        ARCHIVOS.resolve(
                                jdbc.queryForObject(
                                        "SELECT archivo FROM firma.documento_firmable"
                                                + " WHERE id = ?",
                                        String.class,
                                        id)));
        ResultadoValidacion f1 =
                subir.subir(
                        id,
                        FirmasDePrueba.firmar(
                                original,
                                FirmasDePrueba.firmante(CA, "PÉREZ ANA"),
                                "firma-1-RESPONSABLE_CONSOLIDACION"),
                        new Solicitante("docente.demo", Set.of("DOCENTE")));
        rechazar.rechazar(
                id,
                "Falta la carga de un docente de Software.",
                new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR")));

        transacciones.executeWithoutResult(
                estado ->
                        eventos.publishEvent(
                                new ec.mdd.validacionocs.puertos.salida.AvisoDeAnulacion
                                        .FirmasPorAnular(periodo, 1, Instant.now())));

        esperar(
                () ->
                        jdbc.queryForObject(
                                        "SELECT count(*) FROM firma.anulacion_de_firmas"
                                                + " WHERE periodo = ?",
                                        Integer.class,
                                        periodo)
                                == 1);
        assertThat(
                        jdbc.queryForMap(
                                "SELECT version, motivo, firmas_anuladas, anulada_por"
                                        + " FROM firma.anulacion_de_firmas WHERE periodo = ?",
                                periodo))
                .containsEntry("version", 1)
                .containsEntry("motivo", "RECHAZO_EN_FIRMA")
                .containsEntry("firmas_anuladas", 1)
                .containsEntry("anulada_por", "sistema");
        // Las firmas se conservan: F1 sigue registrada, con su archivo.
        assertThat(
                        jdbc.queryForList(
                                "SELECT sha256_subido FROM firma.validacion_firma"
                                        + " WHERE periodo = ? AND aceptada",
                                String.class,
                                periodo))
                .containsExactly(f1.sha256Subido());
        assertThat(ARCHIVOS.resolve("documentos/" + f1.sha256Subido() + ".pdf")).exists();
        esperar(
                () ->
                        jdbc.queryForObject(
                                        "SELECT count(*) FROM eventos_distributivo"
                                                + ".event_publication WHERE serialized_event"
                                                + " LIKE ? AND serialized_event LIKE ?",
                                        Integer.class,
                                        "%FIRMAS_ANULADAS%",
                                        "%" + periodo + "%")
                                == 1);

        // Aprobar la versión 2 la lleva a firma: su documento empieza en F1, y el firmante de F1
        // solo ve la versión 2.
        Dobles.VERSIONES.put(periodo, 2);
        transacciones.executeWithoutResult(estado -> eventos.publishEvent(aviso(periodo, 2)));
        esperar(
                () ->
                        jdbc.queryForObject(
                                        "SELECT count(*) FROM firma.documento_firmable"
                                                + " WHERE periodo = ? AND version = 2",
                                        Integer.class,
                                        periodo)
                                == 1);
        assertThat(pendientes.pendientes(new Solicitante("docente.demo", Set.of("DOCENTE"))))
                .filteredOn(una -> una.periodo().equals(periodo))
                .singleElement()
                .satisfies(
                        una -> {
                            assertThat(una.version()).isEqualTo(2);
                            assertThat(una.orden()).isEqualTo(1);
                        });
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
        return aviso(periodo, 1);
    }

    private static AvisoDeFirma.DistributivoEnFirma aviso(String periodo, int version) {
        return new AvisoDeFirma.DistributivoEnFirma(
                periodo,
                version,
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

        /** Los avisos de rechazo en firma que recibió Aprobación (F14-H04). */
        static final List<String> RECHAZOS =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        /**
         * Aprobación, sin un proceso de aprobación real del periodo: anota el aviso. La vuelta a
         * EN_REVISION_VRA la prueban RegistrarRechazoEnFirmaServicioTest y el script.
         */
        @Bean
        @Primary
        RegistrarRechazoEnFirma registrarRechazoEnFirmaDePrueba() {
            return (periodo, version, rechazo) ->
                    RECHAZOS.add(
                            periodo
                                    + "/"
                                    + version
                                    + "/"
                                    + rechazo.orden()
                                    + "/"
                                    + rechazo.rol()
                                    + "/"
                                    + rechazo.rechazadoPor());
        }

        /** La versión que devuelve el contenido de cada periodo; 1 si no está (F12-H04). */
        static final Map<String, Integer> VERSIONES =
                new java.util.concurrent.ConcurrentHashMap<>();

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
                        VERSIONES.getOrDefault(periodo, 1),
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
