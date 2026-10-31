package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los docentes activos, para sugerirlos (RF-25, ADR-023). Con Mockito. */
class ConsultarDocentesActivosServicioTest {

    private final RepositorioDocentes repositorio = mock(RepositorioDocentes.class);
    private final ConsultarDocentesActivosServicio servicio =
            new ConsultarDocentesActivosServicio(repositorio);

    @Test
    void dadosDocentesEnCadaEstado_cuandoSeConsulta_entoncesSoloDevuelveLosActivos() {
        Docente activo = DocentesDePrueba.docente();
        when(repositorio.todos())
                .thenReturn(
                        List.of(
                                activo,
                                activo.conEstado(EstadoLaboral.LICENCIA),
                                activo.conEstado(EstadoLaboral.INACTIVO)));

        assertThat(servicio.activos()).containsExactly(activo);
    }

    @Test
    void dadosLosDocentesDePrueba_cuandoSeConsulta_entoncesVienenDeVariasCarrerasBase() {
        when(repositorio.todos()).thenReturn(DocentesDePrueba.cargados());

        List<Docente> activos = servicio.activos();

        assertThat(activos).hasSize(46);
        assertThat(activos.stream().map(docente -> docente.datos().carreraBase()).distinct())
                .hasSizeGreaterThan(1);
    }

    @Test
    void dadoQueNadieEstaActivo_cuandoSeConsulta_entoncesListaVacia() {
        when(repositorio.todos())
                .thenReturn(List.of(DocentesDePrueba.docente().conEstado(EstadoLaboral.INACTIVO)));

        assertThat(servicio.activos()).isEmpty();
    }
}
