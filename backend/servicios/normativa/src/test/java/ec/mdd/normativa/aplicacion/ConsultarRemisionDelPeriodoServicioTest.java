package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.RemisionDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Los días de RN-23 salen del parámetro de la versión que rige el periodo (F12-H03, D-114,
 * principio VI). Con Mockito.
 */
class ConsultarRemisionDelPeriodoServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final ConsultarRemisionDelPeriodoServicio servicio =
            new ConsultarRemisionDelPeriodoServicio(repositorio);

    @Test
    void dadaLaPolitica_cuandoSeConsulta_entoncesDevuelveLos30Dias() {
        con("30");

        assertThat(servicio.remision("2025-1")).isEqualTo(new RemisionDeLaVersion(3, 30));
    }

    @Test
    void dadaUnaVersionConOtraCifra_cuandoSeConsulta_entoncesDevuelveEsa() {
        con("45");

        assertThat(servicio.remision("2025-1").diasAntesDelInicio()).isEqualTo(45);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(repositorio.porPeriodo("2026-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.remision("2026-1")).isInstanceOf(PeriodoSinVersion.class);
    }

    @Test
    void dadaUnaVersionSinLaRegla_cuandoSeConsulta_entoncesFallaSinInventarla() {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.regla("RN-23")).thenReturn(Optional.empty());
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> servicio.remision("2025-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void con(String dias) {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.numero()).thenReturn(3);
        when(version.regla("RN-23"))
                .thenReturn(
                        Optional.of(
                                new Regla(
                                        "RN-23",
                                        null,
                                        Map.of("dias_antes_del_inicio", new BigDecimal(dias)))));
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));
    }
}
