package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.dominio.DesignacionInvalida;
import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoCerrado;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.entrada.PeriodoDesconocido;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Designar al Responsable de consolidación (FR-001, FR-004 a FR-006, FR-008), con Mockito. */
class DesignarResponsableServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    private static final String QUIEN = "vicerrectorado.demo";

    private final RepositorioPeriodosConocidos periodos = mock(RepositorioPeriodosConocidos.class);
    private final RepositorioDesignaciones designaciones = mock(RepositorioDesignaciones.class);
    private final RepositorioAsignacionesDeRol asignaciones =
            mock(RepositorioAsignacionesDeRol.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final DesignarResponsableServicio servicio =
            new DesignarResponsableServicio(
                    periodos,
                    designaciones,
                    asignaciones,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    private void conocido(EstadoConocido estado, Optional<Designacion> anterior) {
        when(periodos.bloquear("2026-2"))
                .thenReturn(Optional.of(new PeriodoConocido("2026-2", estado)));
        when(designaciones.dePeriodo("2026-2")).thenReturn(anterior);
        when(asignaciones.rolesDe("docente.demo")).thenReturn(List.of("DOCENTE"));
    }

    @Test
    void dadoUnPeriodoSinResponsable_cuandoSeDesigna_entoncesGuardaYRegistraElNuevo() {
        conocido(EstadoConocido.PLANIFICACION, Optional.empty());

        Designacion designada = servicio.designar("2026-2", "Docente.Demo", QUIEN);

        Designacion esperada = new Designacion("2026-2", "docente.demo", QUIEN, AHORA);
        assertThat(designada).isEqualTo(esperada);
        InOrder orden = inOrder(periodos, designaciones, registro);
        orden.verify(periodos).bloquear("2026-2");
        orden.verify(designaciones).guardar(esperada);
        orden.verify(registro)
                .registrar(
                        new CambioDeRoles(
                                AHORA,
                                QUIEN,
                                CambioDeRoles.Accion.DESIGNAR_RESPONSABLE,
                                Map.of("periodo", "2026-2", "nuevo", "docente.demo")));
    }

    @Test
    void dadoUnPeriodoConOtroResponsable_cuandoSeDesigna_entoncesLoReemplazaYRegistraElAnterior() {
        conocido(
                EstadoConocido.VIGENTE,
                Optional.of(new Designacion("2026-2", "docente.coordinador.demo", QUIEN, AHORA)));

        servicio.designar("2026-2", "docente.demo", QUIEN);

        verify(registro)
                .registrar(
                        new CambioDeRoles(
                                AHORA,
                                QUIEN,
                                CambioDeRoles.Accion.DESIGNAR_RESPONSABLE,
                                Map.of(
                                        "periodo", "2026-2",
                                        "anterior", "docente.coordinador.demo",
                                        "nuevo", "docente.demo")));
    }

    @Test
    void dadoElMismoResponsable_cuandoSeDesignaOtraVez_entoncesNoGuardaNiRegistra() {
        Designacion anterior =
                new Designacion("2026-2", "docente.demo", QUIEN, AHORA.minusSeconds(60));
        conocido(EstadoConocido.PLANIFICACION, Optional.of(anterior));

        assertThat(servicio.designar("2026-2", "docente.demo", QUIEN)).isEqualTo(anterior);
        verify(designaciones, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoQueRolesNoConoce_cuandoSeDesigna_entoncesPeriodoDesconocido() {
        when(periodos.bloquear("2030-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.designar("2030-1", "docente.demo", QUIEN))
                .isInstanceOf(PeriodoDesconocido.class);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoCerradoOUnNoDocente_cuandoSeDesigna_entoncesSeRechazaSinRegistrar() {
        conocido(EstadoConocido.CERRADO, Optional.empty());
        assertThatThrownBy(() -> servicio.designar("2026-2", "docente.demo", QUIEN))
                .isInstanceOf(PeriodoCerrado.class);

        conocido(EstadoConocido.PLANIFICACION, Optional.empty());
        when(asignaciones.rolesDe("rectorado.demo")).thenReturn(List.of("RECTOR"));
        assertThatThrownBy(() -> servicio.designar("2026-2", "rectorado.demo", QUIEN))
                .isInstanceOf(DesignacionInvalida.class);
        verify(designaciones, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoLasAsignacionesDeRol_cuandoSePidenLosDocentes_entoncesSoloLosQueTienenElRol() {
        when(asignaciones.todas())
                .thenReturn(
                        Map.of(
                                "docente.demo", List.of("DOCENTE"),
                                "docente.coordinador.demo",
                                        List.of("COORDINADOR_CARRERA", "DOCENTE"),
                                "rectorado.demo", List.of("RECTOR")));

        assertThat(servicio.docentes()).containsExactly("docente.coordinador.demo", "docente.demo");
    }

    @Test
    void dadoDesignaciones_cuandoSeListan_entoncesTodas() {
        Designacion una = new Designacion("2026-2", "docente.demo", QUIEN, AHORA);
        when(designaciones.todas()).thenReturn(List.of(una));

        assertThat(servicio.responsables()).containsExactly(una);
    }
}
