package ec.mdd.firma.aplicacion;

import static ec.mdd.firma.dominio.Comprobacion.CONFIANZA;
import static ec.mdd.firma.dominio.Comprobacion.INTEGRIDAD;
import static ec.mdd.firma.dominio.Comprobacion.MISMO_DOCUMENTO;
import static ec.mdd.firma.dominio.Comprobacion.UNA_FIRMA_NUEVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmaRegistrada;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.PasoSinNombre;
import ec.mdd.firma.dominio.Resultado;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.AvisoDeCadenaCompleta;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas.HallazgosDeLaFirma;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La subida del PDF firmado (F14-H02, D-103): solo el firmante en turno, las comprobaciones en
 * orden, el registro con su bitácora en una transacción y los rechazos sin guardar el archivo.
 */
class SubirFirmaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T10:00:00Z");
    private static final byte[] VIGENTE =
            "%PDF-1.7 vigente %%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIRMADO =
            "%PDF-1.7 vigente %%EOF firma nueva %%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final CertificadoDeLaFirma CERTIFICADO =
            new CertificadoDeLaFirma(
                    "Docente (demo)",
                    "CN=CA de Prueba MDD Demo,O=MDD Demo,C=EC",
                    "1a2b",
                    AHORA.minusSeconds(86_400),
                    AHORA.plusSeconds(86_400 * 365L));
    private static final Solicitante DESIGNADO = new Solicitante("docente.demo", Set.of("DOCENTE"));
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"));

    private final RepositorioDocumentos documentos = mock(RepositorioDocumentos.class);
    private final AlmacenDeDocumentos almacen = mock(AlmacenDeDocumentos.class);
    private final IdentificacionDeUsuarios identificaciones = mock(IdentificacionDeUsuarios.class);
    private final ValidadorDeFirmas validador = mock(ValidadorDeFirmas.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final TransaccionDePrueba transaccion = new TransaccionDePrueba();
    private final AvisoDeCadenaCompleta aviso = mock(AvisoDeCadenaCompleta.class);

    private final SubirFirmaServicio servicio =
            new SubirFirmaServicio(
                    documentos,
                    almacen,
                    identificaciones,
                    validador,
                    registro,
                    transaccion,
                    aviso,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    private final DocumentoFirmable documento =
            new DocumentoFirmable(
                    UUID.randomUUID(),
                    "2026-I",
                    1,
                    DocumentoFirmable.CONSOLIDADO,
                    "a".repeat(64),
                    sha256(VIGENTE),
                    "documentos/" + sha256(VIGENTE) + ".pdf",
                    VIGENTE.length,
                    "sistema",
                    AHORA.minusSeconds(3600));

    /** Ejecuta el trabajo y anota si se está dentro de la transacción. */
    private static final class TransaccionDePrueba implements Transaccion {
        private boolean dentro;
        private int veces;

        @Override
        public <T> T ejecutar(Supplier<T> trabajo) {
            dentro = true;
            veces++;
            try {
                return trabajo.get();
            } finally {
                dentro = false;
            }
        }
    }

    private FirmasDelDocumento firmas(FirmaRegistrada... registradas) {
        return new FirmasDelDocumento(
                documento,
                List.of(
                        new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                        new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                        new PasoDeFirma(3, "Aprobado", "RECTOR")),
                List.of(
                        FirmanteEsperado.porUsuario(
                                1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Docente (demo)"),
                        FirmanteEsperado.porIdentificacion(
                                2, "VICERRECTOR", "0109990028", "CABRERA VAZQUEZ ANA GABRIELA"),
                        FirmanteEsperado.porIdentificacion(
                                3, "RECTOR", "0109990010", "PERALTA MALDONADO JUAN ANDRES")),
                List.of(registradas));
    }

    private void enF1() {
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas()));
        when(almacen.leer(documento.archivo())).thenReturn(VIGENTE.clone());
    }

    private static HallazgosDeLaFirma aprobados() {
        return aprobados(CERTIFICADO);
    }

    private static HallazgosDeLaFirma aprobados(CertificadoDeLaFirma certificado) {
        return new HallazgosDeLaFirma(
                List.of(
                        ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                        ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                        ResultadoDeComprobacion.aprobada(CONFIANZA)),
                Optional.of(certificado));
    }

    private static CertificadoDeLaFirma de(String cn) {
        return new CertificadoDeLaFirma(
                cn,
                CERTIFICADO.emisor(),
                CERTIFICADO.serie(),
                CERTIFICADO.validoDesde(),
                CERTIFICADO.validoHasta());
    }

    private CambioDeFirma cambioPublicado() {
        ArgumentCaptor<CambioDeFirma> cambio = ArgumentCaptor.forClass(CambioDeFirma.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }

    private ResultadoValidacion validacionRegistrada() {
        ArgumentCaptor<ResultadoValidacion> resultado =
                ArgumentCaptor.forClass(ResultadoValidacion.class);
        verify(documentos).registrarValidacion(resultado.capture(), any());
        return resultado.getValue();
    }

    // --- US1: la firma en turno se registra.

    @Test
    void dadoElFirmanteEnTurnoConUnaFirmaValida_cuandoSube_entoncesSeGuardaSeRegistraYSePublica() {
        enF1();
        when(validador.validar(any(), any())).thenReturn(aprobados());
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        ResultadoValidacion resultado = servicio.subir(documento.id(), FIRMADO, DESIGNADO);

        assertThat(resultado.aceptada()).isTrue();
        assertThat(resultado.orden()).isEqualTo(1);
        assertThat(resultado.subidoPor()).isEqualTo("docente.demo");
        assertThat(resultado.subidoEn()).isEqualTo(AHORA);
        assertThat(resultado.sha256Subido()).isEqualTo(sha256(FIRMADO));
        assertThat(resultado.tamano()).isEqualTo(FIRMADO.length);
        assertThat(resultado.certificado()).contains(CERTIFICADO);
        verify(almacen).guardar(FIRMADO, sha256(FIRMADO));
        assertThat(validacionRegistrada()).isEqualTo(resultado);
        CambioDeFirma cambio = cambioPublicado();
        assertThat(cambio.accion()).isEqualTo(CambioDeFirma.FIRMA_ACEPTADA);
        assertThat(cambio.usuario()).isEqualTo("docente.demo");
        assertThat(cambio.momento()).isEqualTo(AHORA);
        assertThat(cambio.detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1)
                .containsEntry("orden", 1)
                .containsEntry("sha256Subido", sha256(FIRMADO))
                .containsEntry("certificado", "Docente (demo)")
                .containsEntry("serie", "1a2b")
                .containsEntry(
                        "comprobaciones",
                        Map.of(
                                "MISMO_DOCUMENTO", "APROBADA",
                                "UNA_FIRMA_NUEVA", "APROBADA",
                                "INTEGRIDAD", "APROBADA",
                                "CONFIANZA", "APROBADA",
                                "VIGENCIA", "APROBADA",
                                "ORDEN", "APROBADA",
                                "FIRMANTE", "APROBADA"));
        assertThat(cambio.nuevo()).containsEntry("sha256Vigente", sha256(FIRMADO));
        assertThat(transaccion.veces).isEqualTo(1);
    }

    @Test
    void
            dadoElSegundoPaso_cuandoSubeElVicerrector_entoncesLaIdentificacionSePideFueraDeLaTransaccion() {
        FirmaRegistrada primera =
                new FirmaRegistrada(1, sha256(VIGENTE), VIGENTE.length, "docente.demo", AHORA);
        DocumentoFirmable conPrimera = documento;
        when(documentos.firmas(conPrimera.id())).thenReturn(Optional.of(firmas(primera)));
        when(almacen.leer("documentos/" + sha256(VIGENTE) + ".pdf")).thenReturn(VIGENTE.clone());
        when(identificaciones.de("vicerrectorado.demo"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(transaccion.dentro).isFalse();
                            return Optional.of("0109990028");
                        });
        when(validador.validar(any(), any()))
                .thenReturn(aprobados(de("CABRERA VAZQUEZ ANA GABRIELA")));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        ResultadoValidacion resultado = servicio.subir(documento.id(), FIRMADO, VICERRECTOR);

        assertThat(resultado.orden()).isEqualTo(2);
        verify(identificaciones, times(1)).de("vicerrectorado.demo");
    }

    @Test
    void dadoUnArchivoVacio_cuandoSube_entoncesSeRechazaSinValidarNiRegistrar() {
        enF1();

        assertThatThrownBy(() -> servicio.subir(documento.id(), new byte[0], DESIGNADO))
                .isInstanceOf(ArchivoVacio.class);
        verifyNoInteractions(validador, registro);
        verify(documentos, never()).registrarValidacion(any(), any());
    }

    @Test
    void dadoUnArchivoDeMasDeDiezMegas_cuandoSube_entoncesSeRechazaSinValidarNiRegistrar() {
        enF1();

        assertThatThrownBy(
                        () ->
                                servicio.subir(
                                        documento.id(),
                                        new byte[ArchivoDemasiadoGrande.LIMITE_EN_BYTES + 1],
                                        DESIGNADO))
                .isInstanceOf(ArchivoDemasiadoGrande.class);
        verifyNoInteractions(validador, registro);
        verify(documentos, never()).registrarValidacion(any(), any());
    }

    @Test
    void dadoElVigenteQueFaltaEnElAlmacen_cuandoSube_entoncesArchivoNoDisponibleSinRegistrar() {
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas()));
        when(almacen.leer(anyString()))
                .thenThrow(new UncheckedIOException(new java.io.IOException()));

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoNoDisponible.class);
        verifyNoInteractions(validador, registro);
        verify(documentos, never()).registrarValidacion(any(), any());
    }

    @Test
    void dadoElVigenteConOtroSha256_cuandoSube_entoncesArchivoNoDisponible() {
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas()));
        when(almacen.leer(anyString())).thenReturn("otro".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoNoDisponible.class);
        verifyNoInteractions(validador, registro);
    }

    // --- US2: los rechazos.

    @Test
    void dadoOtroDocumento_cuandoSube_entoncesNoEsLaVersionVigenteYNoSeLlamaAlValidador() {
        enF1();
        byte[] otro = "%PDF-1.7 otro %%EOF".getBytes(StandardCharsets.US_ASCII);
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.subir(documento.id(), otro, DESIGNADO))
                .isInstanceOf(ArchivoFirmadoRechazado.NoEsLaVersionVigente.class)
                .satisfies(
                        error ->
                                assertThat(
                                                ((ArchivoFirmadoRechazado) error)
                                                        .resultado()
                                                        .primerRechazo())
                                        .contains(MISMO_DOCUMENTO));
        verifyNoInteractions(validador);
        verify(almacen, never()).guardar(any(), anyString());
        assertThat(validacionRegistrada().aceptada()).isFalse();
        assertThat(cambioPublicado().accion()).isEqualTo(CambioDeFirma.FIRMA_RECHAZADA);
    }

    @Test
    void dadoCadaRechazoDelValidador_cuandoSube_entoncesLanzaSuRechazoSinGuardarElArchivo() {
        record Caso(Comprobacion falla, Class<? extends ArchivoFirmadoRechazado> rechazo) {}
        for (Caso caso :
                List.of(
                        new Caso(UNA_FIRMA_NUEVA, ArchivoFirmadoRechazado.SinUnaFirmaNueva.class),
                        new Caso(INTEGRIDAD, ArchivoFirmadoRechazado.NoIntegra.class),
                        new Caso(CONFIANZA, ArchivoFirmadoRechazado.SinConfianza.class))) {
            SubirFirmaServicioTest prueba = new SubirFirmaServicioTest();
            prueba.enF1();
            when(prueba.validador.validar(any(), any()))
                    .thenReturn(hallazgosQueFallanEn(caso.falla()));
            when(prueba.documentos.registrarValidacion(any(), any())).thenReturn(true);

            assertThatThrownBy(
                            () -> prueba.servicio.subir(prueba.documento.id(), FIRMADO, DESIGNADO))
                    .as(caso.falla().name())
                    .isInstanceOf(caso.rechazo());
            verify(prueba.almacen, never()).guardar(any(), anyString());
            ResultadoValidacion registrada = prueba.validacionRegistrada();
            assertThat(registrada.primerRechazo()).contains(caso.falla());
            assertThat(registrada.comprobaciones())
                    .filteredOn(una -> una.comprobacion() == caso.falla())
                    .singleElement()
                    .extracting(ResultadoDeComprobacion::resultado)
                    .isEqualTo(Resultado.RECHAZADA);
            assertThat(registrada.comprobaciones().getLast().resultado())
                    .isEqualTo(Resultado.NO_EVALUADA);
            CambioDeFirma cambio = prueba.cambioPublicado();
            assertThat(cambio.accion()).isEqualTo(CambioDeFirma.FIRMA_RECHAZADA);
            assertThat(cambio.nuevo()).doesNotContainKey("sha256Vigente");
        }
    }

    @Test
    void dadaOtraSubidaQueFirmoAntes_cuandoSeRegistra_entoncesSeRechazaPorNoSerLaVersionVigente() {
        enF1();
        when(validador.validar(any(), any())).thenReturn(aprobados());
        when(documentos.registrarValidacion(any(), any())).thenReturn(false, true);

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoFirmadoRechazado.NoEsLaVersionVigente.class);
        ArgumentCaptor<ResultadoValidacion> registradas =
                ArgumentCaptor.forClass(ResultadoValidacion.class);
        verify(documentos, times(2)).registrarValidacion(registradas.capture(), eq(firmas()));
        assertThat(registradas.getAllValues().getFirst().aceptada()).isTrue();
        assertThat(registradas.getAllValues().getLast().primerRechazo()).contains(MISMO_DOCUMENTO);
        assertThat(cambioPublicado().accion()).isEqualTo(CambioDeFirma.FIRMA_RECHAZADA);
        assertThat(transaccion.veces).isEqualTo(1);
    }

    // --- F14-H03: el paso sin nombre, la vigencia, el orden y el firmante.

    @Test
    void dadoElPasoEnTurnoSinNombreImpreso_cuandoSube_entoncesPasoSinNombreSinValidarNiRegistrar() {
        FirmasDelDocumento sinNombres =
                new FirmasDelDocumento(
                        documento,
                        firmas().cadena(),
                        List.of(
                                FirmanteEsperado.porUsuario(
                                        1, "RESPONSABLE_CONSOLIDACION", "docente.demo")),
                        List.of());
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(sinNombres));

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(PasoSinNombre.class);
        verifyNoInteractions(validador, registro, almacen);
        verify(documentos, never()).registrarValidacion(any(), any());
    }

    @Test
    void dadaLaFirmaDeLaPersonaDeF3EnElTurnoDeF1_cuandoSube_entoncesFueraDeOrdenYRegistrada() {
        enF1();
        when(validador.validar(any(), any()))
                .thenReturn(aprobados(de("Peralta Maldonado Juan Andrés")));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoFirmadoRechazado.FueraDeOrden.class);
        verify(almacen, never()).guardar(any(), anyString());
        assertThat(validacionRegistrada().primerRechazo()).contains(Comprobacion.ORDEN);
        assertThat(cambioPublicado().accion()).isEqualTo(CambioDeFirma.FIRMA_RECHAZADA);
    }

    @Test
    void dadoUnCertificadoVencidoAlSubir_cuandoSube_entoncesNoVigente() {
        enF1();
        when(validador.validar(any(), any()))
                .thenReturn(
                        aprobados(
                                new CertificadoDeLaFirma(
                                        "Docente (demo)",
                                        CERTIFICADO.emisor(),
                                        CERTIFICADO.serie(),
                                        Instant.parse("2025-01-01T00:00:00Z"),
                                        Instant.parse("2025-02-01T00:00:00Z"))));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoFirmadoRechazado.NoVigente.class);
        assertThat(validacionRegistrada().primerRechazo()).contains(Comprobacion.VIGENCIA);
    }

    @Test
    void dadoElCertificadoDeOtraPersona_cuandoSube_entoncesFirmanteEquivocado() {
        enF1();
        when(validador.validar(any(), any())).thenReturn(aprobados(de("Alguien Distinto")));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, DESIGNADO))
                .isInstanceOf(ArchivoFirmadoRechazado.FirmanteEquivocado.class);
        assertThat(validacionRegistrada().comprobaciones())
                .extracting(ResultadoDeComprobacion::resultado)
                .last()
                .isEqualTo(Resultado.RECHAZADA);
    }

    // --- F14-H03, US2: la última firma completa la cadena.

    @Test
    void
            dadaLaTerceraFirmaAceptada_cuandoSeRegistra_entoncesAvisaQueLaCadenaEstaCompletaEnLaTransaccion() {
        FirmaRegistrada primera = new FirmaRegistrada(1, "c".repeat(64), 10, "docente.demo", AHORA);
        FirmaRegistrada segunda =
                new FirmaRegistrada(
                        2, sha256(VIGENTE), VIGENTE.length, "vicerrectorado.demo", AHORA);
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas(primera, segunda)));
        when(almacen.leer("documentos/" + sha256(VIGENTE) + ".pdf")).thenReturn(VIGENTE.clone());
        when(identificaciones.de("rectorado.demo")).thenReturn(Optional.of("0109990010"));
        when(validador.validar(any(), any()))
                .thenReturn(aprobados(de("PERALTA MALDONADO JUAN ANDRES")));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);
        org.mockito.Mockito.doAnswer(
                        invocacion -> {
                            assertThat(transaccion.dentro).isTrue();
                            return null;
                        })
                .when(aviso)
                .avisar(any());

        servicio.subir(
                documento.id(), FIRMADO, new Solicitante("rectorado.demo", Set.of("RECTOR")));

        verify(aviso).avisar(new CadenaDeFirmasCompleta("2026-I", 1, AHORA));
    }

    @Test
    void dadaLaPrimeraFirmaAceptada_cuandoSeRegistra_entoncesNoAvisa() {
        enF1();
        when(validador.validar(any(), any())).thenReturn(aprobados());
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        servicio.subir(documento.id(), FIRMADO, DESIGNADO);

        verifyNoInteractions(aviso);
    }

    @Test
    void dadaLaTerceraFirmaRechazada_cuandoSeRegistra_entoncesNoAvisa() {
        FirmaRegistrada primera = new FirmaRegistrada(1, "c".repeat(64), 10, "docente.demo", AHORA);
        FirmaRegistrada segunda =
                new FirmaRegistrada(
                        2, sha256(VIGENTE), VIGENTE.length, "vicerrectorado.demo", AHORA);
        when(documentos.firmas(documento.id())).thenReturn(Optional.of(firmas(primera, segunda)));
        when(almacen.leer("documentos/" + sha256(VIGENTE) + ".pdf")).thenReturn(VIGENTE.clone());
        when(identificaciones.de("rectorado.demo")).thenReturn(Optional.of("0109990010"));
        when(validador.validar(any(), any())).thenReturn(aprobados(de("Otra Persona")));
        when(documentos.registrarValidacion(any(), any())).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                servicio.subir(
                                        documento.id(),
                                        FIRMADO,
                                        new Solicitante("rectorado.demo", Set.of("RECTOR"))))
                .isInstanceOf(ArchivoFirmadoRechazado.FirmanteEquivocado.class);
        verifyNoInteractions(aviso);
    }

    // --- US4: quien no está en turno.

    @Test
    void dadoQuienNoEstaEnTurno_cuandoSube_entoncesSinTurnoSinValidarNiRegistrar() {
        enF1();

        for (Solicitante otro :
                List.of(
                        VICERRECTOR,
                        new Solicitante("rectorado.demo", Set.of("RECTOR")),
                        new Solicitante(
                                "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION")))) {
            assertThatThrownBy(() -> servicio.subir(documento.id(), FIRMADO, otro))
                    .as(otro.usuario())
                    .isInstanceOf(SinTurnoDeFirma.class);
        }
        verifyNoInteractions(validador, registro);
        verify(almacen, never()).leer(anyString());
        verify(almacen, never()).guardar(any(), anyString());
        verify(documentos, never()).registrarValidacion(any(), any());
    }

    @Test
    void dadoUnDocumentoInexistenteONoGenerado_cuandoSube_entoncesSinTurno() {
        UUID otro = UUID.randomUUID();
        when(documentos.firmas(otro)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.subir(otro, FIRMADO, DESIGNADO))
                .isInstanceOf(SinTurnoDeFirma.class);
        assertThatThrownBy(() -> servicio.subir(otro, new byte[0], DESIGNADO))
                .as("el turno antes que el tamaño: no dice nada del documento")
                .isInstanceOf(SinTurnoDeFirma.class);
        verifyNoInteractions(validador, registro, almacen);
    }

    private static HallazgosDeLaFirma hallazgosQueFallanEn(Comprobacion falla) {
        return switch (falla) {
            case UNA_FIRMA_NUEVA ->
                    new HallazgosDeLaFirma(
                            List.of(
                                    ResultadoDeComprobacion.rechazada(
                                            UNA_FIRMA_NUEVA,
                                            "El archivo no tiene una firma nueva.")),
                            Optional.empty());
            case INTEGRIDAD ->
                    new HallazgosDeLaFirma(
                            List.of(
                                    ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                    ResultadoDeComprobacion.rechazada(
                                            INTEGRIDAD, "La firma nueva no es íntegra.")),
                            Optional.of(CERTIFICADO));
            case CONFIANZA ->
                    new HallazgosDeLaFirma(
                            List.of(
                                    ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                    ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                                    ResultadoDeComprobacion.rechazada(
                                            CONFIANZA, "No llega a una raíz de confianza.")),
                            Optional.of(CERTIFICADO));
            case MISMO_DOCUMENTO, VIGENCIA, ORDEN, FIRMANTE ->
                    throw new IllegalArgumentException("lo decide el dominio");
        };
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
