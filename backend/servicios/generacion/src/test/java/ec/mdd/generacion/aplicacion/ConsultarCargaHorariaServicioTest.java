package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.MetaDeProporcion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Proporcion;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.CargaDelDocente;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** La carga horaria de un docente activo, en todas las carreras (RF-27, D-82). Con Mockito. */
class ConsultarCargaHorariaServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String DOCENTE = "0109990010";
    private static final MetaDeProporcion META =
            new MetaDeProporcion(new BigDecimal("60"), new BigDecimal("40"));
    private static final AsignaturaParalelo DE_TDS = paralelo("BD-301", "TDS", "8");

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final DatosParaActividades normativa = mock(DatosParaActividades.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final ConsultarCargaHorariaServicio servicio =
            new ConsultarCargaHorariaServicio(datos, normativa, distributivos);

    @BeforeEach
    void preparar() {
        when(datos.docentesActivos()).thenReturn(List.of(activo()));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(DE_TDS)));
        when(normativa.meta(PERIODO)).thenReturn(Optional.of(META));
    }

    @Test
    void dadoUnDocente_cuandoSeConsulta_entoncesTraeSusTotalesSusHorasYLaMeta() {
        con(new ParcialCarrera[] {ParcialCarrera.nuevo("TDS")});

        CargaDelDocente carga = servicio.carga(PERIODO, DOCENTE);

        assertThat(carga.docente()).isEqualTo(DOCENTE);
        assertThat(carga.nombreDocente()).isEqualTo("PEREZ LUIS");
        assertThat(carga.horasContratadas()).isEqualTo(20);
        assertThat(carga.carga().clase()).isEqualByComparingTo("8");
        assertThat(carga.carga().otrasDocencia()).isEqualByComparingTo("4");
        assertThat(carga.carga().proporcion()).contains(new Proporcion(67, 33));
        assertThat(carga.meta()).isEqualTo(META);
    }

    @Test
    void dadoUnParcialEnviado_cuandoSeConsulta_entoncesSusHorasCuentan() {
        con(new ParcialCarrera[] {new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO)});

        assertThat(servicio.carga(PERIODO, DOCENTE).carga().asignadas()).isEqualByComparingTo("12");
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entoncesNoPideSuEstado() {
        con(new ParcialCarrera[] {ParcialCarrera.nuevo("TDS")});

        servicio.carga(PERIODO, DOCENTE);

        verify(datos, never()).periodoListo(PERIODO);
    }

    @Test
    void cuandoSeConsulta_entoncesNoGuardaNada() {
        con(new ParcialCarrera[] {ParcialCarrera.nuevo("TDS")});

        servicio.carga(PERIODO, DOCENTE);

        verify(distributivos).vigenteDe(PERIODO);
        Mockito.verifyNoMoreInteractions(distributivos);
    }

    @Test
    void dadoUnDocenteNoActivo_cuandoSeConsulta_entoncesDocenteNoActivo() {
        when(datos.docentesActivos()).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.carga(PERIODO, DOCENTE))
                .isInstanceOf(DocenteNoActivo.class);
    }

    @Test
    void sinDistributivo_cuandoSeConsulta_entoncesSinDistributivo() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.carga(PERIODO, DOCENTE))
                .isInstanceOf(SinDistributivo.class);
    }

    @Test
    void sinVersionDeNormativa_cuandoSeConsulta_entoncesPeriodoSinAbrir() {
        con(new ParcialCarrera[] {ParcialCarrera.nuevo("TDS")});
        when(normativa.meta(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.carga(PERIODO, DOCENTE))
                .isInstanceOf(PeriodoSinAbrir.class);
    }

    private void con(ParcialCarrera[] parciales) {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                new VersionDistributivo(
                                        UUID.randomUUID(),
                                        1,
                                        EstadoDeVersion.EDITABLE,
                                        List.of(parciales),
                                        List.of(propuesta()),
                                        List.of(actividad()))));
    }

    private static DocenteActivo activo() {
        return new DocenteActivo(
                DOCENTE,
                "PEREZ LUIS",
                Dedicacion.TC,
                20,
                new FormacionDelDocente(
                        new TituloDeTercerNivel("Título", "06", "061"),
                        List.of(),
                        List.of(),
                        List.of()));
    }

    private static AsignaturaParalelo paralelo(String asignatura, String carrera, String horas) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada(asignatura, "Asignatura", "06", "061"),
                "A",
                carrera,
                new BigDecimal(horas),
                false,
                false);
    }

    private static Propuesta propuesta() {
        return new Propuesta(
                "BD-301",
                "A",
                "TDS",
                DOCENTE,
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.parse("2026-10-31T15:00:00Z"),
                "coordinacion.demo");
    }

    private static ActividadNoLectiva actividad() {
        return new ActividadNoLectiva(
                "TDS",
                DOCENTE,
                "PEREZ LUIS",
                "140-b",
                "Planificar",
                Componente.OTRAS_DOCENCIA,
                null,
                null,
                new BigDecimal("4"),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
