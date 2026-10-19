package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La consulta de las versiones de normativa (RF-19), con Mockito en el repositorio. */
class ConsultarNormativaServicioTest {

    private static final VersionNormativa VERSION_1 =
            new VersionNormativa(
                    1,
                    "SENESCYT-2024-03",
                    "Política SENESCYT 2024",
                    LocalDate.of(2024, 3, 11),
                    TipoIst.T2,
                    new TablaCargaHoraria(
                            List.of(
                                    new FilaCargaHoraria(
                                            null,
                                            "DOCENTE_TC",
                                            40,
                                            22,
                                            18,
                                            24,
                                            18,
                                            0,
                                            null,
                                            Severidad.DURA,
                                            "POL 6",
                                            null))),
                    List.of(
                            new Regla(
                                    "RN-02",
                                    Severidad.DURA,
                                    Map.of("horas_maximas_docente", BigDecimal.valueOf(40)))));

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final ConsultarNormativaServicio servicio = new ConsultarNormativaServicio(repositorio);

    @Test
    void dadoLasVersiones_cuandoSeListan_entoncesVienenDelRepositorio() {
        when(repositorio.todas()).thenReturn(List.of(VERSION_1));

        assertThat(servicio.versiones()).containsExactly(VERSION_1);
    }

    @Test
    void dadoLaVersionUno_cuandoSeConsulta_entoncesLaDevuelve() {
        when(repositorio.buscar(1)).thenReturn(Optional.of(VERSION_1));

        assertThat(servicio.version(1)).isEqualTo(VERSION_1);
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeConsulta_entoncesVersionNoEncontrada() {
        when(repositorio.buscar(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.version(7))
                .isInstanceOf(VersionNoEncontrada.class)
                .hasMessageContaining("7");
    }

    @Test
    void dadoUnPeriodoConVersion_cuandoSeConsulta_entoncesDevuelveSuAsignacion() {
        java.time.Instant momento = java.time.Instant.parse("2026-10-18T15:00:00Z");
        when(repositorio.porPeriodo("2025-1"))
                .thenReturn(Optional.of(VERSION_1.asociadaA("2025-1", momento)));

        assertThat(servicio.periodo("2025-1"))
                .isEqualTo(
                        new ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo(
                                "2025-1", 1, momento));
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(repositorio.porPeriodo("2030-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.periodo("2030-1"))
                .isInstanceOf(ec.mdd.normativa.puertos.entrada.PeriodoSinVersion.class)
                .hasMessageContaining("2030-1");
    }
}
