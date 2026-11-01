package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.MotivoNoAsignable;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.CargoConsultado;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectosYCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Solo lo que sirve para asignar: asignable, del docente y vigente (RN-25). Con Mockito. */
class ConsultarProyectosYCargosServicioTest {

    private static final String CEDULA = DocentesDePrueba.CEDULA;
    private static final String OTRO = "0109990099";
    private static final String PERIODO = "2026-I";

    private final RepositorioDocentes docentes = mock(RepositorioDocentes.class);
    private final ConsultarProyectos proyectos = mock(ConsultarProyectos.class);
    private final ConsultarCargos cargos = mock(ConsultarCargos.class);
    private final ConsultarProyectosYCargosServicio servicio =
            new ConsultarProyectosYCargosServicio(docentes, proyectos, cargos);

    @Test
    void dadoUnDocente_cuandoSeConsulta_entoncesTraeSoloLosProyectosAsignablesDondeParticipa() {
        conElDocente();
        when(proyectos.listar(PERIODO, null, null))
                .thenReturn(
                        List.of(
                                proyecto("INV-1", CEDULA, true),
                                proyecto("INV-2", OTRO, true),
                                proyecto("INV-3", CEDULA, false)));

        ProyectosYCargos delDocente = servicio.delDocente(CEDULA, PERIODO);

        assertThat(delDocente.proyectos()).extracting(Proyecto::codigo).containsExactly("INV-1");
    }

    @Test
    void dadoUnDocente_cuandoSeConsulta_entoncesTraeSusCargosVigentes() {
        conElDocente();
        when(cargos.listar(EstadoDeCargo.VIGENTE, CEDULA))
                .thenReturn(
                        List.of(
                                new CargoConsultado(
                                        new Cargo(
                                                CEDULA,
                                                TipoDeCargo.RECTOR,
                                                LocalDate.of(2025, 4, 1),
                                                "RECT",
                                                null,
                                                null,
                                                "RES-1",
                                                null),
                                        "PEREZ LUIS",
                                        List.of())));

        assertThat(servicio.delDocente(CEDULA, PERIODO).cargos())
                .containsExactly(TipoDeCargo.RECTOR);
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(docentes.buscar(CEDULA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.delDocente(CEDULA, PERIODO))
                .isInstanceOf(DocenteNoEncontrado.class);
        verify(proyectos, never()).listar(PERIODO, null, null);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesPropagaElDatoInvalido() {
        conElDocente();
        when(proyectos.listar(PERIODO, null, null))
                .thenThrow(new DatoInvalido("periodo", "El periodo no existe"));

        assertThatThrownBy(() -> servicio.delDocente(CEDULA, PERIODO))
                .isInstanceOf(DatoInvalido.class);
    }

    private void conElDocente() {
        when(docentes.buscar(CEDULA)).thenReturn(Optional.of(DocentesDePrueba.docente()));
    }

    private static ProyectoConsultado proyecto(String codigo, String docente, boolean asignable) {
        return new ProyectoConsultado(
                new Proyecto(
                        codigo,
                        "Monitoreo de hatos",
                        TipoDeProyecto.INVESTIGACION,
                        List.of("550613A01-P-0301"),
                        "RES-OCS-1",
                        LocalDate.of(2025, 3, 20),
                        EstadoDeProyecto.EN_EJECUCION,
                        LocalDate.of(2025, 4, 1),
                        LocalDate.of(2026, 12, 31),
                        docente,
                        List.of(
                                new Participante(
                                        docente, RolDeParticipante.DIRECTOR, new BigDecimal("10"))),
                        new BigDecimal("20")),
                Map.of(),
                Optional.of(
                        new Asignabilidad(
                                asignable,
                                asignable ? Set.of() : Set.of(MotivoNoAsignable.values()[0]))));
    }
}
