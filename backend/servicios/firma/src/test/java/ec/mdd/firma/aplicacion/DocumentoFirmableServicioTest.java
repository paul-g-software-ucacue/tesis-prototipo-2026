package ec.mdd.firma.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.puertos.entrada.PasoDeLaCadena;
import ec.mdd.firma.puertos.entrada.SolicitudDeDocumento;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
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
    private static final Map<String, String> NOMBRES =
            Map.of(
                    "RESPONSABLE_CONSOLIDACION", "PÉREZ ANA",
                    "VICERRECTOR", "LÓPEZ LUIS",
                    "RECTOR", "MORA EVA");

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
        when(firmantes.nombres(
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
        public void registrar(DocumentoFirmable documento) {
            if (registrados.putIfAbsent(documento.periodo() + "/" + documento.version(), documento)
                    != null) {
                throw new IllegalStateException("Ya hay un documento de esa versión");
            }
        }

        @Override
        public Optional<DocumentoFirmable> documento(String periodo, int version) {
            return Optional.ofNullable(registrados.get(periodo + "/" + version));
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
