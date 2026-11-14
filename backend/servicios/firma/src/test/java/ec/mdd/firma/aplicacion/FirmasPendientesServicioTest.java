package ec.mdd.firma.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.FirmaPendiente;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Las firmas pendientes y la descarga del archivo vigente (F14-H01, US1 y US2): solo el firmante
 * esperado del paso en turno, reconocido por su usuario o por su identificación; el rol no basta.
 */
class FirmasPendientesServicioTest {

    private static final Instant GENERADO = Instant.parse("2026-11-14T08:43:00Z");
    private static final byte[] PDF = "%PDF-1.7 consolidado".getBytes(StandardCharsets.US_ASCII);
    private static final String SHA = sha256(PDF);
    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                    new PasoDeFirma(3, "Aprobado", "RECTOR"));
    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(1, "RESPONSABLE_CONSOLIDACION", "docente.demo"),
                    FirmanteEsperado.porIdentificacion(2, "VICERRECTOR", "0109990028"),
                    FirmanteEsperado.porIdentificacion(3, "RECTOR", "0109990010"));

    private static final Solicitante DESIGNADO =
            new Solicitante("docente.demo", Set.of("DOCENTE", "RESPONSABLE_CONSOLIDACION"));
    private static final Solicitante OTRO_RESPONSABLE =
            new Solicitante("consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"));
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"));
    private static final Solicitante RECTOR = new Solicitante("rectorado.demo", Set.of("RECTOR"));
    private static final Solicitante OTRO_VICERRECTOR =
            new Solicitante("otro.vicerrector", Set.of("VICERRECTOR"));

    private final DocumentoFirmableServicioTest.DocumentosEnMemoria documentos =
            new DocumentoFirmableServicioTest.DocumentosEnMemoria();
    private final AlmacenDeDocumentos almacen = mock(AlmacenDeDocumentos.class);
    private final IdentificacionDeUsuarios identificaciones = mock(IdentificacionDeUsuarios.class);
    private final FirmasPendientesServicio servicio =
            new FirmasPendientesServicio(documentos, almacen, identificaciones);

    @BeforeEach
    void conIdentificaciones() {
        when(identificaciones.de(anyString())).thenReturn(Optional.empty());
        when(identificaciones.de("vicerrectorado.demo")).thenReturn(Optional.of("0109990028"));
        when(identificaciones.de("rectorado.demo")).thenReturn(Optional.of("0109990010"));
        when(identificaciones.de("otro.vicerrector")).thenReturn(Optional.of("0999999999"));
        when(almacen.leer("documentos/" + SHA + ".pdf")).thenReturn(PDF);
    }

    // US1: el firmante en turno ve su documento y lo descarga.

    @Test
    void dadoElDesignadoEnTurno_cuandoPideSusPendientes_entoncesVeUnaFilaConElPaso() {
        DocumentoFirmable documento = generado("2026-I", 1, CADENA, FIRMANTES);

        assertThat(servicio.pendientes(DESIGNADO))
                .containsExactly(
                        new FirmaPendiente(
                                documento.id(), "2026-I", 1, 1, "Elaborado por", GENERADO));
        verify(identificaciones, never()).de(anyString());
    }

    @Test
    void dadosVariosPeriodosEnFirma_cuandoPideSusPendientes_entoncesUnaFilaPorDocumento() {
        generado("2026-II", 1, CADENA, FIRMANTES);
        generado("2026-I", 2, CADENA, FIRMANTES);

        assertThat(servicio.pendientes(DESIGNADO))
                .extracting(FirmaPendiente::periodo, FirmaPendiente::version)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2026-I", 2),
                        org.assertj.core.groups.Tuple.tuple("2026-II", 1));
    }

    @Test
    void dadoQueNoHayDocumentos_cuandoPideSusPendientes_entoncesListaVacia() {
        assertThat(servicio.pendientes(DESIGNADO)).isEmpty();
    }

    @Test
    void dadoElDesignadoEnTurno_cuandoDescarga_entoncesElArchivoVerificadoConSuNombre() {
        DocumentoFirmable documento = generado("2026-I", 1, CADENA, FIRMANTES);

        ArchivoVigente archivo = servicio.archivo(documento.id(), DESIGNADO);

        assertThat(archivo.nombre()).isEqualTo("distributivo-2026-I-v1.pdf");
        assertThat(archivo.sha256()).isEqualTo(SHA);
        assertThat(archivo.contenido()).isEqualTo(PDF);
    }

    @Test
    void dadoUnArchivoQueNoCoincideConSuHash_cuandoSeDescarga_entoncesNoDisponibleSinEntregarlo() {
        DocumentoFirmable documento = generado("2026-I", 1, CADENA, FIRMANTES);
        when(almacen.leer("documentos/" + SHA + ".pdf"))
                .thenReturn("%PDF-1.7 otro".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> servicio.archivo(documento.id(), DESIGNADO))
                .isInstanceOf(ArchivoNoDisponible.class);
    }

    @Test
    void dadoUnArchivoQueFalta_cuandoSeDescarga_entoncesNoDisponible() {
        DocumentoFirmable documento = generado("2026-I", 1, CADENA, FIRMANTES);
        when(almacen.leer("documentos/" + SHA + ".pdf"))
                .thenThrow(new UncheckedIOException(new IOException("/var/mdd")));

        assertThatThrownBy(() -> servicio.archivo(documento.id(), DESIGNADO))
                .isInstanceOf(ArchivoNoDisponible.class);
    }

    // US2: quien no está en turno no lo ve ni lo descarga.

    @Test
    void dadoElTurnoEnF1_cuandoPidenLosDemas_entoncesNadaYElMismo403() {
        DocumentoFirmable documento = generado("2026-I", 1, CADENA, FIRMANTES);

        for (Solicitante quien : List.of(VICERRECTOR, RECTOR, OTRO_RESPONSABLE)) {
            assertThat(servicio.pendientes(quien)).as(quien.usuario()).isEmpty();
            assertThatThrownBy(() -> servicio.archivo(documento.id(), quien))
                    .as(quien.usuario())
                    .isInstanceOf(SinTurnoDeFirma.class);
        }
        verify(almacen, never()).leer(anyString());
    }

    @Test
    void dadoUnDocumentoInexistenteOSinGenerar_cuandoSePide_entoncesNoApareceY403() {
        guardarGeneracion("2026-I", 1, CADENA);
        DocumentoFirmable fallido = documento("2026-I", 1);
        documentos.registrar(fallido, FIRMANTES);
        GeneracionDeDocumento generacion = documentos.generacion("2026-I", 1);
        generacion.fallar("Falta el nombre del Rector.", GENERADO);
        documentos.guardar(generacion);

        assertThat(servicio.pendientes(DESIGNADO)).isEmpty();
        assertThatThrownBy(() -> servicio.archivo(fallido.id(), DESIGNADO))
                .isInstanceOf(SinTurnoDeFirma.class);
        assertThatThrownBy(() -> servicio.archivo(UUID.randomUUID(), DESIGNADO))
                .isInstanceOf(SinTurnoDeFirma.class);
    }

    @Test
    void dadoUnDocumentoDeAntesSinFirmantesEsperados_cuandoSePide_entoncesNoEsDeNadie() {
        guardarGeneracion("2026-I", 1, CADENA);
        GeneracionDeDocumento generacion = documentos.generacion("2026-I", 1);
        generacion.completar(GENERADO);
        documentos.guardar(generacion);
        DocumentoFirmable viejo = documento("2026-I", 1);
        documentos.registrarSinFirmantes(viejo);

        assertThat(servicio.pendientes(DESIGNADO)).isEmpty();
        assertThatThrownBy(() -> servicio.archivo(viejo.id(), DESIGNADO))
                .isInstanceOf(SinTurnoDeFirma.class);
    }

    @Test
    void dadoUnPasoDeAutoridadEnTurno_cuandoPiden_entoncesSoloLaCuentaConSuIdentificacion() {
        // Un árbol configurado con el Vicerrector primero (RF-58): su paso está en turno.
        List<PasoDeFirma> cadena = List.of(new PasoDeFirma(1, "Revisado", "VICERRECTOR"));
        DocumentoFirmable documento =
                generado(
                        "2026-I",
                        1,
                        cadena,
                        List.of(
                                FirmanteEsperado.porIdentificacion(
                                        1, "VICERRECTOR", "0109990028")));

        assertThat(servicio.pendientes(VICERRECTOR))
                .singleElement()
                .extracting(FirmaPendiente::rotulo)
                .isEqualTo("Revisado por");
        assertThat(servicio.archivo(documento.id(), VICERRECTOR).sha256()).isEqualTo(SHA);
        assertThat(servicio.pendientes(OTRO_VICERRECTOR)).isEmpty();
        assertThat(servicio.pendientes(DESIGNADO)).isEmpty();
        assertThatThrownBy(() -> servicio.archivo(documento.id(), OTRO_VICERRECTOR))
                .isInstanceOf(SinTurnoDeFirma.class);
    }

    @Test
    void dadosVariosDocumentosPorIdentificacion_cuandoPideSusPendientes_entoncesLaPideUnaVez() {
        List<PasoDeFirma> cadena = List.of(new PasoDeFirma(1, "Revisado", "VICERRECTOR"));
        List<FirmanteEsperado> firmantes =
                List.of(FirmanteEsperado.porIdentificacion(1, "VICERRECTOR", "0109990028"));
        generado("2026-I", 1, cadena, firmantes);
        generado("2026-II", 1, cadena, firmantes);

        assertThat(servicio.pendientes(VICERRECTOR)).hasSize(2);
        verify(identificaciones, times(1)).de("vicerrectorado.demo");
    }

    private DocumentoFirmable generado(
            String periodo,
            int version,
            List<PasoDeFirma> cadena,
            List<FirmanteEsperado> firmantes) {
        guardarGeneracion(periodo, version, cadena);
        GeneracionDeDocumento generacion = documentos.generacion(periodo, version);
        generacion.completar(GENERADO);
        documentos.guardar(generacion);
        DocumentoFirmable documento = documento(periodo, version);
        documentos.registrar(documento, firmantes);
        return documento;
    }

    private void guardarGeneracion(String periodo, int version, List<PasoDeFirma> cadena) {
        documentos.guardar(
                GeneracionDeDocumento.nueva(
                        periodo,
                        version,
                        "e".repeat(64),
                        cadena,
                        "RESPONSABLE_CONSOLIDACION",
                        "sistema",
                        GENERADO));
    }

    private static DocumentoFirmable documento(String periodo, int version) {
        return new DocumentoFirmable(
                UUID.randomUUID(),
                periodo,
                version,
                DocumentoFirmable.CONSOLIDADO,
                "e".repeat(64),
                SHA,
                "documentos/" + SHA + ".pdf",
                PDF.length,
                "sistema",
                GENERADO);
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
