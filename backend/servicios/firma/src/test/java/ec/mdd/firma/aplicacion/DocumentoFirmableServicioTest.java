package ec.mdd.firma.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.GeneracionFallida;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.dominio.SinRolDeFirma;
import ec.mdd.firma.puertos.entrada.EstadoDelDocumento;
import ec.mdd.firma.puertos.entrada.PasoDeLaCadena;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SolicitudDeDocumento;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo.FirmanteDelPeriodo;
import ec.mdd.firma.puertos.salida.GeneradorDePdf;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El documento firmable (F13-H02, research §4 y §8): Aprobación entrega la cadena, Firma genera,
 * guarda por el hash y registra una vez. Generación, los procesos, el PDF y el almacén con dobles;
 * las generaciones en memoria.
 */
class DocumentoFirmableServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Instant ENTRO = Instant.parse("2026-11-11T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-11T15:00:05Z");
    private static final String HASH = "e".repeat(64);
    private static final byte[] PDF = "%PDF-1.7 documento".getBytes(StandardCharsets.US_ASCII);
    private static final List<PasoDeLaCadena> CADENA =
            List.of(
                    new PasoDeLaCadena(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeLaCadena(2, "Revisado", "VICERRECTOR"),
                    new PasoDeLaCadena(3, "Aprobado", "RECTOR"));
    private static final Map<String, FirmanteDelPeriodo> NOMBRES =
            Map.of(
                    "RESPONSABLE_CONSOLIDACION",
                    FirmanteDelPeriodo.porUsuario("PÉREZ ANA", "docente.demo"),
                    "VICERRECTOR",
                    FirmanteDelPeriodo.porIdentificacion("LÓPEZ LUIS", "0109990028"),
                    "RECTOR",
                    FirmanteDelPeriodo.porIdentificacion("MORA EVA", "0109990010"));

    private final DocumentosEnMemoria documentos = new DocumentosEnMemoria();
    private final ContenidoDelDistributivo contenido = mock(ContenidoDelDistributivo.class);
    private final FirmantesDelPeriodo firmantes = mock(FirmantesDelPeriodo.class);
    private final GeneradorDePdf generador = mock(GeneradorDePdf.class);
    private final AlmacenDeDocumentos almacen = mock(AlmacenDeDocumentos.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final List<String> eventos = new ArrayList<>();
    private boolean dentro;
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    eventos.add("transaccion");
                    dentro = true;
                    try {
                        return trabajo.get();
                    } finally {
                        dentro = false;
                    }
                }
            };
    private final DocumentoFirmableServicio servicio =
            new DocumentoFirmableServicio(
                    documentos,
                    contenido,
                    firmantes,
                    generador,
                    almacen,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void conTodo() {
        when(contenido.contenido(PERIODO))
                .thenAnswer(
                        uso -> {
                            eventos.add(dentro ? "contenido dentro" : "contenido");
                            return contenidoDe(1, HASH);
                        });
        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenAnswer(
                        uso -> {
                            eventos.add(dentro ? "firmantes dentro" : "firmantes");
                            return NOMBRES;
                        });
        when(generador.generar(any())).thenReturn(PDF);
        when(almacen.guardar(any(), anyString()))
                .thenAnswer(uso -> "documentos/" + uso.getArgument(1) + ".pdf");
    }

    @Test
    void dadaLaSolicitud_cuandoSePrepara_entoncesGeneraGuardaPorElHashYRegistraUnaVez() {
        servicio.preparar(solicitud());

        // research §8: la generación EN_CURSO se confirma; el contenido y los firmantes, fuera.
        assertThat(eventos).containsExactly("transaccion", "contenido", "firmantes", "transaccion");
        String sha = sha256(PDF);
        verify(almacen).guardar(PDF, sha);
        DocumentoFirmable documento = documentos.documento(PERIODO, 1).orElseThrow();
        assertThat(documento.hashContenido()).isEqualTo(HASH);
        assertThat(documento.sha256Original()).isEqualTo(sha);
        assertThat(documento.archivo()).isEqualTo("documentos/" + sha + ".pdf");
        assertThat(documento.tamano()).isEqualTo(PDF.length);
        assertThat(documento.generadoPor()).isEqualTo("sistema");
        assertThat(documento.generadoEn()).isEqualTo(AHORA);
        assertThat(documento.tipo()).isEqualTo("CONSOLIDADO");
        GeneracionDeDocumento generacion = documentos.generacion(PERIODO, 1);
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.GENERADA);
        assertThat(generacion.rolDeLaEtapa()).isEqualTo("RESPONSABLE_CONSOLIDACION");
        CambioDeFirma cambio = registrado().getFirst();
        assertThat(registrado()).hasSize(1);
        assertThat(cambio.accion()).isEqualTo("DOCUMENTO_FIRMABLE_GENERADO");
        assertThat(cambio.usuario()).isEqualTo("sistema");
        assertThat(cambio.detalle())
                .isEqualTo(
                        Map.of(
                                "periodo",
                                PERIODO,
                                "version",
                                1,
                                "hashContenido",
                                HASH,
                                "sha256",
                                sha));
        assertThat(cambio.nuevo()).isEqualTo(Map.of("estado", "GENERADO"));
    }

    @Test
    void dadoElContenido_cuandoSeGenera_entoncesLlevaLosFirmantesDeLaCadenaEnOrden() {
        servicio.preparar(solicitud());

        ArgumentCaptor<ContenidoDelDocumento> generado =
                ArgumentCaptor.forClass(ContenidoDelDocumento.class);
        verify(generador).generar(generado.capture());
        assertThat(generado.getValue().firmantes())
                .extracting(f -> f.rotulo() + "|" + f.nombre() + "|" + f.campo())
                .containsExactly(
                        "Elaborado por|PÉREZ ANA|firma-1-RESPONSABLE_CONSOLIDACION",
                        "Revisado por|LÓPEZ LUIS|firma-2-VICERRECTOR",
                        "Aprobado por|MORA EVA|firma-3-RECTOR");
        assertThat(generado.getValue().hashContenido()).isEqualTo(HASH);
    }

    @Test
    void dadaUnaVersionYaGenerada_cuandoSePreparaOtraVez_entoncesNoHaceNada() {
        servicio.preparar(solicitud());
        org.mockito.Mockito.clearInvocations(generador, almacen, registro, contenido);

        servicio.preparar(solicitud());

        verify(contenido, never()).contenido(anyString());
        verify(generador, never()).generar(any());
        verify(almacen, never()).guardar(any(), anyString());
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoUnContenidoDeOtraVersion_cuandoSePrepara_entoncesFallaConEseMotivoSinDocumento() {
        when(contenido.contenido(PERIODO)).thenReturn(contenidoDe(2, HASH));

        servicio.preparar(solicitud());

        GeneracionDeDocumento generacion = documentos.generacion(PERIODO, 1);
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(generacion.motivo())
                .contains("El contenido de Generación no es el de la versión 1 que se congeló.");
        assertThat(documentos.documento(PERIODO, 1)).isEmpty();
        verify(generador, never()).generar(any());
        assertThat(registrado()).isEmpty();
    }

    // El reintento y el estado (US4, FR-011 a FR-015).

    private static final Solicitante RESPONSABLE =
            new Solicitante("consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"));
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"));

    private void sinNombreDelRector() {
        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenReturn(
                        Map.of(
                                "RESPONSABLE_CONSOLIDACION",
                                NOMBRES.get("RESPONSABLE_CONSOLIDACION"),
                                "VICERRECTOR",
                                NOMBRES.get("VICERRECTOR")));
    }

    @Test
    void dadoUnFirmanteSinNombre_cuandoSePrepara_entoncesFallidaConElMotivoSinRelanzar() {
        sinNombreDelRector();

        servicio.preparar(solicitud());

        GeneracionDeDocumento generacion = documentos.generacion(PERIODO, 1);
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(generacion.motivo()).contains("Falta el nombre del Rector.");
        assertThat(documentos.documento(PERIODO, 1)).isEmpty();
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoElDocumentoGenerado_cuandoSeRegistra_entoncesLlevaElFirmanteEsperadoDeCadaPaso() {
        servicio.preparar(solicitud());

        DocumentoFirmable documento = documentos.documento(PERIODO, 1).orElseThrow();
        assertThat(documentos.firmantesDe(documento.id()))
                .containsExactly(
                        FirmanteEsperado.porUsuario(
                                1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "PÉREZ ANA"),
                        FirmanteEsperado.porIdentificacion(
                                2, "VICERRECTOR", "0109990028", "LÓPEZ LUIS"),
                        FirmanteEsperado.porIdentificacion(3, "RECTOR", "0109990010", "MORA EVA"));
    }

    @Test
    void dadoUnFirmanteConNombrePeroSinPersona_cuandoSePrepara_entoncesFallaComoSinNombre() {
        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenReturn(
                        Map.of(
                                "RESPONSABLE_CONSOLIDACION",
                                NOMBRES.get("RESPONSABLE_CONSOLIDACION"),
                                "VICERRECTOR",
                                NOMBRES.get("VICERRECTOR"),
                                "RECTOR",
                                new FirmanteDelPeriodo(
                                        "MORA EVA", Optional.empty(), Optional.empty())));

        servicio.preparar(solicitud());

        GeneracionDeDocumento generacion = documentos.generacion(PERIODO, 1);
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(generacion.motivo()).contains("Falta el nombre del Rector.");
        assertThat(documentos.documento(PERIODO, 1)).isEmpty();
    }

    @Test
    void dadoUnFalloDelAlmacen_cuandoSePrepara_entoncesFallidaConUnMotivoSinDetallesInternos() {
        when(almacen.guardar(any(), anyString()))
                .thenThrow(new java.io.UncheckedIOException(new java.io.IOException("/var/mdd")));

        servicio.preparar(solicitud());

        GeneracionDeDocumento generacion = documentos.generacion(PERIODO, 1);
        assertThat(generacion.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(generacion.motivo().orElseThrow())
                .startsWith("No se pudo generar el PDF.")
                .doesNotContain("/var/mdd");
    }

    @Test
    void dadaUnaGeneracionFallida_cuandoElResponsableReintentaYYaEstaCorregida_entoncesGenerado() {
        sinNombreDelRector();
        servicio.preparar(solicitud());
        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenReturn(NOMBRES);

        EstadoDelDocumento estado = servicio.generar(PERIODO, RESPONSABLE);

        assertThat(estado.estado()).isEqualTo(EstadoDelDocumento.Estado.GENERADO);
        assertThat(estado.documento().orElseThrow().generadoPor()).isEqualTo("consolidacion.demo");
        assertThat(documentos.documento(PERIODO, 1).orElseThrow().generadoPor())
                .isEqualTo("consolidacion.demo");
        assertThat(registrado()).hasSize(1);
        assertThat(registrado().getFirst().usuario()).isEqualTo("consolidacion.demo");
    }

    @Test
    void
            dadaUnaGeneracionQueSigueFallando_cuandoSeReintenta_entoncesGeneracionFallidaConElMotivo() {
        sinNombreDelRector();
        servicio.preparar(solicitud());

        assertThatThrownBy(() -> servicio.generar(PERIODO, RESPONSABLE))
                .isInstanceOfSatisfying(
                        GeneracionFallida.class,
                        error ->
                                assertThat(error.motivo())
                                        .isEqualTo("Falta el nombre del Rector."));
        assertThat(documentos.generacion(PERIODO, 1).estado())
                .isEqualTo(EstadoDeGeneracion.FALLIDA);
    }

    @Test
    void dadoElDocumentoYaGenerado_cuandoSePideOtraVez_entoncesElMismoSinOtraEntrada() {
        servicio.preparar(solicitud());
        org.mockito.Mockito.clearInvocations(generador, registro);

        EstadoDelDocumento estado = servicio.generar(PERIODO, RESPONSABLE);

        assertThat(estado.estado()).isEqualTo(EstadoDelDocumento.Estado.GENERADO);
        assertThat(estado.documento().orElseThrow().generadoPor()).isEqualTo("sistema");
        verify(generador, never()).generar(any());
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoOtroRol_cuandoPideGenerar_entoncesSinRolDeFirmaSinTocarNada() {
        sinNombreDelRector();
        servicio.preparar(solicitud());
        org.mockito.Mockito.clearInvocations(generador);

        assertThatThrownBy(() -> servicio.generar(PERIODO, VICERRECTOR))
                .isInstanceOf(SinRolDeFirma.class);
        verify(generador, never()).generar(any());
    }

    @Test
    void dadoUnPeriodoQueNoEntroEnFirma_cuandoSePideGenerar_entoncesEtapaNoAdmiteGeneracion() {
        assertThatThrownBy(() -> servicio.generar(PERIODO, RESPONSABLE))
                .isInstanceOf(EtapaNoAdmiteGeneracion.class);
    }

    @Test
    void dadoCadaEstado_cuandoSeConsulta_entoncesLoDiceConSusAccionesSoloParaElRol() {
        EstadoDelDocumento sin = servicio.estado(PERIODO, RESPONSABLE);
        assertThat(sin.estado()).isEqualTo(EstadoDelDocumento.Estado.SIN_GENERAR);
        assertThat(sin.version()).isEmpty();
        assertThat(sin.acciones()).isEmpty();
        assertThat(sin.cadena()).isEmpty();

        sinNombreDelRector();
        servicio.preparar(solicitud());
        EstadoDelDocumento fallida = servicio.estado(PERIODO, RESPONSABLE);
        assertThat(fallida.estado()).isEqualTo(EstadoDelDocumento.Estado.FALLIDA);
        assertThat(fallida.version()).contains(1);
        assertThat(fallida.motivo()).contains("Falta el nombre del Rector.");
        assertThat(fallida.acciones()).containsExactly("GENERAR");
        assertThat(fallida.cadena()).isEmpty();
        assertThat(servicio.estado(PERIODO, VICERRECTOR).acciones()).isEmpty();

        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenReturn(NOMBRES);
        servicio.generar(PERIODO, RESPONSABLE);
        EstadoDelDocumento generado = servicio.estado(PERIODO, VICERRECTOR);
        assertThat(generado.estado()).isEqualTo(EstadoDelDocumento.Estado.GENERADO);
        assertThat(generado.documento().orElseThrow().sha256()).isEqualTo(sha256(PDF));
        assertThat(generado.documento().orElseThrow().hashContenido()).isEqualTo(HASH);
        assertThat(generado.motivo()).isEmpty();
        assertThat(servicio.estado(PERIODO, RESPONSABLE).acciones()).isEmpty();
        // F16-H02: la cadena con su estado, con los nombres impresos y sin identificaciones.
        assertThat(generado.cadena())
                .containsExactly(
                        new EstadoDelDocumento.Paso(
                                1,
                                "Elaborado por",
                                Optional.of("PÉREZ ANA"),
                                "EN_TURNO",
                                Optional.empty()),
                        new EstadoDelDocumento.Paso(
                                2,
                                "Revisado por",
                                Optional.of("LÓPEZ LUIS"),
                                "PENDIENTE",
                                Optional.empty()),
                        new EstadoDelDocumento.Paso(
                                3,
                                "Aprobado por",
                                Optional.of("MORA EVA"),
                                "PENDIENTE",
                                Optional.empty()));
    }

    @Test
    void dadoUnDocumentoConLasFirmasAnuladas_cuandoSeConsulta_entoncesSinCadena() {
        when(firmantes.firmantes(
                        PERIODO, Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR")))
                .thenReturn(NOMBRES);
        servicio.preparar(solicitud());
        documentos.anular(
                new ec.mdd.firma.dominio.AnulacionDeFirmas(
                        "RECHAZO_EN_FIRMA", 1, "sistema", ENTRO.plusSeconds(60)));

        EstadoDelDocumento anulado = servicio.estado(PERIODO, VICERRECTOR);

        assertThat(anulado.estado()).isEqualTo(EstadoDelDocumento.Estado.GENERADO);
        assertThat(anulado.anulacion()).isPresent();
        assertThat(anulado.cadena()).isEmpty();
    }

    // F19-H02 (D-120): la cadena de una versión, para la ruta interna de Reporting.
    @Test
    void dadoElDocumentoDeLaVersion_cuandoSePidenSusFirmas_entoncesDaLasDeEsaVersion() {
        servicio.preparar(solicitud());

        assertThat(servicio.firmas(PERIODO, 1))
                .hasValueSatisfying(
                        firmas -> {
                            assertThat(firmas.documento().version()).isEqualTo(1);
                            assertThat(firmas.documento().hashContenido()).isEqualTo(HASH);
                            assertThat(firmas.cadena()).hasSize(3);
                        });
    }

    @Test
    void
            dadaUnaVersionNuevaGenerada_cuandoSePidenLasFirmasDeLaAnterior_entoncesDaLasDeLaAnterior() {
        servicio.preparar(solicitud());
        String otro = "f".repeat(64);
        when(contenido.contenido(PERIODO)).thenReturn(contenidoDe(2, otro));
        servicio.preparar(
                new SolicitudDeDocumento(
                        PERIODO, 2, otro, CADENA, "RESPONSABLE_CONSOLIDACION", ENTRO));

        assertThat(servicio.firmas(PERIODO, 1).orElseThrow().documento().hashContenido())
                .isEqualTo(HASH);
        assertThat(servicio.firmas(PERIODO, 2).orElseThrow().documento().hashContenido())
                .isEqualTo(otro);
    }

    @Test
    void dadaUnaVersionSinDocumento_cuandoSePidenSusFirmas_entoncesVacio() {
        assertThat(servicio.firmas(PERIODO, 1)).isEmpty();
    }

    private static SolicitudDeDocumento solicitud() {
        return new SolicitudDeDocumento(
                PERIODO, 1, HASH, CADENA, "RESPONSABLE_CONSOLIDACION", ENTRO);
    }

    private static ContenidoDelDocumento contenidoDe(int version, String hash) {
        ContenidoDelDocumento.Horas horas =
                new ContenidoDelDocumento.Horas(
                        new BigDecimal("4"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        return new ContenidoDelDocumento(
                PERIODO,
                version,
                hash,
                List.of(new ContenidoDelDocumento.Carrera("TDS", "ENVIADO", 1, 1, horas)),
                horas,
                List.of(),
                List.of());
    }

    private List<CambioDeFirma> registrado() {
        ArgumentCaptor<CambioDeFirma> cambios = ArgumentCaptor.forClass(CambioDeFirma.class);
        verify(registro, org.mockito.Mockito.atLeast(0)).registrar(cambios.capture());
        return cambios.getAllValues();
    }

    static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    /** Las generaciones y los documentos en memoria. */
    static final class DocumentosEnMemoria implements RepositorioDocumentos {

        private final Map<String, GeneracionDeDocumento> generaciones = new TreeMap<>();
        private final Map<String, DocumentoFirmable> registrados = new TreeMap<>();
        private final Map<UUID, List<FirmanteEsperado>> firmantes = new java.util.HashMap<>();
        private ec.mdd.firma.dominio.AnulacionDeFirmas anulada;

        /** Anula las firmas de los documentos, como F12-H04 (D-106). */
        void anular(ec.mdd.firma.dominio.AnulacionDeFirmas anulacion) {
            anulada = anulacion;
        }

        @Override
        public Optional<GeneracionDeDocumento> bloquear(String periodo, int version) {
            return Optional.ofNullable(generaciones.get(periodo + "/" + version)).map(this::copia);
        }

        @Override
        public Optional<GeneracionDeDocumento> ultima(String periodo) {
            return generaciones.values().stream()
                    .filter(una -> una.periodo().equals(periodo))
                    .max(java.util.Comparator.comparingInt(GeneracionDeDocumento::version))
                    .map(this::copia);
        }

        @Override
        public void guardar(GeneracionDeDocumento generacion) {
            generaciones.put(generacion.periodo() + "/" + generacion.version(), copia(generacion));
        }

        @Override
        public void registrar(DocumentoFirmable documento, List<FirmanteEsperado> esperados) {
            if (registrados.putIfAbsent(documento.periodo() + "/" + documento.version(), documento)
                    != null) {
                throw new IllegalStateException("Ya hay un documento de esa versión");
            }
            firmantes.put(documento.id(), List.copyOf(esperados));
        }

        // Como RepositorioDocumentosJdbc: solo los de una generación GENERADA.
        @Override
        public List<FirmasDelDocumento> generados() {
            return registrados.values().stream()
                    .flatMap(documento -> firmas(documento.id()).stream())
                    .toList();
        }

        @Override
        public Optional<FirmasDelDocumento> firmas(UUID documento) {
            return registrados.values().stream()
                    .filter(uno -> uno.id().equals(documento))
                    .filter(
                            uno ->
                                    generacion(uno.periodo(), uno.version()).estado()
                                            == EstadoDeGeneracion.GENERADA)
                    .findFirst()
                    .map(
                            uno ->
                                    new FirmasDelDocumento(
                                            uno,
                                            generacion(uno.periodo(), uno.version()).cadena(),
                                            firmantes.getOrDefault(uno.id(), List.of()),
                                            List.of(),
                                            Optional.empty(),
                                            Optional.ofNullable(anulada)));
        }

        /** Registra un documento de antes de F14-H01, sin firmantes esperados. */
        void registrarSinFirmantes(DocumentoFirmable documento) {
            registrados.put(documento.periodo() + "/" + documento.version(), documento);
        }

        List<FirmanteEsperado> firmantesDe(UUID documento) {
            return firmantes.getOrDefault(documento, List.of());
        }

        @Override
        public Optional<DocumentoFirmable> documento(String periodo, int version) {
            return Optional.ofNullable(registrados.get(periodo + "/" + version));
        }

        @Override
        public boolean registrarValidacion(
                ResultadoValidacion resultado, FirmasDelDocumento validadoContra) {
            // La generación del documento no registra firmas (F14-H02 las prueba aparte).
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean registrarRechazo(RechazoDelFirmante rechazo, FirmasDelDocumento vistas) {
            // La generación del documento no registra rechazos (F14-H04 los prueba aparte).
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ec.mdd.firma.dominio.AnulacionDeFirmas> registrarAnulacion(
                DocumentoFirmable documento,
                String motivo,
                String quien,
                java.time.Instant momento) {
            // La generación del documento no anula firmas (F12-H04 lo prueba aparte).
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ResultadoValidacion> validaciones(UUID documento) {
            throw new UnsupportedOperationException();
        }

        GeneracionDeDocumento generacion(String periodo, int version) {
            return generaciones.get(periodo + "/" + version);
        }

        private GeneracionDeDocumento copia(GeneracionDeDocumento una) {
            return GeneracionDeDocumento.de(
                    una.periodo(),
                    una.version(),
                    una.hashContenido(),
                    una.cadena(),
                    una.rolDeLaEtapa(),
                    una.estado(),
                    una.motivo(),
                    una.pedidaPor(),
                    una.actualizadaEn());
        }
    }
}
