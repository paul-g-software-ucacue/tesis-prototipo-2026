package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.reporting.adaptadores.salida.ExportadorSigaCsv;
import ec.mdd.reporting.adaptadores.salida.LectorDeLayoutYaml;
import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CargasDePrueba;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.DocentesDePrueba;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
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
    private final LayoutsGuardados layouts = mock(LayoutsGuardados.class);
    private final ExportadorSiga exportador = mock(ExportadorSiga.class);
    private final RegistroEnBitacora registro = mock(RegistroEnBitacora.class);
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
                .thenReturn(Optional.of(CargasDePrueba.de(HASH, docentes)));
        when(layouts.porDefecto()).thenReturn(plantilla());
        when(layouts.vigente()).thenReturn(Optional.empty());
        when(exportador.exportar(any(), any()))
                .thenAnswer(
                        invocacion -> {
                            pasos.add("exportar");
                            return new ArchivoSiga(new byte[] {1}, "siga-2026-I-v1.csv");
                        });
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            pasos.add("registrar");
                            return null;
                        })
                .when(registro)
                .registrar(any(CsvGenerado.class));
    }

    @Test
    void dadoElPeriodoPublicado_cuandoSeGenera_entoncesExportaYRegistraAntesDeEntregar() {
        ArchivoSiga archivo = servicio().generar("2026-I", "secretaria.demo");

        assertThat(archivo.nombre()).isEqualTo("siga-2026-I-v1.csv");
        assertThat(pasos).containsExactly("exportar", "registrar");
        verify(publicaciones, times(1)).vigentes();
        verify(layouts, times(1)).vigente();
        ArgumentCaptor<FilasParaSiga> filas = ArgumentCaptor.forClass(FilasParaSiga.class);
        ArgumentCaptor<LayoutExportacion> layout = ArgumentCaptor.forClass(LayoutExportacion.class);
        verify(exportador).exportar(filas.capture(), layout.capture());
        assertThat(filas.getValue().publicacion()).isEqualTo(PUBLICACION);
        assertThat(layout.getValue().esPorDefecto()).isTrue();
        ArgumentCaptor<CsvGenerado> generado = ArgumentCaptor.forClass(CsvGenerado.class);
        verify(registro).registrar(generado.capture());
        assertThat(generado.getValue())
                .isEqualTo(new CsvGenerado("secretaria.demo", AHORA, "2026-I", 1, 0));
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
        ArgumentCaptor<CsvGenerado> generado = ArgumentCaptor.forClass(CsvGenerado.class);
        verify(registro).registrar(generado.capture());
        assertThat(generado.getValue().layout()).isEqualTo(3);
    }

    @Test
    void dadoUnPeriodoSinPublicacion_cuandoSeGenera_entoncesSinPublicacionYSinRegistro() {
        assertThatThrownBy(() -> servicio().generar("2027-I", "secretaria.demo"))
                .isInstanceOf(SinPublicacion.class);
        verify(exportador, never()).exportar(any(), any());
        verify(registro, never()).registrar(any(CsvGenerado.class));
    }

    @Test
    void dadaUnaVersionSinContenido_cuandoSeGenera_entoncesEsInconsistenteYSinRegistro() {
        when(contenido.cargas("2026-I", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(ContenidoInconsistente.class);
        verify(registro, never()).registrar(any(CsvGenerado.class));
    }

    @Test
    void dadoQueElRegistroFalla_cuandoSeGenera_entoncesNoEntregaElArchivo() {
        doThrow(new IllegalStateException("sin base"))
                .when(registro)
                .registrar(any(CsvGenerado.class));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadoQueLaExportacionFalla_cuandoSeGenera_entoncesNoRegistra() {
        when(exportador.exportar(any(), any())).thenThrow(new IllegalStateException("roto"));

        assertThatThrownBy(() -> servicio().generar("2026-I", "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
        verify(registro, never()).registrar(any(CsvGenerado.class));
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
                .thenReturn(Optional.of(CargasDePrueba.de(HASH, trescientos)));
        GenerarCsvSigaServicio real =
                new GenerarCsvSigaServicio(
                        publicaciones,
                        contenido,
                        new LayoutVigente(layouts, new LectorDeLayoutYaml()),
                        new ExportadorSigaCsv(),
                        registro,
                        transaccion,
                        reloj);

        long inicio = System.nanoTime();
        ArchivoSiga archivo = real.generar("2026-I", "secretaria.demo");
        Duration tiempo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(tiempo).isLessThan(Duration.ofSeconds(10));
        String csv = new String(archivo.contenido(), StandardCharsets.UTF_8);
        assertThat(csv.split("\r\n")).hasSizeGreaterThan(300);
        System.out.println("SC-008: CSV para SIGA de 300 docentes en " + tiempo.toMillis() + " ms");
    }

    private GenerarCsvSigaServicio servicio() {
        return new GenerarCsvSigaServicio(
                publicaciones,
                contenido,
                new LayoutVigente(layouts, new LectorDeLayoutYaml()),
                exportador,
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
