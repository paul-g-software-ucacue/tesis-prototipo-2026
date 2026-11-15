package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El rechazo en firma devuelve el distributivo a revisión (F14-H04, D-105): con el proceso
 * bloqueado, guarda la transición con su rechazo y la registra en la bitácora; un aviso repetido o
 * fuera de lugar no cambia nada ni falla.
 */
class RegistrarRechazoEnFirmaServicioTest {

    private static final Instant RECHAZADA = Instant.parse("2026-11-15T12:00:00Z");
    private static final RechazoDelFirmante RECHAZO =
            new RechazoDelFirmante(
                    2,
                    "VICERRECTOR",
                    "vicerrectorado.demo",
                    RECHAZADA,
                    new Observacion("Falta la carga de un docente de Software."));
    private static final Transicion A_REVISION =
            new Transicion(
                    TipoDeTransicion.RECHAZO_EN_FIRMA,
                    TipoDeEtapa.CONGELADO_EN_FIRMA,
                    TipoDeEtapa.EN_REVISION_VRA,
                    Optional.empty(),
                    1,
                    "d".repeat(64),
                    "sistema",
                    RECHAZADA,
                    Map.of(),
                    Optional.of(RECHAZO));

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ProcesoAprobacion proceso = mock(ProcesoAprobacion.class);
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

    private final RegistrarRechazoEnFirmaServicio servicio =
            new RegistrarRechazoEnFirmaServicio(procesos, registro, transaccion);

    @Test
    void dadoElProcesoEnFirma_cuandoSeRechaza_entoncesGuardaLaTransicionYLaRegistra() {
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(proceso);
                        });
        when(proceso.rechazarEnFirma(1, RECHAZO, RECHAZADA)).thenReturn(List.of(A_REVISION));

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(procesos).guardar(proceso, List.of(A_REVISION));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA);
        assertThat(cambio.getValue().usuario()).isEqualTo("sistema");
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1)
                .containsEntry("orden", 2)
                .containsEntry("rol", "VICERRECTOR")
                .containsEntry("rechazadoPor", "vicerrectorado.demo")
                .containsEntry("observacion", "Falta la carga de un docente de Software.");
        assertThat(cambio.getValue().anterior()).containsEntry("estado", "CONGELADO_EN_FIRMA");
        assertThat(cambio.getValue().nuevo()).containsEntry("estado", "EN_REVISION_VRA");
    }

    @Test
    void dadoElAvisoRepetidoOFueraDeLugar_cuandoSeRechaza_entoncesNoGuardaNiRegistraNiFalla() {
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(proceso));
        when(proceso.rechazarEnFirma(1, RECHAZO, RECHAZADA)).thenReturn(List.of());

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(procesos, never()).guardar(any(), anyList());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoSinProceso_cuandoSeRechaza_entoncesNoFalla() {
        when(procesos.bloquear("2026-I")).thenReturn(Optional.empty());

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(registro, never()).registrar(any());
    }
}
