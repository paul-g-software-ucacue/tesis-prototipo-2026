package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.MotivoNoAsignable;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.DocenteConProyecto;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Quiénes tienen proyectos asignables en un periodo, con sus tipos (RN-13). Con Mockito. */
class ConsultarDocentesConProyectoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String UNO = "0109990010";
    private static final String OTRO = "0109990020";

    private final ConsultarProyectos proyectos = mock(ConsultarProyectos.class);
    private final ConsultarDocentesConProyectoServicio servicio =
            new ConsultarDocentesConProyectoServicio(proyectos);

    @Test
    void dadosProyectos_cuandoSeConsulta_entoncesCadaDocenteTraeSusTipos() {
        when(proyectos.listar(PERIODO, null, null))
                .thenReturn(
                        List.of(
                                proyecto("A", TipoDeProyecto.INVESTIGACION, true, UNO, OTRO),
                                proyecto("B", TipoDeProyecto.VINCULACION, true, UNO)));

        assertThat(servicio.delPeriodo(PERIODO))
                .containsExactly(
                        new DocenteConProyecto(
                                UNO,
                                Set.of(TipoDeProyecto.INVESTIGACION, TipoDeProyecto.VINCULACION)),
                        new DocenteConProyecto(OTRO, Set.of(TipoDeProyecto.INVESTIGACION)));
    }

    @Test
    void dadoUnProyectoNoAsignable_cuandoSeConsulta_entoncesNoCuenta() {
        when(proyectos.listar(PERIODO, null, null))
                .thenReturn(List.of(proyecto("A", TipoDeProyecto.INVESTIGACION, false, UNO)));

        assertThat(servicio.delPeriodo(PERIODO)).isEmpty();
    }

    private static ProyectoConsultado proyecto(
            String codigo, TipoDeProyecto tipo, boolean asignable, String... participantes) {
        return new ProyectoConsultado(
                new Proyecto(
                        codigo,
                        "Monitoreo de hatos",
                        tipo,
                        List.of("550613A01-P-0301"),
                        "RES-OCS-1",
                        LocalDate.of(2025, 3, 20),
                        EstadoDeProyecto.EN_EJECUCION,
                        LocalDate.of(2025, 4, 1),
                        LocalDate.of(2026, 12, 31),
                        participantes[0],
                        List.of(participantes).stream()
                                .map(
                                        uno ->
                                                new Participante(
                                                        uno,
                                                        RolDeParticipante.DIRECTOR,
                                                        new BigDecimal("10")))
                                .toList(),
                        new BigDecimal("20")),
                Map.of(),
                Optional.of(
                        new Asignabilidad(
                                asignable,
                                asignable ? Set.of() : Set.of(MotivoNoAsignable.values()[0]))));
    }
}
