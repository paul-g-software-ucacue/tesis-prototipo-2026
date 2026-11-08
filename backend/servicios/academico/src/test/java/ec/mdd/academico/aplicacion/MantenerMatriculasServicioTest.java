package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.academico.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Los matriculados se registran y se corrigen con su anterior y su nuevo (F01-H04, F17-H01). */
class MantenerMatriculasServicioTest {

    private static final String PERIODO = "2026-I";
    private static final LocalDate CORTE = LocalDate.of(2026, 10, 1);

    private final RepositorioMatriculas repositorio = mock(RepositorioMatriculas.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final MantenerMatriculasServicio servicio =
            new MantenerMatriculasServicio(
                    repositorio, registro, new DatosDePrueba.TransaccionDePrueba(), RELOJ);

    private CambioRegistrado registrado() {
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }

    @Test
    void unosMatriculadosNuevos_cuandoSeRegistran_entoncesSoloLlevanNuevo() {
        Matricula matricula = new Matricula(PERIODO, "C1", 120, CORTE, "SIGA");

        servicio.registrar(matricula, QUIEN);

        verify(repositorio).registrar(matricula, QUIEN, AHORA);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.MATRICULA_REGISTRADA);
        assertThat(cambio.detalle()).containsEntry("periodo", PERIODO).containsEntry("total", 120);
        assertThat(cambio.anterior()).isEmpty();
        assertThat(cambio.nuevo())
                .isEqualTo(Map.of("total", 120, "fechaCorte", "2026-10-01", "fuente", "SIGA"));
    }

    @Test
    void unosMatriculadosQueExisten_cuandoSeCorrigen_entoncesLlevanElAnteriorYElNuevo() {
        when(repositorio.deLosPeriodos(List.of(PERIODO)))
                .thenReturn(
                        List.of(
                                new Matricula(PERIODO, "C2", 80, CORTE, null),
                                new Matricula(PERIODO, "C1", 100, CORTE, null)));
        Matricula corregida = new Matricula(PERIODO, "C1", 120, CORTE, "SIGA");

        servicio.corregir(corregida, QUIEN);

        verify(repositorio).guardar(corregida);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.MATRICULA_CORREGIDA);
        Map<String, Object> anterior = new HashMap<>();
        anterior.put("total", 100);
        anterior.put("fechaCorte", "2026-10-01");
        anterior.put("fuente", null);
        assertThat(cambio.anterior()).isEqualTo(anterior);
        assertThat(cambio.nuevo()).containsEntry("total", 120).containsEntry("fuente", "SIGA");
    }

    @Test
    void unosMatriculadosConLosMismosValores_cuandoSeCorrigen_entoncesNoGuardaNiRegistra() {
        // D-94: la carga 10 corrige una fila que no cambió; no es un cambio.
        Matricula habia = new Matricula(PERIODO, "C1", 100, CORTE, "SIGA");
        when(repositorio.deLosPeriodos(List.of(PERIODO))).thenReturn(List.of(habia));

        servicio.corregir(new Matricula(PERIODO, "C1", 100, CORTE, "SIGA"), QUIEN);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void unosMatriculadosConOtraFuente_cuandoSeCorrigen_entoncesEsUnCambio() {
        when(repositorio.deLosPeriodos(List.of(PERIODO)))
                .thenReturn(List.of(new Matricula(PERIODO, "C1", 100, CORTE, "SIGA")));
        Matricula otraFuente = new Matricula(PERIODO, "C1", 100, CORTE, "Otra fuente");

        servicio.corregir(otraFuente, QUIEN);

        verify(repositorio).guardar(otraFuente);
        verify(registro).registrar(any());
    }

    @Test
    void unosMatriculadosQueNoExisten_cuandoSeCorrigen_entoncesNoGuardaNiRegistra() {
        when(repositorio.deLosPeriodos(List.of(PERIODO))).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                servicio.corregir(
                                        new Matricula(PERIODO, "C1", 120, CORTE, null), QUIEN))
                .isInstanceOf(IllegalStateException.class);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }
}
