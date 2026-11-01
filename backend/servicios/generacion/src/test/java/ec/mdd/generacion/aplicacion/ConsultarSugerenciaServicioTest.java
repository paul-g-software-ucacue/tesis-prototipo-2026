package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Candidato;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SugerenciaDelParalelo;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La sugerencia de una asignatura-paralelo, con los datos simulados con Mockito. */
class ConsultarSugerenciaServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Optional<String> TODAS = Optional.empty();

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

    private static final DocenteActivo AJENO =
            new DocenteActivo(
                    "0109990020",
                    "LOPEZ ANA",
                    Dedicacion.TC,
                    40,
                    new FormacionDelDocente(
                            new TituloDeTercerNivel("Medicina", "09", "091"),
                            List.of(),
                            List.of(),
                            List.of()));

    private static final AsignaturaOfertada BD =
            new AsignaturaOfertada("BD-301", "Bases de datos", "06", "061");

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
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final ConsultarSugerenciaServicio servicio =
            new ConsultarSugerenciaServicio(datos, afinidad, distributivos);

    @Test
    void dadasPropuestasEnElPeriodo_cuandoSeConsulta_entoncesLasHorasSonLasReales() {
        AsignaturaParalelo deLaCarrera =
                new AsignaturaParalelo(BD, "A", "TDS", new BigDecimal("4"), false, false);
        AsignaturaParalelo deOtra =
                new AsignaturaParalelo(BD, "B", "AGRO", new BigDecimal("6"), false, false);
        conLosDatos();
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(propuesta(deLaCarrera), propuesta(deOtra))));
        when(datos.ofertaDelPeriodo(PERIODO))
                .thenReturn(Optional.of(List.of(OFERTADO, deLaCarrera, deOtra)));

        SugerenciaDelParalelo delParalelo = servicio.sugerencia(PERIODO, "POO-204", "A", TODAS);

        Sugerencia sugerencia = delParalelo.sugerencia();
        assertThat(sugerencia.version()).isEqualTo(2);
        assertThat(sugerencia.minimoAfinidad()).isEqualByComparingTo("3.5");
        Candidato candidato = sugerencia.candidatos().getFirst();
        assertThat(candidato.puntaje().total()).isEqualByComparingTo("12");
        assertThat(candidato.horasDisponibles()).isEqualByComparingTo("30");
        assertThat(candidato.horas().enOtrasCarreras()).isEqualByComparingTo("6");
        assertThat(delParalelo.distributivoCreado()).isTrue();
        assertThat(delParalelo.propuesta()).isEmpty();
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSeConsulta_entoncesNoHayHorasNiPideLaOferta() {
        conLosDatos();

        SugerenciaDelParalelo delParalelo = servicio.sugerencia(PERIODO, "POO-204", "A", TODAS);

        assertThat(delParalelo.distributivoCreado()).isFalse();
        assertThat(delParalelo.sugerencia().candidatos().getFirst().horasDisponibles())
                .isEqualByComparingTo("40");
        verify(datos, never()).ofertaDelPeriodo(any());
    }

    @Test
    void dadoUnDocenteBajoElMinimo_cuandoSeConsulta_entoncesVieneAparte() {
        conLosDatos();
        when(datos.docentesActivos()).thenReturn(List.of(DOCENTE, AJENO));

        Sugerencia sugerencia = servicio.sugerencia(PERIODO, "POO-204", "A", TODAS).sugerencia();

        assertThat(sugerencia.candidatos()).hasSize(1);
        assertThat(sugerencia.bajoElMinimo())
                .extracting(Candidato::identificacion)
                .containsExactly(AJENO.identificacion());
    }

    @Test
    void dadaLaPropuestaDelParalelo_cuandoSeConsulta_entoncesLaTraeYNoCuentaEnSusHoras() {
        conLosDatos();
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(propuesta(OFERTADO))));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(OFERTADO)));

        SugerenciaDelParalelo delParalelo = servicio.sugerencia(PERIODO, "POO-204", "A", TODAS);

        PropuestaVista propuesta = delParalelo.propuesta().orElseThrow();
        assertThat(propuesta.propuesta().docente()).isEqualTo(DOCENTE.identificacion());
        assertThat(propuesta.retirada()).isFalse();
        assertThat(propuesta.horasSuficientes()).isTrue();
        assertThat(delParalelo.sugerencia().candidatos().getFirst().horasDisponibles())
                .isEqualByComparingTo("40");
    }

    @Test
    void dadaUnaPropuestaDeUnDocenteSinHoras_cuandoSeConsulta_entoncesLoAvisa() {
        DocenteActivo sinHoras =
                new DocenteActivo(
                        DOCENTE.identificacion(),
                        DOCENTE.nombre(),
                        Dedicacion.TP,
                        2,
                        DOCENTE.formacion());
        conLosDatos();
        when(datos.docentesActivos()).thenReturn(List.of(sinHoras));
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(propuesta(OFERTADO))));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(OFERTADO)));

        assertThat(
                        servicio.sugerencia(PERIODO, "POO-204", "A", TODAS)
                                .propuesta()
                                .orElseThrow()
                                .horasSuficientes())
                .isFalse();
    }

    @Test
    void dadaUnaPropuestaDeQuienYaNoEstaActivo_cuandoSeConsulta_entoncesNoAvisaDeLasHoras() {
        conLosDatos();
        when(datos.docentesActivos()).thenReturn(List.of(AJENO));
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(propuesta(OFERTADO))));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(OFERTADO)));

        assertThat(
                        servicio.sugerencia(PERIODO, "POO-204", "A", TODAS)
                                .propuesta()
                                .orElseThrow()
                                .horasSuficientes())
                .isTrue();
    }

    @Test
    void dadoUnParaleloQueNoExiste_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A", TODAS))
                .isInstanceOf(ParaleloNoEncontrado.class);
        verify(afinidad, never()).pesos(any());
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnParaleloDeOtraCarrera_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));

        assertThatThrownBy(
                        () -> servicio.sugerencia(PERIODO, "POO-204", "A", Optional.of("OTRA-01")))
                .isInstanceOf(OtraCarrera.class);
        verify(afinidad, never()).pesos(any());
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnParaleloDeSuCarrera_cuandoSeConsulta_entoncesResponde() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.docentesActivos()).thenReturn(List.of(DOCENTE));

        assertThat(
                        servicio.sugerencia(PERIODO, "POO-204", "A", Optional.of("TDS"))
                                .sugerencia()
                                .candidatos())
                .hasSize(1);
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeConsulta_entoncesNoPideNadaMas() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(RETIRADO));

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A", TODAS))
                .isInstanceOf(ParaleloRetirado.class);
        verify(afinidad, never()).pesos(any());
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinAbrir() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A", TODAS))
                .isInstanceOf(PeriodoSinAbrir.class);
        verify(datos, never()).docentesActivos();
    }

    @Test
    void dadoUnFalloEntreProcesos_cuandoSeConsulta_entoncesSePropagaSinListaParcial() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.docentesActivos()).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.sugerencia(PERIODO, "POO-204", "A", TODAS))
                .isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadasActividadesEnOtraCarrera_cuandoSeConsulta_entoncesCuentanEnSusHoras() {
        conLosDatos();
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                new VersionDistributivo(
                                        UUID.randomUUID(),
                                        1,
                                        EstadoDeVersion.EDITABLE,
                                        List.of(ParcialCarrera.nuevo("TDS")),
                                        List.of(),
                                        List.of(
                                                new ActividadNoLectiva(
                                                        "AGRO",
                                                        DOCENTE.identificacion(),
                                                        DOCENTE.nombre(),
                                                        "143-k",
                                                        "Eventos",
                                                        Componente.GESTION,
                                                        null,
                                                        null,
                                                        new BigDecimal("10"),
                                                        Instant.parse("2026-11-01T15:00:00Z"),
                                                        "coordinacion.demo")))));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(OFERTADO)));

        Candidato candidato =
                servicio.sugerencia(PERIODO, "POO-204", "A", TODAS)
                        .sugerencia()
                        .candidatos()
                        .getFirst();

        assertThat(candidato.horas().enOtrasCarreras()).isEqualByComparingTo("10");
        assertThat(candidato.horasDisponibles()).isEqualByComparingTo("30");
    }

    private void conLosDatos() {
        when(datos.paralelo(PERIODO, "POO-204", "A")).thenReturn(Optional.of(OFERTADO));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(datos.docentesActivos()).thenReturn(List.of(DOCENTE));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());
    }

    private static VersionDistributivo version(Propuesta... propuestas) {
        return new VersionDistributivo(
                UUID.randomUUID(),
                1,
                EstadoDeVersion.EDITABLE,
                List.of(ParcialCarrera.nuevo("TDS")),
                List.of(propuestas));
    }

    private static Propuesta propuesta(AsignaturaParalelo paralelo) {
        return new Propuesta(
                paralelo.asignatura().codigo(),
                paralelo.paralelo(),
                paralelo.carrera(),
                DOCENTE.identificacion(),
                DOCENTE.nombre(),
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.parse("2026-10-31T15:00:00Z"),
                "coordinacion.demo");
    }
}
