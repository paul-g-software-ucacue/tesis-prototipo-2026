package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso.EstadoCompartido;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Si el consolidado admite cambios según la etapa del proceso (#215, D-99, research §1 y §5). */
class EtapaDelConsolidadoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final EtapaDelConsolidadoServicio servicio = new EtapaDelConsolidadoServicio(procesos);

    @ParameterizedTest
    @EnumSource(TipoDeEtapa.class)
    void dadoElProcesoEnCadaEtapa_cuandoSePregunta_entoncesSigueLaRegla(TipoDeEtapa etapa) {
        when(procesos.estadoCompartido(PERIODO))
                .thenReturn(Optional.of(new EstadoCompartido(etapa, CREADO)));

        assertThat(servicio.admiteCambios(PERIODO, CREADO))
                .isEqualTo(etapa.admiteCambiosEnElConsolidado());
    }

    @Test
    void dadoUnPeriodoSinProceso_cuandoSePregunta_entoncesAdmiteCambios() {
        when(procesos.estadoCompartido(PERIODO)).thenReturn(Optional.empty());

        assertThat(servicio.admiteCambios(PERIODO, CREADO)).isTrue();
    }

    @Test
    void dadoElProcesoDeOtroDistributivo_cuandoSePregunta_entoncesAdmiteCambios() {
        when(procesos.estadoCompartido(PERIODO))
                .thenReturn(
                        Optional.of(
                                new EstadoCompartido(
                                        TipoDeEtapa.EN_REVISION_VRA,
                                        Instant.parse("2026-10-01T15:00:00Z"))));

        assertThat(servicio.admiteCambios(PERIODO, CREADO)).isTrue();
    }
}
