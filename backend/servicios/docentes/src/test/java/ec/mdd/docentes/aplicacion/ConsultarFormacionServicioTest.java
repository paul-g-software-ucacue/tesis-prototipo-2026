package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La ficha de un docente, para calcular la afinidad (ADR-023). Con Mockito. */
class ConsultarFormacionServicioTest {

    private final RepositorioDocentes repositorio = mock(RepositorioDocentes.class);
    private final ConsultarFormacionServicio servicio = new ConsultarFormacionServicio(repositorio);

    @Test
    void dadoUnDocente_cuandoSeConsulta_entoncesDevuelveSuFicha() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));

        assertThat(servicio.formacion(DocentesDePrueba.CEDULA)).isEqualTo(DocentesDePrueba.ficha());
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSeConsulta_entoncesDocenteNoEncontrado() {
        when(repositorio.buscar("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.formacion("0000000000"))
                .isInstanceOf(DocenteNoEncontrado.class);
    }
}
