package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.academico.aplicacion.DatosDePrueba.CODIGO;
import static ec.mdd.academico.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.academico.aplicacion.DatosDePrueba.periodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
import ec.mdd.academico.puertos.salida.CambioDePeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Abrir un periodo con su versión de normativa (FR-002, FR-003, FR-008, research §3), con Mockito
 * en los puertos.
 */
class AbrirPeriodoServicioTest {

    private final RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final AbrirPeriodoServicio servicio =
            new AbrirPeriodoServicio(repositorio, normativa, registro, transaccion, RELOJ);

    private void existe(EstadoPeriodo estado, OptionalInt version) {
        when(repositorio.porCodigo(CODIGO)).thenReturn(Optional.of(periodo(estado)));
        when(normativa.versionDe(CODIGO)).thenReturn(version);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeAbre_entoncesNormativaLaAsociaFueraDeLaTransaccion() {
        existe(EstadoPeriodo.PLANIFICACION, OptionalInt.empty());
        List<String> pasos = new ArrayList<>();
        doAnswer(
                        llamada -> {
                            pasos.add("asociar dentro=" + transaccion.dentro());
                            return null;
                        })
                .when(normativa)
                .asociar(CODIGO, 2, QUIEN);
        doAnswer(
                        llamada -> {
                            pasos.add("registrar dentro=" + transaccion.dentro());
                            return null;
                        })
                .when(registro)
                .registrar(any());

        PeriodoConsultado abierto = servicio.abrir(CODIGO, 2, QUIEN);

        assertThat(abierto)
                .isEqualTo(
                        new PeriodoConsultado(
                                periodo(EstadoPeriodo.PLANIFICACION), OptionalInt.of(2)));
        // Cada servicio con su transacción: Normativa primero, y el registro solo si confirmó.
        assertThat(pasos).containsExactly("asociar dentro=false", "registrar dentro=true");
        verify(registro)
                .registrar(
                        new CambioDePeriodo(
                                AHORA,
                                QUIEN,
                                Accion.PERIODO_ABIERTO,
                                Map.of("codigo", CODIGO, "version", 2)));
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeAbre_entoncesPeriodoNoEncontrado() {
        when(repositorio.porCodigo(CODIGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 2, QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);
        verify(normativa, never()).asociar(anyString(), anyInt(), anyString());
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeAbre_entoncesPeriodoCerradoSinLlamarANormativa() {
        existe(EstadoPeriodo.CERRADO, OptionalInt.of(1));

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 1, QUIEN))
                .isInstanceOf(PeriodoCerrado.class);
        verify(normativa, never()).asociar(anyString(), anyInt(), anyString());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoLaMismaVersionQueYaTiene_cuandoSeAbre_entoncesNoCambiaNada() {
        existe(EstadoPeriodo.PLANIFICACION, OptionalInt.of(2));

        assertThat(servicio.abrir(CODIGO, 2, QUIEN).version()).hasValue(2);
        verify(normativa, never()).asociar(anyString(), anyInt(), anyString());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoOtraVersionQueLaQueTiene_cuandoSeAbre_entoncesPeriodoConOtraVersion() {
        existe(EstadoPeriodo.PLANIFICACION, OptionalInt.of(1));

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 2, QUIEN))
                .isInstanceOf(PeriodoConOtraVersion.class)
                .hasMessageContaining("versión de normativa 1");
        verify(normativa, never()).asociar(anyString(), anyInt(), anyString());
    }

    @Test
    void dadoUnPeriodoEnAprobacion_cuandoSeAbreConOtraOConLaMisma_entoncesRechazaONoCambia() {
        existe(EstadoPeriodo.EN_APROBACION, OptionalInt.of(1));

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 2, QUIEN))
                .isInstanceOf(PeriodoConOtraVersion.class);
        assertThat(servicio.abrir(CODIGO, 1, QUIEN).version()).hasValue(1);
        verify(normativa, never()).asociar(anyString(), anyInt(), anyString());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoQueNormativaRechazaLaAsociacion_cuandoSeAbre_entoncesNoRegistra() {
        existe(EstadoPeriodo.PLANIFICACION, OptionalInt.empty());
        doThrow(new PeriodoConOtraVersion(CODIGO, 1)).when(normativa).asociar(CODIGO, 2, QUIEN);

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 2, QUIEN))
                .isInstanceOf(PeriodoConOtraVersion.class);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeAbre_entoncesVersionInexistenteSinRegistro() {
        existe(EstadoPeriodo.PLANIFICACION, OptionalInt.empty());
        doThrow(new VersionInexistente(9)).when(normativa).asociar(CODIGO, 9, QUIEN);

        assertThatThrownBy(() -> servicio.abrir(CODIGO, 9, QUIEN))
                .isInstanceOf(VersionInexistente.class);
        verify(registro, never()).registrar(any());
    }
}
