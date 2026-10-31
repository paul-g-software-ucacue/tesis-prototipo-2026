package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.AfinidadDelDocente;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.PuntajeAfinidad;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.plataforma.eventos.ProcesoConError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** La afinidad de un docente en la oferta de un periodo, con los datos simulados con Mockito. */
class ConsultarAfinidadServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String DOCENTE = "0109990010";

    private static final AsignaturaOfertada PROGRAMACION =
            new AsignaturaOfertada("POO-204", "Programación", "06", "061");
    private static final AsignaturaOfertada ANATOMIA =
            new AsignaturaOfertada("ANA-101", "Anatomía", "09", "091");

    private static final FormacionDelDocente FORMACION =
            new FormacionDelDocente(
                    new TituloDeTercerNivel("Ingeniería", "06", "061"),
                    List.of(new ElementoDeFormacion("Maestría", "061")),
                    List.of(),
                    List.of());

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
                            new BigDecimal("4")));

    private final DatosParaAfinidad datos = mock(DatosParaAfinidad.class);
    private final ConsultarAfinidadServicio servicio = new ConsultarAfinidadServicio(datos);

    @Test
    void dadosLosDatos_cuandoSeConsulta_entoncesCalculaConLaVersionDelPeriodoYOrdena() {
        when(datos.asignaturasOfertadas(PERIODO))
                .thenReturn(Optional.of(List.of(ANATOMIA, PROGRAMACION)));
        when(datos.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.formacion(DOCENTE)).thenReturn(Optional.of(FORMACION));

        AfinidadDelDocente afinidad = servicio.afinidad(DOCENTE, PERIODO);

        assertThat(afinidad.periodo()).isEqualTo(PERIODO);
        assertThat(afinidad.version()).isEqualTo(2);
        assertThat(afinidad.puntajes())
                .extracting(PuntajeAfinidad::codigo)
                .containsExactly("POO-204", "ANA-101");
        assertThat(afinidad.puntajes().getFirst().total()).isEqualByComparingTo("12");
        assertThat(afinidad.puntajes().getLast().total()).isEqualByComparingTo("0");
    }

    @Test
    void dadoUnPeriodoSinOferta_cuandoSeConsulta_entoncesListaVaciaConSuVersion() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.of(List.of()));
        when(datos.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.formacion(DOCENTE)).thenReturn(Optional.of(FORMACION));

        AfinidadDelDocente afinidad = servicio.afinidad(DOCENTE, PERIODO);

        assertThat(afinidad.puntajes()).isEmpty();
        assertThat(afinidad.version()).isEqualTo(2);
    }

    @Test
    void dadosLosDatos_cuandoSeConsulta_entoncesPideOfertaPesosYFormacionEnEseOrden() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.of(List.of()));
        when(datos.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.formacion(DOCENTE)).thenReturn(Optional.of(FORMACION));

        servicio.afinidad(DOCENTE, PERIODO);

        InOrder orden = inOrder(datos);
        orden.verify(datos).asignaturasOfertadas(PERIODO);
        orden.verify(datos).pesos(PERIODO);
        orden.verify(datos).formacion(DOCENTE);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesPeriodoNoEncontradoYNoSigue() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.afinidad(DOCENTE, PERIODO))
                .isInstanceOf(PeriodoNoEncontrado.class);

        verify(datos, never()).pesos(any());
        verify(datos, never()).formacion(any());
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinAbrirYNoPideLaFicha() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.of(List.of(PROGRAMACION)));
        when(datos.pesos(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.afinidad(DOCENTE, PERIODO))
                .isInstanceOf(PeriodoSinAbrir.class);

        verify(datos, never()).formacion(any());
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSeConsulta_entoncesDocenteNoEncontrado() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.of(List.of(PROGRAMACION)));
        when(datos.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.formacion(DOCENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.afinidad(DOCENTE, PERIODO))
                .isInstanceOf(DocenteNoEncontrado.class);
    }

    @Test
    void dadoOtroProcesoQueFalla_cuandoSeConsulta_entoncesPropagaElFalloSinPuntajeParcial() {
        when(datos.asignaturasOfertadas(PERIODO)).thenReturn(Optional.of(List.of(PROGRAMACION)));
        when(datos.pesos(PERIODO)).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.afinidad(DOCENTE, PERIODO))
                .isInstanceOf(ProcesoConError.class);
    }
}
