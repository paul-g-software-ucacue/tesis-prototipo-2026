package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.DocentesDePrueba;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.DescargarReporte.ArchivoDelReporte;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.GeneradorDePdf;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import ec.mdd.reporting.puertos.salida.RegistroDeDescargas;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Clock;
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
 * Descargar el reporte individual (F20-H01) o el general (F20-H02), RF-48: el archivo se genera
 * completo y se registra en una transacción antes de entregarlo; si el registro falla, no se
 * entrega nada.
 */
class DescargarReporteServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");
    private static final String HASH = "a".repeat(64);
    private static final String ANA = "0109990010";
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
    private final GeneradorDePdf pdf = mock(GeneradorDePdf.class);
    private final RegistroDeDescargas registro = mock(RegistroDeDescargas.class);
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
    private final DescargarReporteServicio servicio =
            new DescargarReporteServicio(
                    new ConsultarReportesServicio(publicaciones, contenido, reloj),
                    pdf,
                    registro,
                    transaccion,
                    reloj);

    @BeforeEach
    void publicada() {
        when(publicaciones.vigentes()).thenReturn(List.of(PUBLICACION));
        when(contenido.contenido("2026-I", 1, ANA))
                .thenReturn(
                        Optional.of(
                                new ContenidoDelDocente(
                                        HASH,
                                        new DatosDelDocente(ANA, "PEREZ ANA", null, null),
                                        List.of(),
                                        List.of(),
                                        HorasPorComponente.NINGUNA)));
        when(pdf.generar(any(ReporteIndividual.class)))
                .thenAnswer(
                        uso -> {
                            pasos.add("pdf, dentro: " + dentro);
                            return new byte[] {'%', 'P', 'D', 'F'};
                        });
    }

    @Test
    void dadoElPdf_cuandoSeDescarga_entoncesSeGeneraAntesYSeRegistraEnLaTransaccion() {
        org.mockito.Mockito.doAnswer(
                        uso -> {
                            pasos.add("registro, dentro: " + dentro);
                            return null;
                        })
                .when(registro)
                .registrar(any());

        ArchivoDelReporte archivo =
                servicio.descargar("2026-I", ANA, Formato.PDF, "rectorado.demo");

        assertThat(pasos).containsExactly("pdf, dentro: false", "registro, dentro: true");
        assertThat(archivo.contenido()).startsWith((byte) '%');
        assertThat(archivo.nombre()).isEqualTo("reporte-individual-2026-I-" + ANA + ".pdf");
        ArgumentCaptor<DescargaDeReporte> descarga =
                ArgumentCaptor.forClass(DescargaDeReporte.class);
        verify(registro).registrar(descarga.capture());
        assertThat(descarga.getValue())
                .isEqualTo(
                        DescargaDeReporte.individual(
                                "rectorado.demo", AHORA, "2026-I", 1, ANA, Formato.PDF));
    }

    @Test
    void dadoElCsv_cuandoSeDescarga_entoncesSonLosBytesDelEscritorYSeRegistraConSuFormato() {
        ArchivoDelReporte archivo =
                servicio.descargar("2026-I", ANA, Formato.CSV, "rectorado.demo");

        assertThat(archivo.nombre()).isEqualTo("reporte-individual-2026-I-" + ANA + ".csv");
        assertThat(new String(archivo.contenido(), java.nio.charset.StandardCharsets.UTF_8))
                .startsWith("﻿\"Periodo\"");
        ArgumentCaptor<DescargaDeReporte> descarga =
                ArgumentCaptor.forClass(DescargaDeReporte.class);
        verify(registro).registrar(descarga.capture());
        assertThat(descarga.getValue().formato()).isEqualTo(Formato.CSV);
        verify(pdf, never()).generar(any(ReporteIndividual.class));
    }

    @Test
    void dadoUnRegistroQueFalla_cuandoSeDescarga_entoncesNoSeEntregaNada() {
        doThrow(new IllegalStateException("sin registro")).when(registro).registrar(any());

        assertThatThrownBy(() -> servicio.descargar("2026-I", ANA, Formato.PDF, "rectorado.demo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaGeneracionQueFalla_cuandoSeDescarga_entoncesNoSeRegistraNada() {
        when(pdf.generar(any(ReporteIndividual.class)))
                .thenThrow(new java.io.UncheckedIOException(new java.io.IOException()));

        assertThatThrownBy(() -> servicio.descargar("2026-I", ANA, Formato.PDF, "rectorado.demo"))
                .isInstanceOf(java.io.UncheckedIOException.class);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnDocenteQueNoEsta_cuandoSeDescarga_entoncesNoSeGeneraNiSeRegistra() {
        when(contenido.contenido("2026-I", 1, "0109990099")).thenReturn(Optional.empty());
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH, List.of())));

        assertThatThrownBy(
                        () ->
                                servicio.descargar(
                                        "2026-I", "0109990099", Formato.CSV, "rectorado.demo"))
                .isInstanceOf(DocenteSinDistributivo.class);
        verify(registro, never()).registrar(any());
    }

    // --- El reporte general (F20-H02, D-117) ---

    @Test
    void dadoElPdfDelGeneral_cuandoSeDescarga_entoncesSeGeneraAntesYSeRegistraSinDocente() {
        conElGeneral();
        when(pdf.generar(any(ReporteGeneral.class)))
                .thenAnswer(
                        uso -> {
                            pasos.add("pdf, dentro: " + dentro);
                            return new byte[] {'%', 'P', 'D', 'F'};
                        });
        org.mockito.Mockito.doAnswer(
                        uso -> {
                            pasos.add("registro, dentro: " + dentro);
                            return null;
                        })
                .when(registro)
                .registrar(any());

        ArchivoDelReporte archivo =
                servicio.descargarGeneral("2026-I", Formato.PDF, "rectorado.demo");

        assertThat(pasos).containsExactly("pdf, dentro: false", "registro, dentro: true");
        assertThat(archivo.nombre()).isEqualTo("reporte-general-2026-I.pdf");
        assertThat(archivo.formato()).isEqualTo(Formato.PDF);
        ArgumentCaptor<DescargaDeReporte> descarga =
                ArgumentCaptor.forClass(DescargaDeReporte.class);
        verify(registro).registrar(descarga.capture());
        assertThat(descarga.getValue())
                .isEqualTo(
                        DescargaDeReporte.general(
                                "rectorado.demo", AHORA, "2026-I", 1, Formato.PDF));
    }

    @Test
    void dadoElCsvDelGeneral_cuandoSeDescarga_entoncesSonLosBytesDelEscritor() {
        conElGeneral();

        ArchivoDelReporte archivo =
                servicio.descargarGeneral("2026-I", Formato.CSV, "rectorado.demo");

        assertThat(archivo.nombre()).isEqualTo("reporte-general-2026-I.csv");
        assertThat(new String(archivo.contenido(), java.nio.charset.StandardCharsets.UTF_8))
                .startsWith("\uFEFF\"Periodo\"")
                .contains("\"Total institucional\"");
        verify(pdf, never()).generar(any(ReporteGeneral.class));
        verify(registro).registrar(any());
    }

    @Test
    void dadoUnRegistroQueFalla_cuandoSeDescargaElGeneral_entoncesNoSeEntregaNada() {
        conElGeneral();
        doThrow(new IllegalStateException("sin registro")).when(registro).registrar(any());

        assertThatThrownBy(() -> servicio.descargarGeneral("2026-I", Formato.CSV, "rectorado.demo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaUnaGeneracionDelGeneralQueFalla_cuandoSeDescarga_entoncesNoSeRegistraNada() {
        conElGeneral();
        when(pdf.generar(any(ReporteGeneral.class)))
                .thenThrow(new java.io.UncheckedIOException(new java.io.IOException()));

        assertThatThrownBy(() -> servicio.descargarGeneral("2026-I", Formato.PDF, "rectorado.demo"))
                .isInstanceOf(java.io.UncheckedIOException.class);
        verify(registro, never()).registrar(any());
    }

    @Test
    void
            dadoUnPeriodoSinPublicacionOUnContenidoInconsistente_cuandoSeDescargaElGeneral_nadaQueda() {
        assertThatThrownBy(
                        () -> servicio.descargarGeneral("2025-II", Formato.PDF, "rectorado.demo"))
                .isInstanceOf(SinPublicacion.class);
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion("b".repeat(64), List.of())));
        assertThatThrownBy(() -> servicio.descargarGeneral("2026-I", Formato.CSV, "rectorado.demo"))
                .isInstanceOf(ContenidoInconsistente.class);
        verify(registro, never()).registrar(any());
        verify(pdf, never()).generar(any(ReporteGeneral.class));
    }

    private void conElGeneral() {
        when(contenido.docentes("2026-I", 1))
                .thenReturn(
                        Optional.of(new DocentesDeLaVersion(HASH, DocentesDePrueba.repetidos(3))));
    }
}
