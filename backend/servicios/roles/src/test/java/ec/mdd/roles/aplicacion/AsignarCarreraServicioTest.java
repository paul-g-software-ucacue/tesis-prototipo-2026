package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.puertos.entrada.CarreraSinRol;
import ec.mdd.roles.puertos.entrada.UsuarioSinCarrera;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Indicar y quitar la carrera de un coordinador (F06-H05, D-64), con Mockito. */
class AsignarCarreraServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-31T15:00:00Z");

    private final RepositorioCarrerasDeCoordinadores carreras =
            mock(RepositorioCarrerasDeCoordinadores.class);
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
    private final AsignarCarreraServicio servicio =
            new AsignarCarreraServicio(
                    carreras,
                    asignaciones,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnCoordinador_cuandoSeIndicaSuCarrera_entoncesSeGuardaYSeRegistra() {
        when(asignaciones.rolesDe("coordinacion.demo")).thenReturn(List.of("COORDINADOR_CARRERA"));

        servicio.indicar("coordinacion.demo", "DS-01", "admin.demo");

        verify(carreras).guardar("coordinacion.demo", "DS-01", "admin.demo", AHORA);
        CambioDeRoles cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(CambioDeRoles.Accion.INDICAR_CARRERA);
        assertThat(cambio.usuario()).isEqualTo("admin.demo");
        assertThat(cambio.detalle())
                .isEqualTo(Map.of("usuario", "coordinacion.demo", "carrera", "DS-01"));
    }

    @Test
    void dadoUnUsuarioConMayusculas_cuandoSeIndicaSuCarrera_entoncesSeNormaliza() {
        when(asignaciones.rolesDe("coordinacion.demo")).thenReturn(List.of("COORDINADOR_CARRERA"));

        servicio.indicar(" Coordinacion.Demo ", "DS-01", "admin.demo");

        verify(carreras).guardar("coordinacion.demo", "DS-01", "admin.demo", AHORA);
    }

    @Test
    void dadoQuienNoTieneElRol_cuandoSeIndicaSuCarrera_entoncesCarreraSinRolSinGuardar() {
        when(asignaciones.rolesDe("docente.demo")).thenReturn(List.of("DOCENTE"));

        assertThatThrownBy(() -> servicio.indicar("docente.demo", "DS-01", "admin.demo"))
                .isInstanceOf(CarreraSinRol.class);
        verify(carreras, never()).guardar(anyString(), anyString(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnCoordinadorConCarrera_cuandoSeQuita_entoncesSeBorraYSeRegistra() {
        when(carreras.quitar("coordinacion.demo")).thenReturn(true);

        servicio.quitar("coordinacion.demo", "admin.demo");

        assertThat(registrado().accion()).isEqualTo(CambioDeRoles.Accion.QUITAR_CARRERA);
    }

    @Test
    void dadoQuienNoTieneCarrera_cuandoSeQuita_entoncesUsuarioSinCarrera() {
        assertThatThrownBy(() -> servicio.quitar("coordinacion.demo", "admin.demo"))
                .isInstanceOf(UsuarioSinCarrera.class);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadasLasCarreras_cuandoSeListan_entoncesVienenDelRepositorio() {
        when(carreras.todas()).thenReturn(Map.of("coordinacion.demo", "DS-01"));

        assertThat(servicio.carreras()).containsEntry("coordinacion.demo", "DS-01");
    }

    private CambioDeRoles registrado() {
        ArgumentCaptor<CambioDeRoles> cambio = ArgumentCaptor.forClass(CambioDeRoles.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }
}
