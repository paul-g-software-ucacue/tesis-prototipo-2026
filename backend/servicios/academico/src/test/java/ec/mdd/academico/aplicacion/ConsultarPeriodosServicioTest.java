package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.CODIGO;
import static ec.mdd.academico.aplicacion.DatosDePrueba.periodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** La consulta de los periodos con su versión de normativa (FR-009), con Mockito en los puertos. */
class ConsultarPeriodosServicioTest {

    private final RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final ConsultarPeriodosServicio servicio =
            new ConsultarPeriodosServicio(repositorio, normativa);

    @Test
    void dadoPeriodosConYSinVersion_cuandoSeListan_entoncesCadaUnoConLaDeNormativa() {
        PeriodoAcademico anterior =
                PeriodoAcademico.nuevo(
                        "2026-1",
                        "Periodo 2026-1",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 8, 1));
        when(repositorio.todos())
                .thenReturn(List.of(periodo(EstadoPeriodo.EN_APROBACION), anterior));
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));
        when(normativa.versionDe("2026-1")).thenReturn(OptionalInt.empty());

        assertThat(servicio.todos())
                .containsExactly(
                        new PeriodoConsultado(
                                periodo(EstadoPeriodo.EN_APROBACION), OptionalInt.of(2)),
                        new PeriodoConsultado(anterior, OptionalInt.empty()));
    }

    @Test
    void dadoUnPeriodo_cuandoSeConsultaPorSuCodigo_entoncesConSuVersion() {
        when(repositorio.porCodigo(CODIGO))
                .thenReturn(Optional.of(periodo(EstadoPeriodo.PLANIFICACION)));
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.empty());

        assertThat(servicio.porCodigo(CODIGO))
                .isEqualTo(
                        new PeriodoConsultado(
                                periodo(EstadoPeriodo.PLANIFICACION), OptionalInt.empty()));
    }

    @Test
    void dadoUnCodigoQueNoExiste_cuandoSeConsulta_entoncesPeriodoNoEncontrado() {
        when(repositorio.porCodigo("2030-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.porCodigo("2030-1"))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }
}
