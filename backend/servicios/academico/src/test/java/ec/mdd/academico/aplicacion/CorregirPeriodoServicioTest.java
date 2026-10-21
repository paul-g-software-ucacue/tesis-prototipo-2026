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

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.PeriodoCerrado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.CambioDePeriodo;
import ec.mdd.academico.puertos.salida.CambioDePeriodo.Accion;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Corregir el nombre y las fechas de un periodo (FR-005, FR-006, FR-008), con Mockito. */
class CorregirPeriodoServicioTest {

    private static final LocalDate FIN_NUEVO = LocalDate.of(2027, 3, 15);

    private final RepositorioPeriodos repositorio = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final CorregirPeriodoServicio servicio =
            new CorregirPeriodoServicio(
                    repositorio,
                    normativa,
                    registro,
                    new DatosDePrueba.TransaccionDePrueba(),
                    RELOJ);

    private void esta(EstadoPeriodo estado) {
        when(repositorio.bloquear(CODIGO)).thenReturn(Optional.of(periodo(estado)));
        when(normativa.versionDe(CODIGO)).thenReturn(OptionalInt.of(2));
    }

    @Test
    void dadoUnPeriodoVigente_cuandoSeCorrige_entoncesGuardaYRegistraCadaCambio() {
        esta(EstadoPeriodo.VIGENTE);

        PeriodoAcademico corregido =
                servicio.corregir(CODIGO, "Periodo académico 2026-2", INICIO, FIN_NUEVO, QUIEN)
                        .periodo();

        PeriodoAcademico esperado =
                new PeriodoAcademico(
                        CODIGO,
                        "Periodo académico 2026-2",
                        INICIO,
                        FIN_NUEVO,
                        EstadoPeriodo.VIGENTE);
        assertThat(corregido).isEqualTo(esperado);
        verify(repositorio).guardar(esperado);
        verify(registro)
                .registrar(
                        new CambioDePeriodo(
                                AHORA,
                                QUIEN,
                                Accion.PERIODO_CORREGIDO,
                                Map.of(
                                        "codigo",
                                        CODIGO,
                                        "cambios",
                                        List.of(
                                                Map.of(
                                                        "campo", "nombre",
                                                        "anterior", "Periodo 2026-2",
                                                        "nuevo", "Periodo académico 2026-2"),
                                                Map.of(
                                                        "campo", "fin",
                                                        "anterior", "2027-02-28",
                                                        "nuevo", "2027-03-15")))));
    }

    @Test
    void dadoLosMismosDatos_cuandoSeCorrige_entoncesNoGuardaNiRegistra() {
        esta(EstadoPeriodo.PLANIFICACION);

        assertThat(servicio.corregir(CODIGO, "Periodo 2026-2", INICIO, FIN, QUIEN).periodo())
                .isEqualTo(periodo(EstadoPeriodo.PLANIFICACION));
        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeCorrige_entoncesPeriodoCerradoSinGuardar() {
        esta(EstadoPeriodo.CERRADO);

        assertThatThrownBy(() -> servicio.corregir(CODIGO, "Otro", INICIO, FIN, QUIEN))
                .isInstanceOf(PeriodoCerrado.class);
        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeCorrige_entoncesPeriodoNoEncontrado() {
        when(repositorio.bloquear("2030-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.corregir("2030-1", "Otro", INICIO, FIN, QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }
}
