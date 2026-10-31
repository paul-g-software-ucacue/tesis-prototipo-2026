package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Candidato;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.HorasAsignadas;
import ec.mdd.plataforma.eventos.ProcesoConError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La sugerencia de una asignatura-paralelo, con los datos simulados con Mockito. */
class ConsultarSugerenciaServicioTest {

    private static final String PERIODO = "2026-I";

    private static final AsignaturaOfertada ASIGNATURA =
            new AsignaturaOfertada("POO-204", "Programación", "06", "061");

    private static final AsignaturaParalelo OFERTADO =
            new AsignaturaParalelo(ASIGNATURA, "A", "TDS", new BigDecimal("4"), false, false);

    private static final AsignaturaParalelo RETIRADO =
            new AsignaturaParalelo(ASIGNATURA, "A", "TDS", new BigDecimal("4"), false, true);

    private static final DocenteActivo DOCENTE =
            new DocenteActivo(
                    "0109990010",
                    "PEREZ LUIS",
                    Dedicacion.TC,
                    40,
                    new FormacionDelDocente(
                            new TituloDeTercerNivel("Ingeniería", "06", "061"),
                            List.of(new ElementoDeFormacion("Maestría", "061")),
                            List.of(),
                            List.of()));

    private static final PesosDelPeriodo PESOS =
            new PesosDelPeriodo(
                    2,
                    new PesosAfinidad(
                            new BigDecimal("7"),
                            new BigDecimal("4"),
                            new BigDecimal("5"),
                            new BigDecimal("3"),
                            new BigDecimal("2.5"),
                            new BigDecimal("1.5"),
                            new BigDecimal("4")),
                    new BigDecimal("3.5"));

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final DatosParaAfinidad afinidad = mock(DatosParaAfinidad.class);
    private final HorasAsignadas horas = mock(HorasAsignadas.class);
    private final ConsultarSugerenciaServicio servicio =
            new ConsultarSugerenciaServicio(datos, afinidad, horas);

    @Test
    void dadosLosDatos_cuandoSeConsulta_entoncesArmaLaSugerenciaConLasHoras() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.docentesActivos()).thenReturn(List.of(DOCENTE));
        when(horas.delPeriodo(PERIODO, "TDS"))
                .thenReturn(
                        Map.of(
                                DOCENTE.identificacion(),
                                new HorasDelDocente(new BigDecimal("10"), new BigDecimal("6"))));

        Sugerencia sugerencia = servicio.sugerencia(PERIODO, "POO-204", "A");

        assertThat(sugerencia.version()).isEqualTo(2);
        assertThat(sugerencia.minimoAfinidad()).isEqualByComparingTo("3.5");
        Candidato candidato = sugerencia.candidatos().getFirst();
        assertThat(candidato.puntaje().total()).isEqualByComparingTo("12");
        assertThat(candidato.horasDisponibles()).isEqualByComparingTo("30");
        assertThat(candidato.horas().enOtrasCarreras()).isEqualByComparingTo("6");
    }

    @Test
    void dadoUnParaleloQueNoExiste_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A"))
                .isInstanceOf(ParaleloNoEncontrado.class);
        verify(afinidad, never()).pesos(any());
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(RETIRADO));

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A"))
                .isInstanceOf(ParaleloRetirado.class);
        verify(afinidad, never()).pesos(any());
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinAbrir() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A"))
                .isInstanceOf(PeriodoSinAbrir.class);
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnFalloEntreProcesos_cuandoSeConsulta_entoncesSePropagaSinListaParcial() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.docentesActivos()).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A"))
                .isInstanceOf(ProcesoConError.class);
    }
}
