package ec.mdd.normativa.aplicacion;

import static ec.mdd.normativa.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.DIRECTA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.REFORMA;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.conClase;
import static ec.mdd.normativa.aplicacion.DatosDePrueba.dos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.CambioNoPermitido;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.FilaInvalida;
import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Editar una versión que no rige ningún periodo (FR-002 a FR-004, FR-009), con Mockito. */
class EditarVersionServicioTest {

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final EditarVersionServicio servicio =
            new EditarVersionServicio(repositorio, registro, DIRECTA, RELOJ);

    @Test
    void dadoUnaVersionSinPeriodo_cuandoSeCambianLasHoras_entoncesSeBloqueaGuardaYRegistra() {
        when(repositorio.bloquear(2)).thenReturn(Optional.of(dos()));

        VersionNormativa editada = servicio.editar(2, conClase(dos(), REFORMA, 10), QUIEN);

        assertThat(editada.tabla().filas().getFirst().horasClase()).isEqualTo(10);
        verify(repositorio).guardar(editada, QUIEN, AHORA);
        Map<String, Object> cambio = new LinkedHashMap<>();
        cambio.put("campo", "tablaCargaHoraria[DOCENTE_MT/TODOS].horasClase");
        cambio.put("anterior", "12");
        cambio.put("nuevo", "10");
        verify(registro)
                .registrar(
                        new CambioDeNormativa(
                                AHORA,
                                QUIEN,
                                Accion.VERSION_EDITADA,
                                Map.of("numero", 2, "cambios", List.of(cambio))));
        verify(repositorio, never()).buscar(2);
    }

    @Test
    void dadoUnaEdicionSinCambios_cuandoSeAplica_entoncesNoGuardaNiRegistra() {
        when(repositorio.bloquear(2)).thenReturn(Optional.of(dos()));

        assertThat(servicio.editar(2, conClase(dos(), REFORMA, 12), QUIEN)).isEqualTo(dos());
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnaVersionEnUso_cuandoSeEdita_entoncesVersionEnUsoSinGuardar() {
        VersionNormativa enUso = dos().asociadaA("2025-1", AHORA);
        when(repositorio.bloquear(2)).thenReturn(Optional.of(enUso));

        assertThatThrownBy(() -> servicio.editar(2, conClase(enUso, REFORMA, 10), QUIEN))
                .isInstanceOf(VersionEnUso.class);
        verify(repositorio, never()).guardar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnCodigoQueUsaOtraVersion_cuandoSeEdita_entoncesCodigoRepetido() {
        when(repositorio.bloquear(2)).thenReturn(Optional.of(dos()));
        when(repositorio.existeCodigo("SENESCYT-2024-03", 2)).thenReturn(true);
        DatosDeVersion conElDeLaUno =
                new DatosDeVersion("SENESCYT-2024-03", "Reforma", LocalDate.of(2025, 1, 15));

        assertThatThrownBy(() -> servicio.editar(2, conClase(dos(), conElDeLaUno, 12), QUIEN))
                .isInstanceOf(CodigoRepetido.class);
        verify(repositorio, never()).guardar(any(), anyString(), any());
    }

    @Test
    void dadoUnaFilaInvalidaOUnCambioNoPermitido_cuandoSeEdita_entoncesPasaSinGuardar() {
        when(repositorio.bloquear(2)).thenReturn(Optional.of(dos()));

        assertThatThrownBy(() -> servicio.editar(2, conClase(dos(), REFORMA, 13), QUIEN))
                .isInstanceOf(FilaInvalida.class);
        assertThatThrownBy(
                        () ->
                                servicio.editar(
                                        2,
                                        new ec.mdd.normativa.dominio.CambiosDeVersion(
                                                REFORMA, List.of(), Map.of()),
                                        QUIEN))
                .isInstanceOf(CambioNoPermitido.class);
        verify(repositorio, never()).guardar(any(), anyString(), any());
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeEdita_entoncesVersionNoEncontrada() {
        when(repositorio.bloquear(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(9, conClase(dos(), REFORMA, 12), QUIEN))
                .isInstanceOf(VersionNoEncontrada.class);
    }
}
