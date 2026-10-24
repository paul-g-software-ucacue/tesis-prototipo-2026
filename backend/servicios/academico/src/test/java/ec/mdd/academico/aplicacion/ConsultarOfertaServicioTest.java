package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.OfertaDelPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Consultar la oferta de un periodo (FR-007): con sus asignaturas-paralelo y los datos maestros
 * para las horas, sin bloquear nada. Con Mockito.
 */
class ConsultarOfertaServicioTest {

    private final RepositorioPeriodos periodos = mock(RepositorioPeriodos.class);
    private final RepositorioOferta repositorio = mock(RepositorioOferta.class);
    private final RepositorioCarreras carreras = mock(RepositorioCarreras.class);
    private final RepositorioEstructura estructura = mock(RepositorioEstructura.class);
    private final ConsultarOfertaServicio servicio =
            new ConsultarOfertaServicio(periodos, repositorio, carreras, estructura);

    @Test
    void dadoUnPeriodo_cuandoSeConsultaSuOferta_entoncesTraeElPeriodoYSuOfertaSinBloquear() {
        PeriodoAcademico periodo =
                new PeriodoAcademico(
                        "2026-I",
                        "Periodo",
                        LocalDate.of(2026, 4, 6),
                        LocalDate.of(2026, 8, 7),
                        EstadoPeriodo.VIGENTE);
        when(periodos.porCodigo("2026-I")).thenReturn(Optional.of(periodo));
        when(repositorio.delPeriodo("2026-I")).thenReturn(List.of());

        OfertaDelPeriodo oferta = servicio.oferta("2026-I");

        assertThat(oferta.periodo()).isEqualTo(periodo);
        assertThat(oferta.modificable()).isFalse();
        assertThat(oferta.horasACubrir().total()).isZero();
        verify(repositorio, never()).bloquear();
        verify(carreras, never()).bloquear();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsultaSuOferta_entoncesLanzaPeriodoNoEncontrado() {
        when(periodos.porCodigo("NADA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.oferta("NADA")).isInstanceOf(PeriodoNoEncontrado.class);
    }
}
