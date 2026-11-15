package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La cadena de firmas completa pasa el distributivo a EN_OCS (F14-H03, D-104): con el proceso
 * bloqueado, guarda la transición y la registra en la bitácora; un aviso repetido o fuera de lugar
 * no cambia nada ni falla.
 */
class CompletarFirmasServicioTest {

    private static final Instant FIRMADA = Instant.parse("2026-11-14T12:00:00Z");
    private static final Transicion A_EN_OCS =
            new Transicion(
                    TipoDeTransicion.PASO_DE_ETAPA,
                    TipoDeEtapa.CONGELADO_EN_FIRMA,
                    TipoDeEtapa.EN_OCS,
                    Optional.empty(),
                    1,
                    "d".repeat(64),
                    "sistema",
                    FIRMADA);

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

    private final CompletarFirmasServicio servicio =
            new CompletarFirmasServicio(procesos, registro, transaccion);

    @Test
    void dadoElProcesoEnFirma_cuandoSeCompletaLaCadena_entoncesGuardaLaTransicionYLaRegistra() {
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(proceso);
                        });
        when(proceso.completarFirmas(1, FIRMADA)).thenReturn(List.of(A_EN_OCS));

        servicio.completar("2026-I", 1, FIRMADA);

        verify(procesos).guardar(proceso, List.of(A_EN_OCS));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeFlujo.Accion.CADENA_DE_FIRMAS_COMPLETA);
        assertThat(cambio.getValue().usuario()).isEqualTo("sistema");
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1);
        assertThat(cambio.getValue().nuevo()).containsEntry("estado", "EN_OCS");
    }

    @Test
    void
            dadoElAvisoRepetidoOFueraDeLugar_cuandoSeCompletaLaCadena_entoncesNoGuardaNiRegistraNiFalla() {
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(proceso));
        when(proceso.completarFirmas(1, FIRMADA)).thenReturn(List.of());

        servicio.completar("2026-I", 1, FIRMADA);

        verify(procesos, never()).guardar(any(), anyList());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoSinProceso_cuandoSeCompletaLaCadena_entoncesNoFalla() {
        when(procesos.bloquear("2026-I")).thenReturn(Optional.empty());

        servicio.completar("2026-I", 1, FIRMADA);

        verify(registro, never()).registrar(any());
    }
}
