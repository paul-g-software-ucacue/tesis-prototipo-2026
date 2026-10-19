package ec.mdd.normativa.aplicacion;

import static ec.mdd.normativa.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.DIRECTA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.dos;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.politica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.AsignacionDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoConVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Asociar una versión a un periodo (FR-006, FR-009), con Mockito en los puertos. */
class AsociarVersionAPeriodoServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AsociarVersionAPeriodoServicio servicio =
            new AsociarVersionAPeriodoServicio(repositorio, registro, DIRECTA, RELOJ);

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeLeAsocia_entoncesSeBloqueaGuardaYRegistra() {
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.empty());
        when(repositorio.bloquear(1)).thenReturn(Optional.of(politica()));

        AsignacionDelPeriodo asignacion = servicio.asociar("2025-1", 1, QUIEN);

        assertThat(asignacion).isEqualTo(new AsignacionDelPeriodo("2025-1", 1, AHORA));
        InOrder orden = inOrder(repositorio, registro);
        orden.verify(repositorio).bloquearPeriodo("2025-1");
        orden.verify(repositorio).porPeriodo("2025-1");
        orden.verify(repositorio).bloquear(1);
        orden.verify(repositorio).guardar(politica().asociadaA("2025-1", AHORA), QUIEN, AHORA);
        orden.verify(registro)
                .registrar(
                        new CambioDeNormativa(
                                AHORA,
                                QUIEN,
                                Accion.VERSION_ASOCIADA,
                                Map.of("numero", 1, "periodo", "2025-1")));
    }

    @Test
    void dadoElPeriodoYaConEsaVersion_cuandoSeAsociaOtraVez_entoncesNoCambiaNiRegistra() {
        Instant antes = AHORA.minusSeconds(3600);
        when(repositorio.porPeriodo("2025-1"))
                .thenReturn(Optional.of(politica().asociadaA("2025-1", antes)));

        assertThat(servicio.asociar("2025-1", 1, QUIEN))
                .isEqualTo(new AsignacionDelPeriodo("2025-1", 1, antes));
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoElPeriodoConOtraVersion_cuandoSeAsocia_entoncesPeriodoConVersion() {
        VersionNormativa uno = politica().asociadaA("2025-1", AHORA);
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.of(uno));

        assertThatThrownBy(() -> servicio.asociar("2025-1", 2, QUIEN))
                .isInstanceOf(PeriodoConVersion.class)
                .hasMessageContaining("2025-1");
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeAsocia_entoncesVersionNoEncontrada() {
        when(repositorio.porPeriodo("2025-1")).thenReturn(Optional.empty());
        when(repositorio.bloquear(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asociar("2025-1", 9, QUIEN))
                .isInstanceOf(VersionNoEncontrada.class);
        verify(repositorio, never()).guardar(any(), anyString(), any());
    }

    @Test
    void dadoUnaVersionYaEnUsoEnOtroPeriodo_cuandoSeAsocia_entoncesRigeLosDos() {
        VersionNormativa enUso = dos().asociadaA("2025-1", AHORA.minusSeconds(60));
        when(repositorio.porPeriodo("2025-2")).thenReturn(Optional.empty());
        when(repositorio.bloquear(2)).thenReturn(Optional.of(enUso));

        servicio.asociar("2025-2", 2, QUIEN);

        verify(repositorio).guardar(enUso.asociadaA("2025-2", AHORA), QUIEN, AHORA);
    }
}
