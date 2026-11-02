package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ArchivoPdf;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.InformeNoEsPdf;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.InformeIlegible;
import ec.mdd.generacion.puertos.entrada.InformeNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinConsolidar;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.AlmacenDeArchivos;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Adjuntar y descargar el informe, sin dejar nada a medias (FR-014 a FR-022). */
class InformeSustentadoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String QUIEN = "consolidacion.demo";
    private static final String CLAVE = "c3a0ad8e-9e4b-4e65-8c52-0f8c6b1d2a11";
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final UUID VERSION = UUID.randomUUID();
    private static final byte[] PDF = "%PDF-1.7\nprueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final InformeSustentado INFORME =
            new InformeSustentado(
                    "informe.pdf", PDF.length, ArchivoPdf.sha256(PDF), CLAVE, QUIEN, AHORA);

    private final ConsultarConsolidado consultar = mock(ConsultarConsolidado.class);
    private final AlmacenDeArchivos almacen = mock(AlmacenDeArchivos.class);
    private final RepositorioConsolidacion repositorio = mock(RepositorioConsolidacion.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final InformeSustentadoServicio servicio =
            new InformeSustentadoServicio(
                    consultar,
                    almacen,
                    repositorio,
                    registro,
                    new Transaccion() {
                        @Override
                        public <T> T ejecutar(Supplier<T> trabajo) {
                            return trabajo.get();
                        }
                    },
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void consolidado() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                true,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.empty()));
        when(almacen.guardar(any())).thenReturn(CLAVE);
    }

    @Test
    void dadoUnPdf_cuandoSeAdjunta_entoncesGuardaElArchivoYLaFila() {
        InformeSustentado informe = servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN);

        assertThat(informe).isEqualTo(INFORME);
        verify(almacen).guardar(PDF);
        verify(repositorio).guardarInforme(VERSION, INFORME);
    }

    @Test
    void dadoUnPdf_cuandoSeAdjunta_entoncesVaAlRegistroSinElContenido() {
        servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN);

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeDistributivo.Accion.INFORME_ADJUNTADO);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("huella", INFORME.huella())
                .doesNotContainKey("contenido");
    }

    @Test
    void sinConsolidar_cuandoSeAdjunta_entoncesLoRechaza() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(consolidado(true, Optional.empty(), Optional.empty()));

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(SinConsolidar.class);

        assertNoGuardoNada();
    }

    @Test
    void sinDistributivo_cuandoSeAdjunta_entoncesLoRechaza() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(ConsolidadoDelPeriodo.sinDistributivo(PERIODO, true));

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(SinDistributivo.class);

        assertNoGuardoNada();
    }

    @Test
    void conElPeriodoCerrado_cuandoSeAdjunta_entoncesLoRechaza() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                false,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.empty()));

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(PeriodoCerrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoAlgoQueNoEsPdf_cuandoSeAdjunta_entoncesNoLoGuarda() {
        byte[] texto = "no soy un PDF".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", texto, QUIEN))
                .isInstanceOf(InformeNoEsPdf.class);

        assertNoGuardoNada();
    }

    @Test
    void siFallaElAlmacen_cuandoSeAdjunta_entoncesNoQuedaLaFila() {
        when(almacen.guardar(any()))
                .thenThrow(new UncheckedIOException(new IOException("disco lleno")));

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(UncheckedIOException.class);

        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnInformeAdjunto_cuandoSeAdjuntaOtro_entoncesAgregaOtraFila() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                true,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.of(INFORME)));

        servicio.adjuntar(PERIODO, "otro.pdf", PDF, QUIEN);

        verify(repositorio).guardarInforme(any(), any());
    }

    @Test
    void dadoUnInforme_cuandoSeDescarga_entoncesDevuelveElMismoArchivo() {
        conInforme();
        when(almacen.leer(CLAVE)).thenReturn(PDF);

        assertThat(servicio.descargar(PERIODO)).isEqualTo(PDF);
    }

    @Test
    void conElPeriodoCerrado_cuandoSeDescarga_entoncesSeDescarga() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                false,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.of(INFORME)));
        when(almacen.leer(CLAVE)).thenReturn(PDF);

        assertThat(servicio.descargar(PERIODO)).isEqualTo(PDF);
    }

    @Test
    void sinInforme_cuandoSeDescarga_entoncesNoEncontrado() {
        assertThatThrownBy(() -> servicio.descargar(PERIODO))
                .isInstanceOf(InformeNoEncontrado.class);
    }

    @Test
    void conUnArchivoCambiado_cuandoSeDescarga_entoncesNoLoEntrega() {
        conInforme();
        when(almacen.leer(CLAVE)).thenReturn("%PDF-otro".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> servicio.descargar(PERIODO)).isInstanceOf(InformeIlegible.class);
    }

    @Test
    void conUnArchivoQueFalta_cuandoSeDescarga_entoncesIlegible() {
        conInforme();
        when(almacen.leer(CLAVE)).thenThrow(new UncheckedIOException(new IOException("no existe")));

        assertThatThrownBy(() -> servicio.descargar(PERIODO)).isInstanceOf(InformeIlegible.class);
    }

    private void conInforme() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                true,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.of(INFORME)));
    }

    private void assertNoGuardoNada() {
        verify(almacen, never()).guardar(any());
        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
    }

    private static ConsolidadoDelPeriodo consolidado(
            boolean modificable,
            Optional<Consolidacion> consolidacion,
            Optional<InformeSustentado> informe) {
        return new ConsolidadoDelPeriodo(
                PERIODO,
                modificable,
                Optional.of(VERSION),
                1,
                consolidacion,
                false,
                List.of(),
                new CargaHoraria(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO),
                0,
                0,
                informe);
    }
}
