package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.PesosAfinidad;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.PesosDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Los pesos de la versión que rige un periodo (RN-24, ADR-023). Con Mockito. */
class ConsultarPesosDelPeriodoServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final ConsultarPesosDelPeriodoServicio servicio =
            new ConsultarPesosDelPeriodoServicio(repositorio);

    @Test
    void dadoUnPeriodoConVersion_cuandoSeConsulta_entoncesDevuelveSuVersionYSusPesos() {
        VersionNormativa version = mock(VersionNormativa.class);
        PesosAfinidad pesos = mock(PesosAfinidad.class);
        when(version.numero()).thenReturn(3);
        when(version.pesosAfinidad()).thenReturn(Optional.of(pesos));
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));

        assertThat(servicio.pesos("2025-1")).isEqualTo(new PesosDeLaVersion(3, pesos));
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(repositorio.porPeriodo("2026-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.pesos("2026-1")).isInstanceOf(PeriodoSinVersion.class);
    }

    @Test
    void dadaUnaVersionSinPesos_cuandoSeConsulta_entoncesFallaSinInventarlos() {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.pesosAfinidad()).thenReturn(Optional.empty());
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> servicio.pesos("2025-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
