package ec.mdd.firma;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.adaptadores.salida.RepositorioDocumentosJdbc;
import ec.mdd.firma.adaptadores.salida.TransaccionSpring;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.FirmaRegistrada;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.infraestructura.FirmaFlywayConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Las generaciones y los documentos contra PostgreSQL real (F13-H02, research §8): guardar y leer,
 * la cadena en jsonb, y dos generaciones a la vez con un solo documento (SC-005).
 */
class RepositorioDocumentosIntegrationTest {

    private static final Instant MOMENTO =
            Instant.parse("2026-11-11T15:00:00.123456Z").truncatedTo(ChronoUnit.MICROS);
    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"));
    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(
                            1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Docente (demo)"),
                    FirmanteEsperado.porIdentificacion(
                            2, "VICERRECTOR", "0109990028", "CABRERA VAZQUEZ ANA GABRIELA"));

    private static PostgreSQLContainer postgres;
    private static RepositorioDocumentosJdbc repositorio;
    private static DataSource dataSource;
    private static TransaccionSpring transaccion;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new FirmaFlywayConfig().flywayFirma(dataSource).migrate();
        repositorio = new RepositorioDocumentosJdbc(JdbcClient.create(dataSource));
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadaUnaGeneracion_cuandoSeGuardaYSeLee_entoncesQuedaIgualConSuCadena() {
        String periodo = periodo();
        GeneracionDeDocumento generacion = nueva(periodo);
        generacion.fallar("Falta el nombre del Rector.", MOMENTO);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generacion);
                    return null;
                });

        GeneracionDeDocumento leida =
                transaccion.ejecutar(() -> repositorio.bloquear(periodo, 1).orElseThrow());
        assertThat(leida.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(leida.motivo()).contains("Falta el nombre del Rector.");
        assertThat(leida.cadena()).isEqualTo(CADENA);
        assertThat(leida.rolDeLaEtapa()).isEqualTo("RESPONSABLE_CONSOLIDACION");
        assertThat(leida.hashContenido()).isEqualTo("a".repeat(64));
        assertThat(leida.actualizadaEn()).isEqualTo(MOMENTO);
        assertThat(repositorio.ultima(periodo)).isPresent();
        assertThat(transaccion.ejecutar(() -> repositorio.bloquear(periodo, 2))).isEmpty();
    }

    @Test
    void dadoUnDocumento_cuandoSeRegistraYSeLee_entoncesQuedaIgual() {
        String periodo = periodo();
        GeneracionDeDocumento generacion = nueva(periodo);
        DocumentoFirmable documento = documento(periodo);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generacion);
                    repositorio.registrar(documento, FIRMANTES);
                    return null;
                });

        assertThat(repositorio.documento(periodo, 1)).contains(documento);
        assertThat(repositorio.documento(periodo, 2)).isEmpty();
    }

    @Test
    void dadoUnDocumentoConSusFirmantes_cuandoSeLeenSusFirmas_entoncesTraeCadenaYFirmantes() {
        String periodo = periodo();
        DocumentoFirmable documento = documento(periodo);
        GeneracionDeDocumento generada = nueva(periodo);
        generada.completar(MOMENTO);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generada);
                    repositorio.registrar(documento, FIRMANTES);
                    return null;
                });

        FirmasDelDocumento firmas = repositorio.firmas(documento.id()).orElseThrow();
        assertThat(firmas.documento()).isEqualTo(documento);
        assertThat(firmas.cadena()).isEqualTo(CADENA);
        assertThat(firmas.firmantes()).containsExactlyElementsOf(FIRMANTES);
        assertThat(repositorio.generados())
                .filteredOn(una -> una.documento().periodo().equals(periodo))
                .singleElement()
                .isEqualTo(firmas);
        assertThat(repositorio.firmas(UUID.randomUUID())).isEmpty();
    }

    @Test
    void dadoUnDocumentoDeUnaGeneracionQueNoEstaGenerada_cuandoSeLeen_entoncesNoAparece() {
        String periodo = periodo();
        DocumentoFirmable documento = documento(periodo);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(nueva(periodo));
                    repositorio.registrar(documento, FIRMANTES);
                    return null;
                });

        assertThat(repositorio.firmas(documento.id())).isEmpty();
        assertThat(repositorio.generados())
                .noneMatch(una -> una.documento().periodo().equals(periodo));
    }

    @Test
    void dadasDosGeneracionesALaVez_cuandoCadaUnaRegistra_entoncesQuedaUnSoloDocumento()
            throws Exception {
        String periodo = periodo();
        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(nueva(periodo));
                    return null;
                });
        CountDownLatch salida = new CountDownLatch(1);
        List<String> resultados = Collections.synchronizedList(new ArrayList<>());
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    salida.await(10, TimeUnit.SECONDS);
                                    return transaccion.ejecutar(
                                            () -> {
                                                GeneracionDeDocumento una =
                                                        repositorio
                                                                .bloquear(periodo, 1)
                                                                .orElseThrow();
                                                if (una.estado() == EstadoDeGeneracion.GENERADA) {
                                                    resultados.add("ya estaba");
                                                    return null;
                                                }
                                                pausa();
                                                repositorio.registrar(
                                                        documento(periodo), FIRMANTES);
                                                una.completar(MOMENTO);
                                                repositorio.guardar(una);
                                                resultados.add("registró");
                                                return null;
                                            });
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(resultados).containsExactlyInAnyOrder("registró", "ya estaba");
        assertThat(repositorio.documento(periodo, 1)).isPresent();
    }

    @Test
    void dadaUnaValidacionAceptada_cuandoSeRegistra_entoncesEsLaFirmaDelPasoYElTurnoAvanza() {
        FirmasDelDocumento antes = generado(periodo());
        ResultadoValidacion aceptada = aceptada(antes, 1, "c");

        boolean registrada =
                transaccion.ejecutar(() -> repositorio.registrarValidacion(aceptada, antes));

        FirmasDelDocumento despues = repositorio.firmas(antes.documento().id()).orElseThrow();
        assertThat(registrada).isTrue();
        assertThat(despues.firmas())
                .containsExactly(
                        new FirmaRegistrada(1, "c".repeat(64), 4096, "docente.demo", MOMENTO));
        assertThat(despues.archivoVigente().sha256()).isEqualTo("c".repeat(64));
        assertThat(despues.pasoEnTurno()).get().extracting(t -> t.paso().orden()).isEqualTo(2);
        assertThat(repositorio.generados())
                .filteredOn(una -> una.documento().id().equals(antes.documento().id()))
                .singleElement()
                .isEqualTo(despues);
        assertThat(repositorio.validaciones(antes.documento().id())).containsExactly(aceptada);
    }

    @Test
    void dadaUnaValidacionRechazada_cuandoSeRegistra_entoncesQuedaSinFirmaYConSusComprobaciones() {
        FirmasDelDocumento antes = generado(periodo());
        ResultadoValidacion rechazada =
                new ResultadoValidacion(
                        UUID.randomUUID(),
                        antes.documento().id(),
                        1,
                        "docente.demo",
                        MOMENTO,
                        "d".repeat(64),
                        512,
                        ResultadoDeComprobacion.desde(
                                ResultadoDeComprobacion.rechazada(
                                        Comprobacion.MISMO_DOCUMENTO, "No es la versión vigente.")),
                        Optional.empty());

        boolean registrada =
                transaccion.ejecutar(() -> repositorio.registrarValidacion(rechazada, antes));

        assertThat(registrada).isTrue();
        assertThat(repositorio.firmas(antes.documento().id()).orElseThrow().firmas()).isEmpty();
        assertThat(repositorio.validaciones(antes.documento().id())).containsExactly(rechazada);
    }

    @Test
    void
            dadasLasFirmasCambiadasDesdeLaValidacion_cuandoSeRegistraLaAceptada_entoncesNoSeRegistra() {
        FirmasDelDocumento antes = generado(periodo());
        transaccion.ejecutar(() -> repositorio.registrarValidacion(aceptada(antes, 1, "c"), antes));

        boolean registrada =
                transaccion.ejecutar(
                        () -> repositorio.registrarValidacion(aceptada(antes, 1, "e"), antes));

        assertThat(registrada).isFalse();
        assertThat(repositorio.firmas(antes.documento().id()).orElseThrow().firmas())
                .extracting(FirmaRegistrada::sha256)
                .containsExactly("c".repeat(64));
        assertThat(repositorio.validaciones(antes.documento().id())).hasSize(1);
    }

    @Test
    void dadasDosSubidasAceptadasALaVez_cuandoSeRegistran_entoncesSoloUnaEsLaFirma()
            throws Exception {
        FirmasDelDocumento antes = generado(periodo());
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Boolean> resultados = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (String hash : List.of("c", "e")) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    salida.await(10, TimeUnit.SECONDS);
                                    return resultados.add(
                                            transaccion.ejecutar(
                                                    () ->
                                                            repositorio.registrarValidacion(
                                                                    aceptada(antes, 1, hash),
                                                                    antes)));
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(repositorio.firmas(antes.documento().id()).orElseThrow().firmas()).hasSize(1);
    }

    @Test
    void
            dadaUnaValidacionDeF14H02ConCuatroComprobaciones_cuandoSeLee_entoncesLasNuevasQuedanNoEvaluadas() {
        FirmasDelDocumento antes = generado(periodo());
        UUID id = UUID.randomUUID();
        JdbcClient.create(dataSource)
                .sql(
                        "insert into firma.validacion_firma (id, periodo, version, tipo, orden,"
                                + " subido_por, subido_en, sha256_subido, tamano, aceptada,"
                                + " comprobaciones) values (:id, :periodo, 1, 'CONSOLIDADO', 1,"
                                + " 'docente.demo', :momento, :sha, 10, false, cast(:comprobaciones"
                                + " as jsonb))")
                .param("id", id)
                .param("periodo", antes.documento().periodo())
                .param("momento", java.sql.Timestamp.from(MOMENTO))
                .param("sha", "f".repeat(64))
                .param(
                        "comprobaciones",
                        "[{\"comprobacion\":\"MISMO_DOCUMENTO\",\"resultado\":\"RECHAZADA\","
                                + "\"motivo\":\"Otro.\"},{\"comprobacion\":\"UNA_FIRMA_NUEVA\","
                                + "\"resultado\":\"NO_EVALUADA\",\"motivo\":\"Otro.\"},"
                                + "{\"comprobacion\":\"INTEGRIDAD\",\"resultado\":\"NO_EVALUADA\","
                                + "\"motivo\":\"Otro.\"},{\"comprobacion\":\"CONFIANZA\","
                                + "\"resultado\":\"NO_EVALUADA\",\"motivo\":\"Otro.\"}]")
                .update();

        assertThat(repositorio.validaciones(antes.documento().id()))
                .singleElement()
                .satisfies(
                        una ->
                                assertThat(una.comprobaciones())
                                        .hasSize(7)
                                        .last()
                                        .satisfies(
                                                ultima ->
                                                        assertThat(ultima.resultado())
                                                                .isEqualTo(
                                                                        ec.mdd.firma.dominio
                                                                                .Resultado
                                                                                .NO_EVALUADA)));
    }

    // El rechazo en firma (F14-H04, D-105).

    private static RechazoDelFirmante rechazo(int orden) {
        return new RechazoDelFirmante(
                orden, "vicerrectorado.demo", MOMENTO, "Falta la carga de un docente de Software.");
    }

    @Test
    void dadoUnRechazo_cuandoSeRegistraYSeLee_entoncesElDocumentoQuedaCerradoConSuRechazo() {
        FirmasDelDocumento antes = generado(periodo());
        transaccion.ejecutar(() -> repositorio.registrarValidacion(aceptada(antes, 1, "c"), antes));
        FirmasDelDocumento conF1 = repositorio.firmas(antes.documento().id()).orElseThrow();

        boolean registrado =
                transaccion.ejecutar(() -> repositorio.registrarRechazo(rechazo(2), conF1));

        FirmasDelDocumento despues = repositorio.firmas(antes.documento().id()).orElseThrow();
        assertThat(registrado).isTrue();
        assertThat(despues.rechazo()).contains(rechazo(2));
        assertThat(despues.pasoEnTurno()).isEmpty();
        assertThat(despues.firmas()).isEqualTo(conF1.firmas());
        assertThat(despues.archivoVigente()).isEqualTo(conF1.archivoVigente());
    }

    @Test
    void dadaUnaFirmaRegistradaEntretanto_cuandoSeRegistraElRechazo_entoncesNoSeGuarda() {
        FirmasDelDocumento antes = generado(periodo());
        transaccion.ejecutar(() -> repositorio.registrarValidacion(aceptada(antes, 1, "c"), antes));

        boolean registrado =
                transaccion.ejecutar(() -> repositorio.registrarRechazo(rechazo(1), antes));

        assertThat(registrado).isFalse();
        assertThat(repositorio.firmas(antes.documento().id()).orElseThrow().rechazo()).isEmpty();
    }

    @Test
    void dadoUnRechazoRegistrado_cuandoLlegaUnaFirmaAceptada_entoncesNoSeRegistra() {
        FirmasDelDocumento antes = generado(periodo());
        transaccion.ejecutar(() -> repositorio.registrarRechazo(rechazo(1), antes));

        boolean registrada =
                transaccion.ejecutar(
                        () -> repositorio.registrarValidacion(aceptada(antes, 1, "c"), antes));

        assertThat(registrada).isFalse();
        assertThat(repositorio.firmas(antes.documento().id()).orElseThrow().firmas()).isEmpty();
    }

    @Test
    void dadosUnRechazoYUnaFirmaDelMismoPasoALaVez_cuandoSeRegistran_entoncesQuedaSoloUno()
            throws Exception {
        FirmasDelDocumento antes = generado(periodo());
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Boolean> resultados = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tareas = new ArrayList<>();
            tareas.add(
                    hilos.submit(
                            () -> {
                                salida.await(10, TimeUnit.SECONDS);
                                return resultados.add(
                                        transaccion.ejecutar(
                                                () ->
                                                        repositorio.registrarRechazo(
                                                                rechazo(1), antes)));
                            }));
            tareas.add(
                    hilos.submit(
                            () -> {
                                salida.await(10, TimeUnit.SECONDS);
                                return resultados.add(
                                        transaccion.ejecutar(
                                                () ->
                                                        repositorio.registrarValidacion(
                                                                aceptada(antes, 1, "c"), antes)));
                            }));
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        FirmasDelDocumento despues = repositorio.firmas(antes.documento().id()).orElseThrow();
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(despues.firmas().size() + (despues.rechazo().isPresent() ? 1 : 0)).isEqualTo(1);
    }

    private FirmasDelDocumento generado(String periodo) {
        DocumentoFirmable documento = documento(periodo);
        GeneracionDeDocumento generada = nueva(periodo);
        generada.completar(MOMENTO);
        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generada);
                    repositorio.registrar(documento, FIRMANTES);
                    return null;
                });
        return repositorio.firmas(documento.id()).orElseThrow();
    }

    private static ResultadoValidacion aceptada(FirmasDelDocumento contra, int orden, String hash) {
        return new ResultadoValidacion(
                UUID.randomUUID(),
                contra.documento().id(),
                orden,
                "docente.demo",
                MOMENTO,
                hash.repeat(64),
                4096,
                java.util.Arrays.stream(Comprobacion.values())
                        .map(ResultadoDeComprobacion::aprobada)
                        .toList(),
                Optional.of(
                        new CertificadoDeLaFirma(
                                "Firmante Uno Prueba (F1)",
                                "CN=CA de Prueba MDD Demo,O=MDD Demo,C=EC",
                                "19a2b3",
                                MOMENTO.minus(1, ChronoUnit.DAYS),
                                MOMENTO.plus(365, ChronoUnit.DAYS))));
    }

    private static GeneracionDeDocumento nueva(String periodo) {
        return GeneracionDeDocumento.nueva(
                periodo,
                1,
                "a".repeat(64),
                CADENA,
                "RESPONSABLE_CONSOLIDACION",
                "sistema",
                MOMENTO);
    }

    private static DocumentoFirmable documento(String periodo) {
        return new DocumentoFirmable(
                UUID.randomUUID(),
                periodo,
                1,
                "CONSOLIDADO",
                "a".repeat(64),
                "b".repeat(64),
                "documentos/" + "b".repeat(64) + ".pdf",
                2048,
                "sistema",
                MOMENTO);
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void pausa() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}
