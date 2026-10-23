package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.academico.aplicacion.DatosDePrueba.CODIGO;
import static ec.mdd.academico.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.academico.aplicacion.DatosDePrueba.periodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoSinVersion;
import ec.mdd.academico.dominio.TransicionNoPermitida;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.AvisosDelPeriodo;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Cambiar el estado de un periodo (FR-004, FR-008), con Mockito en los puertos. */
class CambiarEstadoPeriodoServicioTest {

    private final RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final AvisosDelPeriodo avisos = mock(AvisosDelPeriodo.class);
    private final CambiarEstadoPeriodoServicio servicio =
            new CambiarEstadoPeriodoServicio(
                    repositorio, normativa, registro, avisos, transaccion, RELOJ);

    private void esta(EstadoPeriodo estado) {
        when(repositorio.porCodigo(CODIGO)).thenReturn(Optional.of(periodo(estado)));
        when(repositorio.bloquear(CODIGO)).thenReturn(Optional.of(periodo(estado)));
    }

    @Test
    void dadoUnPeriodoAbiertoEnPlanificacion_cuandoPasaAEnAprobacion_entoncesGuardaYRegistra() {
        esta(EstadoPeriodo.PLANIFICACION);
        List<String> consultas = new ArrayList<>();
        when(normativa.versionDe(CODIGO))
                .thenAnswer(
                        llamada -> {
                            consultas.add("dentro=" + transaccion.dentro());
                            return OptionalInt.of(2);
                        });

        PeriodoConsultado cambiado = servicio.cambiar(CODIGO, EstadoPeriodo.EN_APROBACION, QUIEN);

        assertThat(cambiado)
                .isEqualTo(
                        new PeriodoConsultado(
                                periodo(EstadoPeriodo.EN_APROBACION), OptionalInt.of(2)));
        // Normativa se consulta fuera de la transacción de Académico (principio V).
        assertThat(consultas).isNotEmpty().allMatch("dentro=false"::equals);
        InOrder orden = inOrder(repositorio, registro);
        orden.verify(repositorio).bloquear(CODIGO);
        orden.verify(repositorio).guardar(periodo(EstadoPeriodo.EN_APROBACION));
        orden.verify(registro)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                QUIEN,
                                Accion.PERIODO_ESTADO_CAMBIADO,
                                Map.of(
                                        "codigo", CODIGO,
                                        "anterior", "PLANIFICACION",
                                        "nuevo", "EN_APROBACION")));
    }

    @Test
    void dadoUnCambioDeEstado_cuandoSeGuarda_entoncesAvisaARolesDentroDeLaTransaccion() {
        esta(EstadoPeriodo.VIGENTE);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));
        List<Boolean> dentro = new ArrayList<>();
        org.mockito.Mockito.doAnswer(
                        llamada -> {
                            dentro.add(transaccion.dentro());
                            return null;
                        })
                .when(avisos)
                .actualizado(periodo(EstadoPeriodo.CERRADO));

        servicio.cambiar(CODIGO, EstadoPeriodo.CERRADO, QUIEN);

        assertThat(dentro).containsExactly(true);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoPasaAEnAprobacion_entoncesPeriodoSinVersionSinGuardar() {
        esta(EstadoPeriodo.PLANIFICACION);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.empty());

        assertThatThrownBy(() -> servicio.cambiar(CODIGO, EstadoPeriodo.EN_APROBACION, QUIEN))
                .isInstanceOf(PeriodoSinVersion.class);
        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoVigente_cuandoSeCierra_entoncesGuardaYRegistra() {
        esta(EstadoPeriodo.VIGENTE);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));

        assertThat(servicio.cambiar(CODIGO, EstadoPeriodo.CERRADO, QUIEN).periodo().estado())
                .isEqualTo(EstadoPeriodo.CERRADO);
        verify(repositorio).guardar(periodo(EstadoPeriodo.CERRADO));
    }

    @Test
    void dadoUnSaltoOUnRetroceso_cuandoSeCambia_entoncesTransicionNoPermitidaSinGuardar() {
        esta(EstadoPeriodo.EN_APROBACION);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));

        assertThatThrownBy(() -> servicio.cambiar(CODIGO, EstadoPeriodo.PLANIFICACION, QUIEN))
                .isInstanceOf(TransicionNoPermitida.class);
        assertThatThrownBy(() -> servicio.cambiar(CODIGO, EstadoPeriodo.CERRADO, QUIEN))
                .isInstanceOf(TransicionNoPermitida.class);
        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoElMismoEstado_cuandoSeCambia_entoncesNoGuardaNiRegistra() {
        esta(EstadoPeriodo.VIGENTE);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));

        assertThat(servicio.cambiar(CODIGO, EstadoPeriodo.VIGENTE, QUIEN).periodo())
                .isEqualTo(periodo(EstadoPeriodo.VIGENTE));
        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
        verify(avisos, never()).actualizado(any());
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeCambia_entoncesPeriodoNoEncontrado() {
        when(repositorio.porCodigo("2030-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiar("2030-1", EstadoPeriodo.VIGENTE, QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }
}
