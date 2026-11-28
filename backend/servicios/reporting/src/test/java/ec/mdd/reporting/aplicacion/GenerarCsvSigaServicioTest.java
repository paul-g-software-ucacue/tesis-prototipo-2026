package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.reporting.adaptadores.salida.ExportadorSigaCsv;
import ec.mdd.reporting.adaptadores.salida.LectorDeLayoutYaml;
import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CargasDeLaVersion;
import ec.mdd.reporting.dominio.CargasDePrueba;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.CsvExportado;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.DocentesDePrueba;
import ec.mdd.reporting.dominio.ExportacionRechazada;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import ec.mdd.reporting.dominio.InconsistenciasAbiertas;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.MotivoDeRechazo;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.RechazoDeExportacion;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.ExportacionesSiga;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
import ec.mdd.reporting.puertos.salida.FirmasDeLasVersiones;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados.LayoutGuardado;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Generar el CSV para SIGA (F19-H01, RF-52, D-119): una sola lectura de la publicación y del
 * layout, las filas comprobadas contra el reporte general de la misma versión, y la entrada en la
 * bitácora antes de entregar el archivo.
 */
class GenerarCsvSigaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");
    private static final String HASH = "a".repeat(64);
    private static final byte[] BYTES = "ID\r\n1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    false);

    private final PublicacionesDelDistributivo publicaciones =
            mock(PublicacionesDelDistributivo.class);
    private final ContenidoDeLaVersion contenido = mock(ContenidoDeLaVersion.class);
    private final FirmasDeLasVersiones firmas = mock(FirmasDeLasVersiones.class);
    private final LayoutsGuardados layouts = mock(LayoutsGuardados.class);
    private final ExportadorSiga exportador = mock(ExportadorSiga.class);
    private final RegistroEnBitacora registro = mock(RegistroEnBitacora.class);
    private final ExportacionesSiga exportaciones = mock(ExportacionesSiga.class);
    private final List<String> pasos = new ArrayList<>();
    private boolean dentro;
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    dentro = true;
                    try {
                        return trabajo.get();
                    } finally {
                        dentro = false;
                    }
                }
            };
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final List<DocentesDeLaVersion.Docente> docentes =
            DocentesDePrueba.todos().subList(0, 5);

    @BeforeEach
    void preparar() {
        when(publicaciones.vigentes()).thenReturn(List.of(PUBLICACION));
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH, docentes)));
        when(contenido.cargas("2026-I", 1))
                .thenReturn(
                        Optional.of(conInconsistencias(CargasDePrueba.de(HASH, docentes), 0, 0)));
        when(firmas.firmas("2026-I", 1)).thenReturn(Optional.of(firmadas(true, true, true)));
        when(layouts.porDefecto()).thenReturn(plantilla());
        when(layouts.vigente()).thenReturn(Optional.empty());
        when(exportador.exportar(any(), any()))
                .thenAnswer(
                        invocacion -> {
                            pasos.add("exportar");
                            return new ArchivoSiga(BYTES, "siga-2026-I-v1.csv");
                        });
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            pasos.add("guardar");
                            return null;
                        })
                .when(exportaciones)
                .guardar(any());
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            pasos.add("registrar");
                            return null;
                        })
                .when(registro)
                .registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadoElPeriodoPublicado_cuandoSeGenera_entoncesExportaYRegistraAntesDeEntregar() {
        CsvExportado exportado = servicio().generar("2026-I", "secretaria.demo");

        assertThat(exportado.archivo().nombre()).isEqualTo("siga-2026-I-v1.csv");
        // F19-H02 (D-120): el registro y su entrada, en la misma transacción, antes de entregar.
        assertThat(pasos).containsExactly("exportar", "guardar", "registrar");
        verify(publicaciones, times(1)).vigentes();
        verify(layouts, times(1)).vigente();
        ArgumentCaptor<FilasParaSiga> filas = ArgumentCaptor.forClass(FilasParaSiga.class);
        ArgumentCaptor<LayoutExportacion> layout = ArgumentCaptor.forClass(LayoutExportacion.class);
        verify(exportador).exportar(filas.capture(), layout.capture());
        assertThat(filas.getValue().publicacion()).isEqualTo(PUBLICACION);
        assertThat(layout.getValue().esPorDefecto()).isTrue();
        ExportacionSiga exportacion = exportado.exportacion();
        assertThat(exportacion.usuario()).isEqualTo("secretaria.demo");
        assertThat(exportacion.momento()).isEqualTo(AHORA);
        assertThat(exportacion.version()).isEqualTo(1);
        assertThat(exportacion.hashVersion()).isEqualTo(HASH);
        assertThat(exportacion.layout()).isZero();
        assertThat(exportacion.filas()).isEqualTo(filas.getValue().filas().size());
        assertThat(exportacion.suma()).isEqualTo(SumaDeVerificacion.de(BYTES));
        verify(exportaciones).guardar(exportacion);
        verify(registro).registrar(exportacion);
    }

    @Test
    void dadoUnLayoutCargado_cuandoSeGenera_entoncesUsaEseYLoRegistra() {
        when(layouts.vigente())
                .thenReturn(
                        Optional.of(
                                new LayoutGuardado(
                                        3,
                                        "columnas:\n  - {nombre: ID, campo: identificacion}\n"
                                                + "  - {nombre: C, campo: componente}\n"
                                                + "  - {nombre: H, campo: horas}\n",
                                        "c".repeat(64),
                                        "secretaria.demo",
                                        AHORA)));

        servicio().generar("2026-I", "secretaria.demo");

        ArgumentCaptor<LayoutExportacion> layout = ArgumentCaptor.forClass(LayoutExportacion.class);
        verify(exportador).exportar(any(), layout.capture());
        assertThat(layout.getValue().numero()).isEqualTo(3);
        assertThat(layout.getValue().encabezado()).containsExactly("ID", "C", "H");
        ArgumentCaptor<ExportacionSiga> exportacion =
                ArgumentCaptor.forClass(ExportacionSiga.class);
        verify(registro).registrar(exportacion.capture());
        assertThat(exportacion.getValue().layout()).isEqualTo(3);
    }

    @Test
    void dadoUnPeriodoSinPublicacion_cuandoSeGenera_entoncesSinPublicacionYSinRegistro() {
        assertThatThrownBy(() -> servicio().generar("2027-I", "secretaria.demo"))
                .isInstanceOf(SinPublicacion.class);
        verify(exportador, never()).exportar(any(), any());
        verify(registro, never()).registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadaUnaVersionSinContenido_cuandoSeGenera_entoncesEsInconsistenteYSinRegistro() {
        when(contenido.cargas("2026-I", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(ContenidoInconsistente.class);
        verify(registro, never()).registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadoQueElRegistroFalla_cuandoSeGenera_entoncesNoEntregaElArchivo() {
        doThrow(new IllegalStateException("sin base"))
                .when(registro)
                .registrar(any(ExportacionSiga.class));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadoQueElGuardadoDeLaExportacionFalla_cuandoSeGenera_entoncesNoEntregaNiAnotaLaEntrada() {
        doThrow(new IllegalStateException("sin base")).when(exportaciones).guardar(any());

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
        verify(registro, never()).registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadosDosPedidosIguales_cuandoSeGenera_entoncesDosExportacionesConLaMismaSuma() {
        CsvExportado una = servicio().generar("2026-I", "secretaria.demo");
        CsvExportado otra = servicio().generar("2026-I", "secretaria.demo");

        assertThat(una.exportacion().suma()).isEqualTo(otra.exportacion().suma());
        assertThat(una.exportacion().id()).isNotEqualTo(otra.exportacion().id());
        verify(exportaciones, times(2)).guardar(any());
    }

    @Test
    void dadoQueLaExportacionFalla_cuandoSeGenera_entoncesNoRegistra() {
        when(exportador.exportar(any(), any())).thenThrow(new IllegalStateException("roto"));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
        verify(registro, never()).registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadaUnaReforma_cuandoSeGenera_entoncesSaleDeLaVersionPublicadaVigente() {
        Publicacion enReforma =
                new Publicacion(
                        "2026-I", 1, HASH, PUBLICACION.publicadaEn(), "secretaria.demo", true);
        when(publicaciones.vigentes()).thenReturn(List.of(enReforma));

        servicio().generar("2026-I", "secretaria.demo");

        verify(contenido).cargas("2026-I", 1);
        verify(contenido).docentes("2026-I", 1);
    }

    @Test
    void dados300Docentes_cuandoSeGeneraConElExportadorReal_entoncesTardaMenosDe10Segundos() {
        List<DocentesDeLaVersion.Docente> trescientos = DocentesDePrueba.repetidos(300);
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH, trescientos)));
        when(contenido.cargas("2026-I", 1))
                .thenReturn(
                        Optional.of(
                                conInconsistencias(CargasDePrueba.de(HASH, trescientos), 0, 0)));
        GenerarCsvSigaServicio real =
                new GenerarCsvSigaServicio(
                        publicaciones,
                        contenido,
                        firmas,
                        new LayoutVigente(layouts, new LectorDeLayoutYaml()),
                        new ExportadorSigaCsv(),
                        exportaciones,
                        registro,
                        transaccion,
                        reloj);

        long inicio = System.nanoTime();
        ArchivoSiga archivo = real.generar("2026-I", "secretaria.demo").archivo();
        Duration tiempo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(tiempo).isLessThan(Duration.ofSeconds(10));
        String csv = new String(archivo.contenido(), StandardCharsets.UTF_8);
        assertThat(csv.split("\r\n")).hasSizeGreaterThan(300);
        System.out.println(
                "SC-007 (F19-H02): CSV para SIGA de 300 docentes, validado y registrado, en "
                        + tiempo.toMillis()
                        + " ms");
    }

    // F19-H02 (D-120): la validación antes de exportar.
    @Test
    void dadasFirmasIncompletas_cuandoSeGenera_entoncesRechazaSinExportarYRegistraElRechazo() {
        when(firmas.firmas("2026-I", 1)).thenReturn(Optional.of(firmadas(true, false, false)));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOfSatisfying(
                        ExportacionRechazada.class,
                        rechazo ->
                                assertThat(rechazo.condiciones().motivos())
                                        .containsExactly(MotivoDeRechazo.FIRMAS_INCOMPLETAS));
        verify(exportador, never()).exportar(any(), any());
        verify(registro, never()).registrar(any(ExportacionSiga.class));
        ArgumentCaptor<RechazoDeExportacion> rechazo =
                ArgumentCaptor.forClass(RechazoDeExportacion.class);
        verify(registro).registrar(rechazo.capture());
        assertThat(rechazo.getValue().usuario()).isEqualTo("secretaria.demo");
        assertThat(rechazo.getValue().momento()).isEqualTo(AHORA);
        assertThat(rechazo.getValue().version()).isEqualTo(1);
    }

    @Test
    void dadasInconsistenciasAbiertas_cuandoSeGenera_entoncesRechazaYRegistraEnUnaTransaccion() {
        when(contenido.cargas("2026-I", 1))
                .thenReturn(
                        Optional.of(conInconsistencias(CargasDePrueba.de(HASH, docentes), 1, 2)));
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return null;
                        })
                .when(registro)
                .registrar(any(RechazoDeExportacion.class));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(ExportacionRechazada.class);
        verify(registro).registrar(any(RechazoDeExportacion.class));
        verify(exportador, never()).exportar(any(), any());
    }

    @Test
    void dadoQueElRegistroDelRechazoFalla_cuandoSeGenera_entoncesSigueElRechazo() {
        when(firmas.firmas("2026-I", 1)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("sin base"))
                .when(registro)
                .registrar(any(RechazoDeExportacion.class));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(ExportacionRechazada.class);
    }

    @Test
    void dadoUnPeriodoSinPublicacion_cuandoSeGenera_entoncesNoPideLasFirmasNiRegistraRechazo() {
        assertThatThrownBy(() -> servicio().generar("2027-I", "secretaria.demo"))
                .isInstanceOf(SinPublicacion.class);
        verify(firmas, never()).firmas(any(), anyInt());
        verify(registro, never()).registrar(any(RechazoDeExportacion.class));
    }

    @Test
    void dadoQueFirmaNoResponde_cuandoSeGenera_entoncesElErrorDelProcesoSinRegistro() {
        when(firmas.firmas("2026-I", 1)).thenThrow(new ProcesoSinRespuesta());

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(ProcesoSinRespuesta.class);
        verify(registro, never()).registrar(any(RechazoDeExportacion.class));
        verify(registro, never()).registrar(any(ExportacionSiga.class));
    }

    @Test
    void dadaUnaReforma_cuandoSeGenera_entoncesValidaLasFirmasDeLaVersionPublicadaUnaVez() {
        when(publicaciones.vigentes())
                .thenReturn(
                        List.of(
                                new Publicacion(
                                        "2026-I",
                                        1,
                                        HASH,
                                        PUBLICACION.publicadaEn(),
                                        "secretaria.demo",
                                        true)));

        servicio().generar("2026-I", "secretaria.demo");

        verify(firmas, times(1)).firmas("2026-I", 1);
    }

    private static FirmasDeLaVersion firmadas(boolean... firmados) {
        List<FirmasDeLaVersion.PasoFirmado> pasos = new ArrayList<>();
        for (int i = 0; i < firmados.length; i++) {
            pasos.add(new FirmasDeLaVersion.PasoFirmado(i + 1, firmados[i]));
        }
        return new FirmasDeLaVersion(HASH, pasos, false, false);
    }

    private static CargasDeLaVersion conInconsistencias(
            CargasDeLaVersion cargas, int duras, int blandas) {
        return new CargasDeLaVersion(
                cargas.hash(),
                cargas.cargas(),
                Optional.of(new InconsistenciasAbiertas(duras, blandas)));
    }

    private GenerarCsvSigaServicio servicio() {
        return new GenerarCsvSigaServicio(
                publicaciones,
                contenido,
                firmas,
                new LayoutVigente(layouts, new LectorDeLayoutYaml()),
                exportador,
                exportaciones,
                registro,
                transaccion,
                reloj);
    }

    private static String plantilla() {
        try (InputStream entrada =
                GenerarCsvSigaServicioTest.class.getResourceAsStream(
                        "/reporting/layout-siga-por-defecto.yaml")) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
