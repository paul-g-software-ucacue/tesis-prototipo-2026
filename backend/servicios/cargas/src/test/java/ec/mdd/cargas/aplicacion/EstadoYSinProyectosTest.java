package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.ConfirmacionSinProyectos;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.EstadoDeCarga;
import ec.mdd.cargas.dominio.EstadoDeLaCarga;
import ec.mdd.cargas.puertos.entrada.PeriodoConProyectos;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El estado de las cargas y la confirmación «sin proyectos» (F01-H05), con Mockito. */
class EstadoYSinProyectosTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00Z");
    private static final String PROYECTOS = "proyectos_practicas";

    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);
    private final AplicadorDeCargas dueno = mock(AplicadorDeCargas.class);
    private final HistorialDeCargas historial = mock(HistorialDeCargas.class);
    private final ProyectosDelPeriodo proyectos = mock(ProyectosDelPeriodo.class);
    private final ConsultarEstadoDeLasCargasServicio estado;
    private final ConfirmarSinProyectosServicio sinProyectos;

    EstadoYSinProyectosTest() {
        when(fuente.leer()).thenReturn(new DiccionarioYaml().leer());
        when(dueno.cargas()).thenReturn(ValidarArchivoServicioTest.CARGAS);
        ConsultarPlantillasServicio plantillas = new ConsultarPlantillasServicio(fuente);
        DuenosDeLasCargas duenos =
                new DuenosDeLasCargas(List.of(dueno), ValidarArchivoServicioTest.CARGAS);
        estado = new ConsultarEstadoDeLasCargasServicio(plantillas, duenos, historial, proyectos);
        sinProyectos =
                new ConfirmarSinProyectosServicio(
                        plantillas, proyectos, historial, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private EstadoDeCarga deProyectos() {
        return estado.estados().stream()
                .filter(una -> una.codigo().equals(PROYECTOS))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void elEstadoDeLasDiez_saleDeLoQueDiceCadaDueno() {
        when(dueno.tieneDatos("estructura_organica")).thenReturn(true);

        List<EstadoDeCarga> estados = estado.estados();

        assertThat(estados).hasSize(10);
        assertThat(estados.getFirst().estado()).isEqualTo(EstadoDeLaCarga.CARGADA);
        assertThat(estados.get(1).estado()).isEqualTo(EstadoDeLaCarga.PENDIENTE);
        assertThat(estados.get(1).faltan()).isEmpty();
    }

    @Test
    void unaConfirmacionSinProyectosVale_mientrasElPeriodoNoTengaAsignables() {
        when(historial.sinProyectos(PROYECTOS))
                .thenReturn(
                        List.of(
                                EntradaDeHistorial.sinProyectos(PROYECTOS, "2026-I", "x", AHORA),
                                EntradaDeHistorial.sinProyectos(PROYECTOS, "2026-II", "x", AHORA)));
        when(proyectos.hayAsignables("2026-I")).thenReturn(true);
        when(proyectos.hayAsignables("2026-II")).thenReturn(false);

        EstadoDeCarga deProyectos = deProyectos();

        assertThat(deProyectos.estado()).isEqualTo(EstadoDeLaCarga.SIN_PROYECTOS);
        assertThat(deProyectos.periodosSinProyectos())
                .as("2026-I ya tiene proyectos asignables: deja de valer")
                .containsExactly("2026-II");
    }

    @Test
    void unaConfirmacionDeUnPeriodoQueYaNoExiste_noVale() {
        when(historial.sinProyectos(PROYECTOS))
                .thenReturn(
                        List.of(EntradaDeHistorial.sinProyectos(PROYECTOS, "2026-I", "x", AHORA)));
        when(proyectos.hayAsignables("2026-I")).thenThrow(new PeriodoNoEncontrado("2026-I"));

        assertThat(deProyectos().estado()).isEqualTo(EstadoDeLaCarga.PENDIENTE);
    }

    @Test
    void sinProyectosDeUnPeriodoSinAsignables_seGuardaEnElHistorialDeLaCargaDeProyectos() {
        when(proyectos.hayAsignables("2026-II")).thenReturn(false);
        when(historial.confirmarSinProyectos(any()))
                .thenAnswer(uno -> new ConfirmacionSinProyectos(uno.getArgument(0), true));

        ConfirmacionSinProyectos confirmada =
                sinProyectos.confirmar("2026-II", "consolidacion.demo");

        ArgumentCaptor<EntradaDeHistorial> entrada =
                ArgumentCaptor.forClass(EntradaDeHistorial.class);
        verify(historial).confirmarSinProyectos(entrada.capture());
        assertThat(entrada.getValue())
                .isEqualTo(
                        EntradaDeHistorial.sinProyectos(
                                PROYECTOS, "2026-II", "consolidacion.demo", AHORA));
        assertThat(confirmada.nueva()).isTrue();
    }

    @Test
    void sinProyectosDeUnPeriodoConAsignables_esPeriodoConProyectosSinGuardar() {
        when(proyectos.hayAsignables("2026-II")).thenReturn(true);

        assertThatThrownBy(() -> sinProyectos.confirmar("2026-II", "consolidacion.demo"))
                .isInstanceOfSatisfying(
                        PeriodoConProyectos.class,
                        error -> assertThat(error.periodo()).isEqualTo("2026-II"));
        verify(historial, never()).confirmarSinProyectos(any());
    }

    @Test
    void sinProyectosDeUnPeriodoQueNoExiste_esPeriodoNoEncontradoSinGuardar() {
        when(proyectos.hayAsignables("2027-I")).thenThrow(new PeriodoNoEncontrado("2027-I"));

        assertThatThrownBy(() -> sinProyectos.confirmar("2027-I", "consolidacion.demo"))
                .isInstanceOf(PeriodoNoEncontrado.class);
        verify(historial, never()).confirmarSinProyectos(any());
    }

    @Test
    void sinProyectosRepetido_devuelveLaQueYaEstaba() {
        EntradaDeHistorial ya =
                EntradaDeHistorial.sinProyectos(PROYECTOS, "2026-II", "datos.demo", AHORA);
        when(proyectos.hayAsignables("2026-II")).thenReturn(false);
        when(historial.confirmarSinProyectos(any()))
                .thenReturn(new ConfirmacionSinProyectos(ya, false));

        assertThat(sinProyectos.confirmar("2026-II", "consolidacion.demo"))
                .isEqualTo(new ConfirmacionSinProyectos(ya, false));
    }
}
