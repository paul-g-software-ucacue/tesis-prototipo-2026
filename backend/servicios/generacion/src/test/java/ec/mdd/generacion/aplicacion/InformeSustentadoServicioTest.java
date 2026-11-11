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
import ec.mdd.generacion.dominio.VersionCongelada;
import ec.mdd.generacion.puertos.entrada.ConsolidadoEnRevision;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.EnRevision;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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
    // #215: si el distributivo está en revisión, fuera o dentro de la transacción (research §2).
    private boolean dentro;
    private boolean enRevisionFuera;
    private boolean enRevisionDentro;
    private final List<Boolean> consultadoDentro = new ArrayList<>();
    // F13-H01: la versión congelada, también fuera o dentro de la transacción.
    private Integer congeladaFuera;
    private Integer congeladaDentro;
    private final EnRevision enRevision =
            new EnRevision() {
                @Override
                public boolean enRevision(String periodo) {
                    consultadoDentro.add(dentro);
                    return dentro ? enRevisionDentro : enRevisionFuera;
                }

                @Override
                public OptionalInt versionCongelada(String periodo) {
                    Integer numero = dentro ? congeladaDentro : congeladaFuera;
                    return numero == null ? OptionalInt.empty() : OptionalInt.of(numero);
                }
            };
    private final InformeSustentadoServicio servicio =
            new InformeSustentadoServicio(
                    consultar,
                    almacen,
                    repositorio,
                    registro,
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
                    },
                    enRevision,
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

    // A2 del análisis: antes de guardar el archivo, sin tocar el almacén.
    @Test
    void dadoElDistributivoEnRevision_cuandoSeAdjunta_entoncesSeRechazaSinTocarElAlmacen() {
        enRevisionFuera = true;
        enRevisionDentro = true;

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(ConsolidadoEnRevision.class);
        verify(almacen, never()).guardar(any());
        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
    }

    // F13-H01, research §4: la versión congelada gana a la revisión, antes del almacén.
    @Test
    void
            dadaLaVersionCongeladaYEnRevision_cuandoSeAdjunta_entoncesVersionCongeladaSinTocarElAlmacen() {
        enRevisionFuera = true;
        enRevisionDentro = true;
        congeladaFuera = 1;
        congeladaDentro = 1;

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(VersionCongelada.class);
        verify(almacen, never()).guardar(any());
        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaCongelacionEntreLasDosComprobaciones_cuandoSeAdjunta_entoncesNoGuardaLaFila() {
        congeladaDentro = 1;

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(VersionCongelada.class);
        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
    }

    // Y otra vez dentro de la transacción, por si el envío a revisión llegó entre las dos.
    @Test
    void dadoUnEnvioARevisionEntreLasDosComprobaciones_cuandoSeAdjunta_entoncesNoGuardaLaFila() {
        enRevisionDentro = true;

        assertThatThrownBy(() -> servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN))
                .isInstanceOf(ConsolidadoEnRevision.class);
        verify(repositorio, never()).guardarInforme(any(), any());
        verify(registro, never()).registrar(any());
        assertThat(consultadoDentro).containsExactly(false, true);
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
        assertThat(cambio.getValue().anterior()).isEmpty();
        assertThat(cambio.getValue().nuevo())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("huella", INFORME.huella(), "tamano", PDF.length));
    }

    @Test
    void dadoUnInformeAnterior_cuandoSeReemplaza_entoncesRegistraSuHuella() {
        InformeSustentado anterior =
                new InformeSustentado("viejo.pdf", 10, "ab".repeat(32), CLAVE, QUIEN, AHORA);
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                true,
                                Optional.of(new Consolidacion(QUIEN, AHORA)),
                                Optional.of(anterior)));

        servicio.adjuntar(PERIODO, "informe.pdf", PDF, QUIEN);

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().anterior())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("huella", anterior.huella(), "tamano", 10));
        assertThat(cambio.getValue().nuevo()).containsEntry("huella", INFORME.huella());
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
