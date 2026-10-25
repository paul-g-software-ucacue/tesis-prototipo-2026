package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.MotivoNoAsignable;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.ProyectoConsultado;
import ec.mdd.docentes.puertos.entrada.ProyectoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ProyectoRepetido;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Mantener y consultar los proyectos aprobados (F03-H05, FR-007, FR-009, FR-012), con Mockito en
 * los puertos de salida y los 11 proyectos de la carga 09 de prueba.
 */
class ProyectosServiciosTest {

    private static final String QUIEN = "datos.demo";
    private static final Instant AHORA = Instant.parse("2026-10-25T15:00:00Z");

    private final Map<String, Docente> cargados = CargasDePrueba.docentes();
    private final List<Proyecto> deLaCarga = CargasDePrueba.proyectos();
    private final RepositorioProyectos repositorio = mock(RepositorioProyectos.class);
    private final RepositorioDocentes docentes = mock(RepositorioDocentes.class);
    private final ReferenciasAcademicas referencias = mock(ReferenciasAcademicas.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final MantenerProyectosServicio mantener =
            new MantenerProyectosServicio(
                    repositorio,
                    docentes,
                    referencias,
                    registro,
                    new TransaccionDePrueba(),
                    Clock.fixed(AHORA, ZoneOffset.UTC));
    private final ConsultarProyectosServicio consultar =
            new ConsultarProyectosServicio(repositorio, docentes, referencias);

    @BeforeEach
    void datos() {
        when(referencias.referencias()).thenReturn(CargasDePrueba.referencias());
        when(docentes.buscar(anyString()))
                .thenAnswer(uno -> Optional.ofNullable(cargados.get(uno.getArgument(0))));
        when(docentes.todos()).thenReturn(List.copyOf(cargados.values()));
        when(repositorio.buscar(anyString())).thenReturn(Optional.empty());
        when(repositorio.todos()).thenReturn(deLaCarga);
    }

    @Test
    void dadoUnProyectoValido_cuandoSeRegistra_entoncesBloqueaGuardaYRegistraConSusNombres() {
        Proyecto proyecto = deLaCarga.getFirst();

        ProyectoConsultado registrado = mantener.registrar(proyecto, QUIEN);

        assertThat(registrado.nombresDeDocentes()).containsKey(proyecto.director());
        InOrder orden = inOrder(referencias, repositorio, registro);
        orden.verify(referencias).referencias();
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).registrar(proyecto, QUIEN, AHORA);
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        orden.verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioRegistrado.Accion.PROYECTO_REGISTRADO);
        assertThat(cambio.getValue().detalle()).containsEntry("codigo", "INV-2025-01");
    }

    @Test
    void dadoUnCodigoRepetido_cuandoSeRegistra_entoncesProyectoRepetidoSinGuardar() {
        when(repositorio.buscar("INV-2025-01")).thenReturn(Optional.of(deLaCarga.getFirst()));

        assertThatThrownBy(() -> mantener.registrar(deLaCarga.getFirst(), QUIEN))
                .isInstanceOf(ProyectoRepetido.class);
        verify(repositorio, never()).registrar(any(), any(), any());
    }

    @Test
    void dadoUnParticipanteQueNoEsDocente_cuandoSeRegistra_entoncesDatoInvalidoConSuPosicion() {
        Proyecto proyecto = deLaCarga.getFirst();
        when(docentes.buscar(proyecto.participantes().get(1).docente()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantener.registrar(proyecto, QUIEN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("participantes[1].docente");
        verify(repositorio, never()).registrar(any(), any(), any());
    }

    @Test
    void dadoUnProyecto_cuandoSeSuspende_entoncesGuardaYRegistraElCambio() {
        Proyecto antes = deLaCarga.getFirst();
        when(repositorio.buscar(antes.codigo())).thenReturn(Optional.of(antes));
        Proyecto suspendido = conEstado(antes, EstadoDeProyecto.SUSPENDIDO);

        mantener.corregir(suspendido, QUIEN);

        verify(repositorio).guardar(suspendido);
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioRegistrado.Accion.PROYECTO_CORREGIDO);
    }

    @Test
    void
            dadoUnProyectoSinCambiosOQueNoExiste_cuandoSeCorrige_entoncesNoRegistraOProyectoNoEncontrado() {
        Proyecto antes = deLaCarga.getFirst();
        when(repositorio.buscar(antes.codigo())).thenReturn(Optional.of(antes));
        mantener.corregir(antes, QUIEN);
        verify(registro, never()).registrar(any());

        assertThatThrownBy(() -> mantener.corregir(deLaCarga.get(1), QUIEN))
                .isInstanceOf(ProyectoNoEncontrado.class);
    }

    @Test
    void dadosLos11ProyectosDePrueba_cuandoSeListanEn2026I_entoncesLasPracticasSonAsignables() {
        List<ProyectoConsultado> enElPrimero = consultar.listar("2026-I", null, null);

        assertThat(enElPrimero).hasSize(11);
        assertThat(enElPrimero).allMatch(uno -> uno.asignabilidad().orElseThrow().asignable());
    }

    @Test
    void
            dadosLos11ProyectosDePrueba_cuandoSeListanEn2026II_entoncesLasPracticasNoLoSonPorSuVigencia() {
        List<ProyectoConsultado> enElSegundo =
                consultar.listar("2026-II", TipoDeProyecto.PRACTICAS_PREPROFESIONALES, null);

        assertThat(enElSegundo).hasSize(7);
        assertThat(enElSegundo)
                .allSatisfy(
                        uno ->
                                assertThat(uno.asignabilidad().orElseThrow().motivos())
                                        .containsExactly(MotivoNoAsignable.VIGENCIA));
        Proyecto vinculacion =
                deLaCarga.stream()
                        .filter(uno -> uno.codigo().equals("VIN-2026-02"))
                        .findFirst()
                        .orElseThrow();
        when(repositorio.buscar("VIN-2026-02")).thenReturn(Optional.of(vinculacion));
        assertThat(consultar.proyecto("VIN-2026-02", "2026-II").asignabilidad())
                .hasValueSatisfying(uno -> assertThat(uno.asignable()).isTrue());
    }

    @Test
    void
            dadoUnPeriodoQueNoExisteOSinPeriodo_cuandoSeListan_entoncesDatoInvalidoOSinAsignabilidad() {
        assertThatThrownBy(() -> consultar.listar("2030-I", null, null))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("periodo");
        assertThat(consultar.listar(null, null, EstadoDeProyecto.EN_EJECUCION))
                .singleElement()
                .satisfies(uno -> assertThat(uno.asignabilidad()).isEmpty());
        assertThatThrownBy(() -> consultar.proyecto("NO-EXISTE", null))
                .isInstanceOf(ProyectoNoEncontrado.class);
    }

    private static Proyecto conEstado(Proyecto proyecto, EstadoDeProyecto estado) {
        return new Proyecto(
                proyecto.codigo(),
                proyecto.nombre(),
                proyecto.tipo(),
                proyecto.carreras(),
                proyecto.resolucionOcs(),
                proyecto.fechaResolucion(),
                estado,
                proyecto.vigenciaInicio(),
                proyecto.vigenciaFin(),
                proyecto.director(),
                proyecto.participantes(),
                proyecto.horasAprobadas());
    }
}
