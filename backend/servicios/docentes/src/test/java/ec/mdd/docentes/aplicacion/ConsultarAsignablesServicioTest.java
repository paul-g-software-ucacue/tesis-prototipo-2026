package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.puertos.entrada.AsignablesDelPeriodo;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Si un periodo tiene proyectos asignables (F01-H05, F03-H05), con Mockito en los puertos de salida
 * y los 11 proyectos de la carga 09 de prueba.
 */
class ConsultarAsignablesServicioTest {

    private final List<Proyecto> deLaCarga = CargasDePrueba.proyectos();
    private final RepositorioProyectos repositorio = mock(RepositorioProyectos.class);
    private final ReferenciasAcademicas referencias = mock(ReferenciasAcademicas.class);
    private final ConsultarAsignablesServicio consultar =
            new ConsultarAsignablesServicio(repositorio, referencias);

    @BeforeEach
    void datos() {
        when(referencias.referencias()).thenReturn(CargasDePrueba.referencias());
        when(repositorio.todos()).thenReturn(deLaCarga);
    }

    @Test
    void conLosProyectosDePrueba_elPeriodo2026IITieneAlgunoAsignable() {
        assertThat(consultar.asignablesEn("2026-II")).isEqualTo(AsignablesDelPeriodo.ALGUNO);
    }

    @Test
    void conSoloProyectosFinalizadosOSuspendidos_elPeriodoNoTieneNinguno() {
        when(repositorio.todos())
                .thenReturn(
                        deLaCarga.stream()
                                .filter(
                                        proyecto ->
                                                proyecto.estado() == EstadoDeProyecto.FINALIZADO
                                                        || proyecto.estado()
                                                                == EstadoDeProyecto.SUSPENDIDO)
                                .toList());

        assertThat(consultar.asignablesEn("2026-II")).isEqualTo(AsignablesDelPeriodo.NINGUNO);
    }

    @Test
    void sinProyectos_elPeriodoNoTieneNinguno() {
        when(repositorio.todos()).thenReturn(List.of());

        assertThat(consultar.asignablesEn("2026-I")).isEqualTo(AsignablesDelPeriodo.NINGUNO);
    }

    @Test
    void unPeriodoQueNoEstaEnAcademico_esPeriodoNoEncontrado() {
        assertThat(consultar.asignablesEn("2027-I"))
                .isEqualTo(AsignablesDelPeriodo.PERIODO_NO_ENCONTRADO);
    }
}
