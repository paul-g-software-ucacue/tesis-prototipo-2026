package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ProporcionDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La meta de la proporción sale de los parámetros de RN-05 (principio VI). Con Mockito. */
class ConsultarProporcionDelPeriodoServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final ConsultarProporcionDelPeriodoServicio servicio =
            new ConsultarProporcionDelPeriodoServicio(repositorio);

    @Test
    void dadaLaMetaDeLaPolitica_cuandoSeConsulta_entoncesDevuelveSusPorcentajes() {
        con(Map.of("porcentaje_clase", "60", "porcentaje_otras_docencia", "40"));

        ProporcionDeLaVersion meta = servicio.proporcion("2025-1");

        assertThat(meta.version()).isEqualTo(3);
        assertThat(meta.clase()).isEqualByComparingTo("60");
        assertThat(meta.otrasDocencia()).isEqualByComparingTo("40");
    }

    @Test
    void dadaUnaVersionConOtraMeta_cuandoSeConsulta_entoncesDevuelveEsa() {
        con(Map.of("porcentaje_clase", "70", "porcentaje_otras_docencia", "30"));

        assertThat(servicio.proporcion("2025-1").clase()).isEqualByComparingTo("70");
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(repositorio.porPeriodo("2026-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proporcion("2026-1"))
                .isInstanceOf(PeriodoSinVersion.class);
    }

    @Test
    void dadaUnaVersionSinLaRegla_cuandoSeConsulta_entoncesFallaSinInventarla() {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.regla("RN-05")).thenReturn(Optional.empty());
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> servicio.proporcion("2025-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void con(Map<String, String> parametros) {
        VersionNormativa version = mock(VersionNormativa.class);
        when(version.numero()).thenReturn(3);
        Map<String, BigDecimal> valores =
                Map.of(
                        "porcentaje_clase", new BigDecimal(parametros.get("porcentaje_clase")),
                        "porcentaje_otras_docencia",
                                new BigDecimal(parametros.get("porcentaje_otras_docencia")));
        when(version.regla("RN-05"))
                .thenReturn(Optional.of(new Regla("RN-05", Severidad.BLANDA, valores)));
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(version));
    }
}
