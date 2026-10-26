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

/** Las identificaciones que Docentes publica para Cargas (F01-H03), con Mockito. */
class ConsultarIdentificacionesServicioTest {

    private final RepositorioDocentes repositorio = mock(RepositorioDocentes.class);
    private final ConsultarIdentificacionesServicio servicio =
            new ConsultarIdentificacionesServicio(repositorio);

    @Test
    void dadosLos47DocentesDePrueba_entoncesDevuelveSusIdentificacionesDeCualquierEstado() {
        List<Docente> cargados = DocentesDePrueba.cargados();
        when(repositorio.todos()).thenReturn(cargados);

        assertThat(servicio.identificaciones())
                .hasSize(47)
                .contains(DocentesDePrueba.CEDULA)
                .containsAll(
                        cargados.stream()
                                .filter(docente -> docente.estado() != EstadoLaboral.ACTIVO)
                                .map(docente -> docente.identificacion().numero())
                                .toList());
    }

    @Test
    void sinDocentes_entoncesNinguna() {
        when(repositorio.todos()).thenReturn(List.of());

        assertThat(servicio.identificaciones()).isEmpty();
    }
}
