package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Consolidar marca sin bloquear: guarda quién y cuándo, y se repite (FR-002 a FR-006). */
class ConsolidarServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String QUIEN = "consolidacion.demo";
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final UUID VERSION = UUID.randomUUID();

    private final ConsultarConsolidado consultar = mock(ConsultarConsolidado.class);
    private final RepositorioConsolidacion consolidacion = mock(RepositorioConsolidacion.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ConsolidarServicio servicio =
            new ConsolidarServicio(
                    consultar,
                    consolidacion,
                    registro,
                    new Transaccion() {
                        @Override
                        public <T> T ejecutar(Supplier<T> trabajo) {
                            return trabajo.get();
                        }
                    },
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadaLaVersion_cuandoSeConsolida_entoncesGuardaQuienYCuando() {
        when(consultar.consolidado(PERIODO)).thenReturn(consolidado(true, 0));

        servicio.consolidar(PERIODO, QUIEN);

        verify(consolidacion).guardarConsolidacion(VERSION, new Consolidacion(QUIEN, AHORA));
    }

    @Test
    void dadaLaVersion_cuandoSeConsolida_entoncesVaAlRegistro() {
        when(consultar.consolidado(PERIODO)).thenReturn(consolidado(true, 0));

        servicio.consolidar(PERIODO, QUIEN);

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeDistributivo.Accion.DISTRIBUTIVO_CONSOLIDADO);
        assertThat(cambio.getValue().detalle()).containsEntry("periodo", PERIODO);
        assertThat(cambio.getValue().anterior()).isEmpty();
        assertThat(cambio.getValue().nuevo())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("consolidadoPor", QUIEN, "consolidadoEn", AHORA.toString()));
    }

    @Test
    void dadaUnaVersionYaConsolidada_cuandoSeConsolidaOtraVez_entoncesRegistraLaAnterior() {
        Instant antes = AHORA.minusSeconds(3600);
        when(consultar.consolidado(PERIODO))
                .thenReturn(
                        consolidado(
                                true, 0, Optional.of(new Consolidacion("otra.persona", antes))));

        servicio.consolidar(PERIODO, QUIEN);

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().anterior())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "consolidadoPor",
                                "otra.persona",
                                "consolidadoEn",
                                antes.toString()));
        assertThat(cambio.getValue().nuevo()).containsEntry("consolidadoPor", QUIEN);
    }

    @Test
    void dadaUnaConsolidacionAnterior_cuandoSeRepite_entoncesGuardaOtra() {
        when(consultar.consolidado(PERIODO)).thenReturn(consolidado(true, 0));

        servicio.consolidar(PERIODO, QUIEN);
        servicio.consolidar(PERIODO, QUIEN);

        verify(consolidacion, times(2)).guardarConsolidacion(any(), any());
    }

    @Test
    void conInconsistenciasAbiertas_cuandoSeConsolida_entoncesSeConsolidaIgual() {
        when(consultar.consolidado(PERIODO)).thenReturn(consolidado(true, 5));

        servicio.consolidar(PERIODO, QUIEN);

        verify(consolidacion).guardarConsolidacion(any(), any());
    }

    @Test
    void alConsolidar_cuandoTermina_entoncesDevuelveElConsolidadoActual() {
        ConsolidadoDelPeriodo antes = consolidado(true, 0);
        ConsolidadoDelPeriodo despues = consolidado(true, 1);
        when(consultar.consolidado(PERIODO)).thenReturn(antes, despues);

        assertThat(servicio.consolidar(PERIODO, QUIEN)).isSameAs(despues);
    }

    @Test
    void sinDistributivo_cuandoSeConsolida_entoncesLoRechaza() {
        when(consultar.consolidado(PERIODO))
                .thenReturn(ConsolidadoDelPeriodo.sinDistributivo(PERIODO, true));

        assertThatThrownBy(() -> servicio.consolidar(PERIODO, QUIEN))
                .isInstanceOf(SinDistributivo.class);

        assertNoGuardoNada();
    }

    @Test
    void conElPeriodoCerrado_cuandoSeConsolida_entoncesLoRechaza() {
        when(consultar.consolidado(PERIODO)).thenReturn(consolidado(false, 0));

        assertThatThrownBy(() -> servicio.consolidar(PERIODO, QUIEN))
                .isInstanceOf(PeriodoCerrado.class);

        assertNoGuardoNada();
    }

    private void assertNoGuardoNada() {
        verify(consolidacion, never()).guardarConsolidacion(any(), any());
        verify(registro, never()).registrar(any());
    }

    private static ConsolidadoDelPeriodo consolidado(boolean modificable, long duras) {
        return consolidado(modificable, duras, Optional.empty());
    }

    private static ConsolidadoDelPeriodo consolidado(
            boolean modificable, long duras, Optional<Consolidacion> anterior) {
        return new ConsolidadoDelPeriodo(
                PERIODO,
                modificable,
                Optional.of(VERSION),
                1,
                anterior,
                false,
                List.of(),
                new CargaHoraria(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO),
                duras,
                0,
                Optional.empty());
    }
}
