package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El consolidado se calcula al abrirlo, reutilizando el panel (FR-010, FR-011). */
class ConsultarConsolidadoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final VersionDistributivo VERSION =
            new VersionDistributivo(
                    UUID.randomUUID(), 2, EstadoDeVersion.EDITABLE, List.of(), List.of());

    private final ConsultarPanel panel = mock(ConsultarPanel.class);
    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RepositorioConsolidacion consolidacion = mock(RepositorioConsolidacion.class);
    private final ConsultarConsolidadoServicio servicio =
            new ConsultarConsolidadoServicio(panel, datos, distributivos, consolidacion);

    @BeforeEach
    void conUnaVersion() {
        when(panel.panel(PERIODO))
                .thenReturn(
                        new PanelDeInconsistencias(
                                PERIODO, true, Optional.of(VERSION.id()), 2, List.of()));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(VERSION));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of()));
        when(consolidacion.ultimaConsolidacion(VERSION.id())).thenReturn(Optional.empty());
        when(consolidacion.ultimoInforme(VERSION.id())).thenReturn(Optional.empty());
        when(consolidacion.ultimoCambioDeAsignaciones(PERIODO)).thenReturn(Optional.empty());
    }

    @Test
    void dadaLaVersion_cuandoSeConsulta_entoncesArmaElConsolidado() {
        ConsolidadoDelPeriodo consolidado = servicio.consolidado(PERIODO);

        assertThat(consolidado.creado()).isTrue();
        assertThat(consolidado.numero()).isEqualTo(2);
        assertThat(consolidado.modificable()).isTrue();
    }

    @Test
    void dadaUnaConsolidacionYUnCambioPosterior_cuandoSeConsulta_entoncesAvisa() {
        when(consolidacion.ultimaConsolidacion(VERSION.id()))
                .thenReturn(Optional.of(new Consolidacion("consolidacion.demo", MOMENTO)));
        when(consolidacion.ultimoCambioDeAsignaciones(PERIODO))
                .thenReturn(Optional.of(MOMENTO.plusSeconds(1)));

        assertThat(servicio.consolidado(PERIODO).desactualizado()).isTrue();
    }

    @Test
    void dadoUnInforme_cuandoSeConsulta_entoncesLoTrae() {
        InformeSustentado informe =
                new InformeSustentado(
                        "informe.pdf", 100, "a".repeat(64), "clave", "consolidacion.demo", MOMENTO);
        when(consolidacion.ultimoInforme(VERSION.id())).thenReturn(Optional.of(informe));

        assertThat(servicio.consolidado(PERIODO).informe()).contains(informe);
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeConsulta_entoncesVacioSinPedirMas() {
        when(panel.panel(PERIODO))
                .thenReturn(PanelDeInconsistencias.sinDistributivo(PERIODO, true));

        ConsolidadoDelPeriodo consolidado = servicio.consolidado(PERIODO);

        assertThat(consolidado.creado()).isFalse();
        verify(datos, never()).ofertaDelPeriodo(any());
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entoncesSeLee() {
        when(panel.panel(PERIODO))
                .thenReturn(
                        new PanelDeInconsistencias(
                                PERIODO, false, Optional.of(VERSION.id()), 2, List.of()));

        ConsolidadoDelPeriodo consolidado = servicio.consolidado(PERIODO);

        assertThat(consolidado.creado()).isTrue();
        assertThat(consolidado.modificable()).isFalse();
    }
}
