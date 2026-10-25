package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.BusquedaDeDocentes;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Buscar y consultar los docentes (FR-007, FR-008), con Mockito. */
class ConsultarDocentesServicioTest {

    private final RepositorioDocentes repositorio = mock(RepositorioDocentes.class);
    private final ReferenciasAcademicas referencias = mock(ReferenciasAcademicas.class);
    private final ConsultarDocentesServicio servicio =
            new ConsultarDocentesServicio(repositorio, referencias);

    @Test
    void dadosLos47DocentesDePrueba_cuandoSeBuscaPorTextoYEstado_entoncesFiltraEnSuOrden() {
        List<Docente> cargados = DocentesDePrueba.cargados();
        when(repositorio.todos()).thenReturn(cargados);

        assertThat(servicio.buscar(BusquedaDeDocentes.todos())).hasSize(47);
        assertThat(servicio.buscar(new BusquedaDeDocentes(null, null, EstadoLaboral.LICENCIA)))
                .hasSize(1);
        assertThat(servicio.buscar(new BusquedaDeDocentes("peralta", null, null)))
                .isNotEmpty()
                .allSatisfy(
                        docente ->
                                assertThat(docente.datos().nombreCompleto()).contains("PERALTA"));
    }

    @Test
    void dadaUnaIdentificacion_cuandoSeConsulta_entoncesDevuelveElDocenteO404() {
        Docente docente = DocentesDePrueba.docente();
        when(repositorio.buscar(DocentesDePrueba.CEDULA)).thenReturn(Optional.of(docente));
        when(repositorio.buscar("0109990028")).thenReturn(Optional.empty());

        assertThat(servicio.docente(DocentesDePrueba.CEDULA)).isEqualTo(docente);
        assertThatThrownBy(() -> servicio.docente("0109990028"))
                .isInstanceOf(DocenteNoEncontrado.class);
    }

    @Test
    void dadasLasReferenciasDeAcademico_cuandoSePiden_entoncesSonLasDelPuerto() {
        when(referencias.referencias()).thenReturn(DocentesDePrueba.referencias());

        assertThat(servicio.referencias().carrera("C1")).contains("DESARROLLO DE SOFTWARE");
    }
}
