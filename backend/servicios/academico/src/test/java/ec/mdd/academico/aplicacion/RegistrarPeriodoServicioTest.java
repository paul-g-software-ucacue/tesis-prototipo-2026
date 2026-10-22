package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.AHORA;
import static ec.mdd.academico.aplicacion.DatosDePrueba.CODIGO;
import static ec.mdd.academico.aplicacion.DatosDePrueba.FIN;
import static ec.mdd.academico.aplicacion.DatosDePrueba.INICIO;
import static ec.mdd.academico.aplicacion.DatosDePrueba.QUIEN;
import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static ec.mdd.academico.aplicacion.DatosDePrueba.periodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.puertos.entrada.CodigoRepetido;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.salida.AvisosDelPeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Registrar un periodo (FR-001, FR-006, FR-008), con Mockito en los puertos. */
class RegistrarPeriodoServicioTest {

    private final RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AvisosDelPeriodo avisos = mock(AvisosDelPeriodo.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final RegistrarPeriodoServicio servicio =
            new RegistrarPeriodoServicio(
                    repositorio, normativa, registro, avisos, transaccion, RELOJ);

    @Test
    void dadoUnCodigoNuevo_cuandoSeRegistra_entoncesQuedaEnPlanificacionYSeRegistra() {
        when(repositorio.registrar(periodo(EstadoPeriodo.PLANIFICACION), QUIEN, AHORA))
                .thenReturn(true);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.empty());

        PeriodoConsultado registrado =
                servicio.registrar(CODIGO, "Periodo 2026-2", INICIO, FIN, QUIEN);

        assertThat(registrado)
                .isEqualTo(
                        new PeriodoConsultado(
                                periodo(EstadoPeriodo.PLANIFICACION), OptionalInt.empty()));
        verify(registro)
                .registrar(
                        new CambioDePeriodo(
                                AHORA,
                                QUIEN,
                                Accion.PERIODO_REGISTRADO,
                                Map.of(
                                        "codigo", CODIGO,
                                        "nombre", "Periodo 2026-2",
                                        "inicio", "2026-09-01",
                                        "fin", "2027-02-28")));
    }

    @Test
    void dadoUnCodigoNuevo_cuandoSeRegistra_entoncesAvisaARolesDentroDeLaTransaccion() {
        when(repositorio.registrar(periodo(EstadoPeriodo.PLANIFICACION), QUIEN, AHORA))
                .thenReturn(true);
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.empty());
        java.util.List<Boolean> dentro = new java.util.ArrayList<>();
        org.mockito.Mockito.doAnswer(
                        llamada -> {
                            dentro.add(transaccion.dentro());
                            return null;
                        })
                .when(avisos)
                .actualizado(periodo(EstadoPeriodo.PLANIFICACION));

        servicio.registrar(CODIGO, "Periodo 2026-2", INICIO, FIN, QUIEN);

        // Con el registro de publicaciones, el aviso sale solo si la transacción se confirma.
        assertThat(dentro).containsExactly(true);
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entoncesCodigoRepetidoSinRegistro() {
        when(repositorio.registrar(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(CODIGO, "Otro", INICIO, FIN, QUIEN))
                .isInstanceOf(CodigoRepetido.class);
        verify(registro, never()).registrar(any());
        verify(avisos, never()).actualizado(any());
    }

    @Test
    void dadoUnDatoInvalido_cuandoSeRegistra_entoncesNoGuardaNada() {
        assertThatThrownBy(() -> servicio.registrar(CODIGO, "Periodo", FIN, INICIO, QUIEN))
                .isInstanceOf(DatoInvalido.class);
        verify(repositorio, never()).registrar(any(), any(), any());
        verify(registro, never()).registrar(any());
    }
}
