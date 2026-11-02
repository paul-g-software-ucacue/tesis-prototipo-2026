package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.PesosAfinidad;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.ReglasDelPeriodo;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.HorasExcedidas;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDePropuesta;
import ec.mdd.generacion.puertos.entrada.TopeDeClase;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import ec.mdd.plataforma.eventos.ProcesoConError;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Proponer y quitar docentes (RF-24, RN-19, D-19), con los puertos simulados con Mockito. */
class ProponerDocenteServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String ASIGNATURA = "POO-204";
    private static final String QUIEN = "coordinacion.demo";
    private static final Instant AHORA = Instant.parse("2026-10-31T15:00:00Z");
    private static final UUID VERSION = UUID.randomUUID();
    private static final Optional<String> TODAS = Optional.empty();
    private static final String TEXTO = "Dictó esta materia durante tres años";

    private static final AsignaturaOfertada OFERTADA =
            new AsignaturaOfertada(ASIGNATURA, "Programación", "06", "061");

    private static final AsignaturaParalelo PARALELO = paralelo(false, false);

    // Puntaje 12: tercer nivel y cuarto nivel en el campo específico.
    private static final DocenteActivo AFIN = docente("0109990010", 40, "061");
    // Puntaje 0: nada coincide.
    private static final DocenteActivo AJENO = docente("0109990020", 40, "091");

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
    private final DatosParaActividades actividades = mock(DatosParaActividades.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final ProponerDocenteServicio servicio =
            new ProponerDocenteServicio(
                    datos,
                    afinidad,
                    actividades,
                    distributivos,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnDocenteAfin_cuandoSePropone_entoncesGuardaConQuienYCuando() {
        conTodo(version());

        PropuestaVista vista = servicio.proponer(solicitud(AFIN, null));

        Propuesta guardada = guardada();
        assertThat(vista.propuesta()).isEqualTo(guardada);
        assertThat(guardada.docente()).isEqualTo(AFIN.identificacion());
        assertThat(guardada.nombreDocente()).isEqualTo(AFIN.nombre());
        assertThat(guardada.carrera()).isEqualTo("TDS");
        assertThat(guardada.propuestaEn()).isEqualTo(AHORA);
        assertThat(guardada.propuestaPor()).isEqualTo(QUIEN);
        assertThat(vista.retirada()).isFalse();
        assertThat(vista.horasSuficientes()).isTrue();
    }

    @Test
    void dadaUnaPropuestaNueva_cuandoSeGuarda_entoncesElRegistroNoLlevaElTexto() {
        conTodo(version());

        servicio.proponer(solicitud(AJENO, TEXTO));

        CambioDeDistributivo cambio = cambio();
        assertThat(cambio.accion()).isEqualTo(CambioDeDistributivo.Accion.PROPUESTA_GUARDADA);
        assertThat(cambio.usuario()).isEqualTo(QUIEN);
        assertThat(cambio.momento()).isEqualTo(AHORA);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("asignatura", ASIGNATURA)
                .containsEntry("paralelo", "A")
                .containsEntry("docente", AJENO.identificacion())
                .containsEntry("justificacion", "EXCEPCION")
                .doesNotContainKey("docenteAnterior");
        assertThat(cambio.detalle().toString()).doesNotContain(TEXTO);
    }

    @Test
    void dadaUnaPropuestaAnterior_cuandoSeReemplaza_entoncesRegistraAlDocenteAnterior() {
        conTodo(version(propuestaDe(AJENO.identificacion())));

        servicio.proponer(solicitud(AFIN, null));

        assertThat(guardada().docente()).isEqualTo(AFIN.identificacion());
        assertThat(cambio().detalle()).containsEntry("docenteAnterior", AJENO.identificacion());
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSePropone_entoncesLoCreaConLaPrimeraPropuesta() {
        conTodo(
                new VersionDistributivo(
                        VERSION, 1, EstadoDeVersion.EDITABLE, List.of(), List.of()));

        servicio.proponer(solicitud(AFIN, null));

        verify(distributivos).agregarParcial(VERSION, ParcialCarrera.nuevo("TDS"));
    }

    @Test
    void dadaUnaCarreraConParcial_cuandoSePropone_entoncesNoLoVuelveACrear() {
        conTodo(version());

        servicio.proponer(solicitud(AFIN, null));

        verify(distributivos, never()).agregarParcial(any(), any());
    }

    @Test
    void dadoUnDocenteSinHorasSuficientes_cuandoSePropone_entoncesNoGuardaYDiceLasQueQuedan() {
        DocenteActivo sinHoras = docente("0109990030", 2, "061");
        conTodo(version());
        when(datos.docentesActivos()).thenReturn(List.of(sinHoras));

        assertThatThrownBy(() -> servicio.proponer(solicitud(sinHoras, null)))
                .isInstanceOfSatisfying(
                        HorasExcedidas.class,
                        error -> assertThat(error.horasQueQuedan()).isEqualByComparingTo("2"));
        assertNoGuardoNada();
    }

    @Test
    void dadasHorasEnOtraCarrera_cuandoSePropone_entoncesCuentanYNoGuarda() {
        AsignaturaParalelo otro = enOtraCarrera("37.5");
        conTodo(version(propuestaEn(otro)));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(HorasExcedidas.class);
        assertNoGuardoNada();
    }

    @Test
    void justoEnElLimite_cuandoSePropone_entoncesGuarda() {
        AsignaturaParalelo otro = enOtraCarrera("36");
        conTodo(version(propuestaEn(otro)));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));

        servicio.proponer(solicitud(AFIN, null));

        assertThat(guardada().docente()).isEqualTo(AFIN.identificacion());
    }

    @Test
    void enUnaCarreraSinParcial_cuandoSeRechaza_entoncesNoCreaElParcial() {
        DocenteActivo sinHoras = docente("0109990030", 2, "061");
        conTodo(
                new VersionDistributivo(
                        VERSION, 1, EstadoDeVersion.EDITABLE, List.of(), List.of()));
        when(datos.docentesActivos()).thenReturn(List.of(sinHoras));

        assertThatThrownBy(() -> servicio.proponer(solicitud(sinHoras, null)))
                .isInstanceOf(HorasExcedidas.class);
        assertNoGuardoNada();
    }

    @Test
    void sinLasReglasDelPeriodo_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(actividades.reglas(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(PeriodoSinAbrir.class);
        assertNoGuardoNada();
    }

    @Test
    void sinLosProyectosDelDocente_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(actividades.proyectosYCargos(AFIN.identificacion(), PERIODO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(DocenteNoActivo.class);
        assertNoGuardoNada();
    }

    @Test
    void dadoUnDocenteQueAlcanzaElMinimo_cuandoSePropone_entoncesEsAfinidadYSinTexto() {
        conTodo(version());

        servicio.proponer(solicitud(AFIN, TEXTO));

        assertThat(guardada().justificacion())
                .isEqualTo(new Justificacion(TipoDeJustificacion.AFINIDAD, null));
    }

    @Test
    void dadoTroncoComunBajoElMinimo_cuandoSePropone_entoncesEsDisponibilidadYSinTexto() {
        conTodo(version());
        when(datos.paralelo(PERIODO, ASIGNATURA, "A")).thenReturn(Optional.of(deTroncoComun()));

        servicio.proponer(solicitud(AJENO, TEXTO));

        assertThat(guardada().justificacion())
                .isEqualTo(new Justificacion(TipoDeJustificacion.DISPONIBILIDAD, null));
    }

    @Test
    void dadoUnDocenteBajoElMinimoConTexto_cuandoSePropone_entoncesEsExcepcionConSuTexto() {
        conTodo(version());

        servicio.proponer(solicitud(AJENO, "  " + TEXTO + "  "));

        assertThat(guardada().justificacion())
                .isEqualTo(new Justificacion(TipoDeJustificacion.EXCEPCION, TEXTO));
    }

    @Test
    void dadoUnDocenteBajoElMinimoSinTexto_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AJENO, null)))
                .isInstanceOf(JustificacionObligatoria.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnParaleloQueNoExiste_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(datos.paralelo(PERIODO, ASIGNATURA, "A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(ParaleloNoEncontrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnParaleloDeOtraCarrera_cuandoSePropone_entoncesNoPideNadaMas() {
        conTodo(version());
        SolicitudDePropuesta deOtra =
                new SolicitudDePropuesta(
                        PERIODO,
                        ASIGNATURA,
                        "A",
                        AFIN.identificacion(),
                        null,
                        Optional.of("AGRO"),
                        QUIEN);

        assertThatThrownBy(() -> servicio.proponer(deOtra)).isInstanceOf(OtraCarrera.class);

        verify(distributivos, never()).vigenteDe(any());
        assertNoGuardoNada();
    }

    @Test
    void dadoUnParaleloDeSuCarrera_cuandoSePropone_entoncesGuarda() {
        conTodo(version());

        servicio.proponer(
                new SolicitudDePropuesta(
                        PERIODO,
                        ASIGNATURA,
                        "A",
                        AFIN.identificacion(),
                        null,
                        Optional.of("TDS"),
                        QUIEN));

        assertThat(guardada().carrera()).isEqualTo("TDS");
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(datos.paralelo(PERIODO, ASIGNATURA, "A"))
                .thenReturn(Optional.of(paralelo(true, false)));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(ParaleloRetirado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(datos.paralelo(PERIODO, ASIGNATURA, "A"))
                .thenReturn(Optional.of(paralelo(false, true)));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(PeriodoCerrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(SinDistributivo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnParcialEnviado_cuandoSePropone_entoncesNoGuarda() {
        conTodo(enviado());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(ParcialNoEditable.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoSinVersionDeNormativa_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(PeriodoSinAbrir.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnDocenteQueNoEstaActivo_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(datos.docentesActivos()).thenReturn(List.of(AJENO));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(DocenteNoActivo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnFalloEntreProcesos_cuandoSePropone_entoncesNoGuarda() {
        conTodo(version());
        when(datos.docentesActivos()).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(ProcesoConError.class);

        assertNoGuardoNada();
    }

    @Test
    void dadaUnaPropuesta_cuandoSeQuita_entoncesLaBorraYLoRegistra() {
        conTodo(version(propuestaDe(AFIN.identificacion())));
        when(distributivos.quitarPropuesta(VERSION, ASIGNATURA, "A")).thenReturn(true);

        servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN);

        CambioDeDistributivo cambio = cambio();
        assertThat(cambio.accion()).isEqualTo(CambioDeDistributivo.Accion.PROPUESTA_QUITADA);
        assertThat(cambio.usuario()).isEqualTo(QUIEN);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("asignatura", ASIGNATURA)
                .containsEntry("paralelo", "A");
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeQuita_entoncesTambienSePuede() {
        conTodo(version(propuestaDe(AFIN.identificacion())));
        when(datos.paralelo(PERIODO, ASIGNATURA, "A"))
                .thenReturn(Optional.of(paralelo(true, false)));
        when(distributivos.quitarPropuesta(VERSION, ASIGNATURA, "A")).thenReturn(true);

        servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN);

        verify(distributivos).quitarPropuesta(VERSION, ASIGNATURA, "A");
    }

    @Test
    void dadoQueNoTienePropuesta_cuandoSeQuita_entoncesPropuestaNoEncontrada() {
        conTodo(version());
        when(distributivos.quitarPropuesta(VERSION, ASIGNATURA, "A")).thenReturn(false);

        assertThatThrownBy(() -> servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN))
                .isInstanceOf(PropuestaNoEncontrada.class);

        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSeQuita_entoncesPropuestaNoEncontrada() {
        conTodo(version());
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN))
                .isInstanceOf(PropuestaNoEncontrada.class);
    }

    @Test
    void dadoUnParaleloDeOtraCarrera_cuandoSeQuita_entoncesNoBorraNada() {
        conTodo(version(propuestaDe(AFIN.identificacion())));

        assertThatThrownBy(
                        () -> servicio.quitar(PERIODO, ASIGNATURA, "A", Optional.of("AGRO"), QUIEN))
                .isInstanceOf(OtraCarrera.class);

        verify(distributivos, never()).quitarPropuesta(any(), any(), any());
    }

    @Test
    void dadoUnParcialEnviado_cuandoSeQuita_entoncesNoBorraNada() {
        conTodo(enviado());

        assertThatThrownBy(() -> servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN))
                .isInstanceOf(ParcialNoEditable.class);

        verify(distributivos, never()).quitarPropuesta(any(), any(), any());
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeQuita_entoncesNoBorraNada() {
        conTodo(version(propuestaDe(AFIN.identificacion())));
        when(datos.paralelo(PERIODO, ASIGNATURA, "A"))
                .thenReturn(Optional.of(paralelo(false, true)));

        assertThatThrownBy(() -> servicio.quitar(PERIODO, ASIGNATURA, "A", TODAS, QUIEN))
                .isInstanceOf(PeriodoCerrado.class);

        verify(distributivos, never()).quitarPropuesta(any(), any(), any());
    }

    @Test
    void dadasActividadesDelDocente_cuandoSePropone_entoncesCuentanYNoGuarda() {
        conTodo(conActividades(gestion("37.5")));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(HorasExcedidas.class);
        assertNoGuardoNada();
    }

    @Test
    void conUnProyectoDeInvestigacionYElTope_cuandoSePropone_entoncesNoGuarda() {
        AsignaturaParalelo otro = enOtraCarrera("14");
        conTodo(version(propuestaEn(otro)));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));
        conProyecto("INVESTIGACION");

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOfSatisfying(
                        TopeDeClase.class,
                        error -> assertThat(error.horasQueQuedan()).isEqualByComparingTo("2"));
        assertNoGuardoNada();
    }

    @Test
    void conUnProyectoDeVinculacionYElTope_cuandoSePropone_entoncesNoGuarda() {
        AsignaturaParalelo otro = enOtraCarrera("14");
        conTodo(version(propuestaEn(otro)));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));
        conProyecto("VINCULACION");

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(TopeDeClase.class);
    }

    @Test
    void conUnProyectoDePracticas_cuandoPasaDeDieciseisDeClase_entoncesGuarda() {
        AsignaturaParalelo otro = enOtraCarrera("14");
        conTodo(version(propuestaEn(otro)));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));
        conProyecto("PRACTICAS_PREPROFESIONALES");

        servicio.proponer(solicitud(AFIN, null));

        assertThat(guardada().docente()).isEqualTo(AFIN.identificacion());
    }

    @Test
    void conOtraCifraEnLaNormativa_cuandoSePropone_entoncesLaUsa() {
        conTodo(version());
        conProyecto("INVESTIGACION");
        when(actividades.reglas(PERIODO))
                .thenReturn(
                        Optional.of(
                                new ReglasDelPeriodo(new BigDecimal("40"), new BigDecimal("3"))));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(TopeDeClase.class);
    }

    @Test
    void unDocenteExcedido_cuandoSeVuelveAProponerIgual_entoncesGuarda() {
        conTodo(
                new VersionDistributivo(
                        VERSION,
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(ParcialCarrera.nuevo("TDS")),
                        List.of(propuestaDe(AFIN.identificacion())),
                        List.of(gestion("38"))));

        PropuestaVista vista = servicio.proponer(solicitud(AFIN, null));

        assertThat(guardada().docente()).isEqualTo(AFIN.identificacion());
        assertThat(vista.horasSuficientes()).isFalse();
    }

    @Test
    void unDocenteExcedido_cuandoSeLeAgregaOtraClase_entoncesNoGuarda() {
        conTodo(conActividades(gestion("30"), gestion("11")));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(HorasExcedidas.class);
        assertNoGuardoNada();
    }

    @Test
    void cuandoSeGuarda_entoncesEvaluaConLaVersionReleidaDentroDeLaTransaccion() {
        conTodo(version());
        AsignaturaParalelo otro = enOtraCarrera("37.5");
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO, otro)));
        when(distributivos.bloquearVigente(PERIODO))
                .thenReturn(Optional.of(version(propuestaEn(otro))));

        assertThatThrownBy(() -> servicio.proponer(solicitud(AFIN, null)))
                .isInstanceOf(HorasExcedidas.class);
        assertNoGuardoNada();
    }

    private void conProyecto(String tipo) {
        when(actividades.proyectosYCargos(AFIN.identificacion(), PERIODO))
                .thenReturn(
                        Optional.of(
                                new ProyectosYCargosDelDocente(
                                        List.of(new ProyectoDelDocente("P-1", "Un proyecto", tipo)),
                                        List.of())));
    }

    private static AsignaturaParalelo enOtraCarrera(String horas) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada("BD-301", "Bases de datos", "06", "061"),
                "A",
                "AGRO",
                new BigDecimal(horas),
                false,
                false);
    }

    private static Propuesta propuestaEn(AsignaturaParalelo paralelo) {
        return new Propuesta(
                paralelo.asignatura().codigo(),
                paralelo.paralelo(),
                paralelo.carrera(),
                AFIN.identificacion(),
                AFIN.nombre(),
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                AHORA,
                QUIEN);
    }

    private static ActividadNoLectiva gestion(String horas) {
        return new ActividadNoLectiva(
                "AGRO",
                AFIN.identificacion(),
                AFIN.nombre(),
                "143-k",
                "Eventos",
                Componente.GESTION,
                null,
                null,
                new BigDecimal(horas),
                AHORA,
                QUIEN);
    }

    private static VersionDistributivo conActividades(ActividadNoLectiva... actividades) {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(ParcialCarrera.nuevo("TDS")),
                List.of(),
                List.of(actividades));
    }

    private void conTodo(VersionDistributivo version) {
        when(datos.paralelo(PERIODO, ASIGNATURA, "A")).thenReturn(Optional.of(PARALELO));
        when(datos.docentesActivos()).thenReturn(List.of(AFIN, AJENO));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(PARALELO)));
        when(afinidad.pesos(PERIODO)).thenReturn(Optional.of(PESOS));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(version));
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(version));
        when(actividades.reglas(PERIODO))
                .thenReturn(
                        Optional.of(
                                new ReglasDelPeriodo(new BigDecimal("40"), new BigDecimal("16"))));
        when(actividades.proyectosYCargos(any(), eq(PERIODO)))
                .thenReturn(Optional.of(new ProyectosYCargosDelDocente(List.of(), List.of())));
    }

    private Propuesta guardada() {
        ArgumentCaptor<Propuesta> propuesta = ArgumentCaptor.forClass(Propuesta.class);
        verify(distributivos).guardarPropuesta(eq(VERSION), propuesta.capture());
        return propuesta.getValue();
    }

    private CambioDeDistributivo cambio() {
        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }

    private void assertNoGuardoNada() {
        verify(distributivos, never()).guardarPropuesta(any(), any());
        verify(distributivos, never()).agregarParcial(any(), any());
        verify(registro, never()).registrar(any());
    }

    private static SolicitudDePropuesta solicitud(DocenteActivo docente, String texto) {
        return new SolicitudDePropuesta(
                PERIODO, ASIGNATURA, "A", docente.identificacion(), texto, TODAS, QUIEN);
    }

    private static VersionDistributivo version(Propuesta... propuestas) {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(ParcialCarrera.nuevo("TDS")),
                List.of(propuestas));
    }

    private static VersionDistributivo enviado() {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO)),
                List.of());
    }

    private static Propuesta propuestaDe(String docente) {
        return new Propuesta(
                ASIGNATURA,
                "A",
                "TDS",
                docente,
                "ANTERIOR",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                AHORA,
                QUIEN);
    }

    private static AsignaturaParalelo paralelo(boolean retirada, boolean periodoCerrado) {
        return new AsignaturaParalelo(
                OFERTADA, "A", "TDS", new BigDecimal("4"), false, retirada, periodoCerrado);
    }

    private static AsignaturaParalelo deTroncoComun() {
        return new AsignaturaParalelo(OFERTADA, "A", "TDS", new BigDecimal("4"), true, false);
    }

    private static DocenteActivo docente(String id, int horas, String campo) {
        return new DocenteActivo(
                id,
                "DOCENTE " + id,
                Dedicacion.TC,
                horas,
                new FormacionDelDocente(
                        new TituloDeTercerNivel("Título", campo.substring(0, 2), campo),
                        List.of(new ElementoDeFormacion("Maestría", campo)),
                        List.of(),
                        List.of()));
    }
}
