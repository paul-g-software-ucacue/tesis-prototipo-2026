package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.PeriodoListo;
import ec.mdd.generacion.dominio.ReglasDelPanel;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaElPanel;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.RepositorioJustificaciones;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El panel se evalúa al abrirlo y no guarda nada (FR-001). */
class ConsultarPanelServicioTest {

    private static final String PERIODO = "2026-I";
    private static final VersionDistributivo VERSION =
            new VersionDistributivo(
                    UUID.randomUUID(), 2, EstadoDeVersion.EDITABLE, List.of(), List.of());
    private static final ReglasDelPanel REGLAS =
            new ReglasDelPanel(
                    Map.of(
                            "RF-24", Severidad.DURA,
                            "RN-02", Severidad.DURA,
                            "RN-05", Severidad.BLANDA,
                            "RN-10", Severidad.BLANDA,
                            "RN-12", Severidad.DURA,
                            "RN-17", Severidad.DURA,
                            "RN-18", Severidad.DURA),
                    new BigDecimal("40"),
                    new BigDecimal("60"),
                    new BigDecimal("40"),
                    new BigDecimal("5"),
                    8,
                    11,
                    List.of("140-b", "140-g"));
    private static final AsignaturaParalelo PARALELO =
            new AsignaturaParalelo(
                    new AsignaturaOfertada("POO", "Programación", "06", "061"),
                    "A",
                    "TDS",
                    new BigDecimal("4"),
                    false,
                    false);

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final DatosParaElPanel normativa = mock(DatosParaElPanel.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RepositorioJustificaciones justificaciones =
            mock(RepositorioJustificaciones.class);
    private final ConsultarPanelServicio servicio =
            new ConsultarPanelServicio(datos, normativa, distributivos, justificaciones);

    @BeforeEach
    void conUnPeriodo() {
        when(datos.periodoListo(PERIODO))
                .thenReturn(Optional.of(new PeriodoListo(true, false, true)));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(VERSION));
        when(normativa.reglasDelPanel(PERIODO)).thenReturn(Optional.of(REGLAS));
        when(normativa.cargosVigentes()).thenReturn(List.of());
        when(datos.docentesActivos()).thenReturn(List.of());
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO)));
        when(justificaciones.vigentes(VERSION.id())).thenReturn(List.of());
    }

    @Test
    void dadaLaVersion_cuandoSeConsulta_entoncesEvaluaConLasCifrasDeLaNormativa() {
        PanelDeInconsistencias panel = servicio.panel(PERIODO);

        assertThat(panel.creado()).isTrue();
        assertThat(panel.numero()).isEqualTo(2);
        assertThat(panel.modificable()).isTrue();
        assertThat(panel.entrada(TipoDeInconsistencia.PARALELO_SIN_PROPUESTA, "POO/A")).isPresent();
        assertThat(panel.entrada(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO, PERIODO))
                .isPresent();
    }

    @Test
    void dadaUnaJustificacionGuardada_cuandoSeConsulta_entoncesLaAplica() {
        when(justificaciones.vigentes(VERSION.id()))
                .thenReturn(
                        List.of(
                                new JustificacionDeInconsistencia(
                                        TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO,
                                        PERIODO,
                                        "El instituto es pequeño este año",
                                        "consolidacion.demo",
                                        Instant.parse("2026-11-02T15:00:00Z"))));

        PanelDeInconsistencias panel = servicio.panel(PERIODO);

        assertThat(
                        panel.entrada(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO, PERIODO)
                                .orElseThrow()
                                .abierta())
                .isFalse();
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeConsulta_entoncesVacioSinPedirMas() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        PanelDeInconsistencias panel = servicio.panel(PERIODO);

        assertThat(panel.creado()).isFalse();
        assertThat(panel.entradas()).isEmpty();
        verify(normativa, never()).reglasDelPanel(any());
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entoncesSeLeePeroNoEsModificable() {
        when(datos.periodoListo(PERIODO))
                .thenReturn(Optional.of(new PeriodoListo(true, true, true)));

        PanelDeInconsistencias panel = servicio.panel(PERIODO);

        assertThat(panel.modificable()).isFalse();
        assertThat(panel.entradas()).isNotEmpty();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesNoEncontrado() {
        when(datos.periodoListo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.panel(PERIODO)).isInstanceOf(PeriodoNoEncontrado.class);
    }

    @Test
    void dadaUnaVersionDeNormativaAusente_cuandoSeConsulta_entoncesSinAbrir() {
        when(normativa.reglasDelPanel(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.panel(PERIODO)).isInstanceOf(PeriodoSinAbrir.class);
    }

    @Test
    void alConsultar_cuandoTermina_entoncesNoGuardoNada() {
        servicio.panel(PERIODO);

        verify(justificaciones).vigentes(VERSION.id());
        verifyNoMoreInteractions(justificaciones);
    }
}
