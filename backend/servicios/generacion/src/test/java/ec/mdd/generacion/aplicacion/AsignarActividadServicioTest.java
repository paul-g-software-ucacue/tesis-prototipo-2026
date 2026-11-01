package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.CargoNoVigente;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.HorasNoValidas;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.PeriodoListo;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectoNoValido;
import ec.mdd.generacion.dominio.ProyectosYCargosDelDocente;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadGuardada;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.ActividadQuitada;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDeActividad;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaActividades;
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

/** Asignar y quitar actividades (RF-26, RN-22, RN-25), con los puertos simulados con Mockito. */
class AsignarActividadServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String CARRERA = "TDS";
    private static final String DOCENTE = "0109990010";
    private static final String QUIEN = "coordinacion.demo";
    private static final String PROYECTO = "INV-2025-01";
    private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");
    private static final UUID VERSION = UUID.randomUUID();
    private static final Optional<String> TODAS = Optional.empty();
    private static final PeriodoListo ABIERTO = new PeriodoListo(true, false, true);
    private static final ProyectosYCargosDelDocente NADA =
            new ProyectosYCargosDelDocente(List.of(), List.of());

    private static final ActividadDelCatalogo PLANIFICAR =
            new ActividadDelCatalogo(
                    "140-b", "Planificar", Componente.OTRAS_DOCENCIA, List.of(), List.of());
    private static final ActividadDelCatalogo INVESTIGAR =
            new ActividadDelCatalogo(
                    "141-a",
                    "Investigar",
                    Componente.INVESTIGACION,
                    List.of("INVESTIGACION"),
                    List.of());
    private static final ActividadDelCatalogo COORDINAR =
            new ActividadDelCatalogo(
                    "143-d",
                    "Coordinar carreras",
                    Componente.GESTION,
                    List.of(),
                    List.of("COORDINADOR_CARRERA"));
    private static final ActividadDelCatalogo AUTORIDAD =
            new ActividadDelCatalogo(
                    "143-a",
                    "Rector o vicerrector",
                    Componente.GESTION,
                    List.of(),
                    List.of("RECTOR", "VICERRECTOR"));
    private static final ActividadDelCatalogo EVENTOS =
            new ActividadDelCatalogo(
                    "143-k", "Organizar eventos", Componente.GESTION, List.of(), List.of());

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
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
    private final AsignarActividadServicio servicio =
            new AsignarActividadServicio(
                    datos,
                    actividades,
                    distributivos,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadaUnaActividad_cuandoSeAsigna_entoncesGuardaConQuienYCuando() {
        conTodo(version(), NADA);

        ActividadGuardada guardada = servicio.asignar(solicitud("140-b", "4.25", null));

        ActividadNoLectiva actividad = guardada();
        assertThat(guardada.actividad()).isEqualTo(actividad);
        assertThat(actividad.carrera()).isEqualTo(CARRERA);
        assertThat(actividad.docente()).isEqualTo(DOCENTE);
        assertThat(actividad.nombreDocente()).isEqualTo("PEREZ LUIS");
        assertThat(actividad.nombreActividad()).isEqualTo("Planificar");
        assertThat(actividad.componente()).isEqualTo(Componente.OTRAS_DOCENCIA);
        assertThat(actividad.horas()).isEqualByComparingTo("4.25");
        assertThat(actividad.proyecto()).isNull();
        assertThat(actividad.asignadaEn()).isEqualTo(AHORA);
        assertThat(actividad.asignadaPor()).isEqualTo(QUIEN);
        assertThat(guardada.horasSuficientes()).isTrue();
    }

    @Test
    void dadaUnaActividad_cuandoSeAsigna_entoncesLoRegistra() {
        conTodo(version(), NADA);

        servicio.asignar(solicitud("140-b", "4", null));

        CambioDeDistributivo cambio = cambio();
        assertThat(cambio.accion()).isEqualTo(CambioDeDistributivo.Accion.ACTIVIDAD_GUARDADA);
        assertThat(cambio.usuario()).isEqualTo(QUIEN);
        assertThat(cambio.momento()).isEqualTo(AHORA);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("carrera", CARRERA)
                .containsEntry("docente", DOCENTE)
                .containsEntry("actividad", "140-b")
                .containsEntry("horas", "4")
                .doesNotContainKey("horasAnteriores");
    }

    @Test
    void dadaUnaActividadQueYaTiene_cuandoSeReemplaza_entoncesRegistraLasHorasAnteriores() {
        conTodo(version(actividad("140-b", "4", null)), NADA);

        servicio.asignar(solicitud("140-b", "6", null));

        assertThat(guardada().horas()).isEqualByComparingTo("6");
        assertThat(cambio().detalle()).containsEntry("horasAnteriores", "4");
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeAsigna_entoncesLoCreaConLaPrimeraActividad() {
        conTodo(
                new VersionDistributivo(VERSION, 1, EstadoDeVersion.EDITABLE, List.of(), List.of()),
                NADA);

        servicio.asignar(solicitud("140-b", "4", null));

        verify(distributivos).agregarParcial(VERSION, ParcialCarrera.nuevo(CARRERA));
    }

    @Test
    void dadaUnaCarreraConParcial_cuandoSeAsigna_entoncesNoLoVuelveACrear() {
        conTodo(version(), NADA);

        servicio.asignar(solicitud("140-b", "4", null));

        verify(distributivos, never()).agregarParcial(any(), any());
    }

    @Test
    void dadoUnDocenteSinHoras_cuandoSeAsigna_entoncesGuardaConElAviso() {
        conTodo(version(actividad("140-g", "38", null)), NADA);

        ActividadGuardada guardada = servicio.asignar(solicitud("140-b", "4", null));

        assertThat(guardada().horas()).isEqualByComparingTo("4");
        assertThat(guardada.horasSuficientes()).isFalse();
    }

    @Test
    void dadoQueSeReemplaza_cuandoSeCalculaElAviso_entoncesNoCuentaLasHorasAnteriores() {
        // Un docente de 40 h con 36 h, de las que 4 h son de esta actividad, admite pasarla a 8 h.
        conTodo(version(actividad("140-b", "4", null), actividad("140-g", "32", null)), NADA);

        assertThat(servicio.asignar(solicitud("140-b", "8", null)).horasSuficientes()).isTrue();
    }

    @Test
    void dadasHorasNoValidas_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "41", null)))
                .isInstanceOf(HorasNoValidas.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnParcialDeOtraCarrera_cuandoSeAsigna_entoncesNoPideNadaMas() {
        conTodo(version(), NADA);
        SolicitudDeActividad deOtra =
                new SolicitudDeActividad(
                        PERIODO,
                        CARRERA,
                        DOCENTE,
                        "140-b",
                        new BigDecimal("4"),
                        null,
                        Optional.of("AGRO"),
                        QUIEN);

        assertThatThrownBy(() -> servicio.asignar(deOtra)).isInstanceOf(OtraCarrera.class);

        verify(distributivos, never()).vigenteDe(any());
        assertNoGuardoNada();
    }

    @Test
    void dadoElParcialDeSuCarrera_cuandoSeAsigna_entoncesGuarda() {
        conTodo(version(), NADA);

        servicio.asignar(
                new SolicitudDeActividad(
                        PERIODO,
                        CARRERA,
                        DOCENTE,
                        "140-b",
                        new BigDecimal("4"),
                        null,
                        Optional.of(CARRERA),
                        QUIEN));

        assertThat(guardada().carrera()).isEqualTo(CARRERA);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(datos.periodoListo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(PeriodoNoEncontrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(datos.periodoListo(PERIODO))
                .thenReturn(Optional.of(new PeriodoListo(true, true, true)));

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(PeriodoCerrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(SinDistributivo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnParcialEnviado_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(enviado(), NADA);

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(ParcialNoEditable.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoSinVersionDeNormativa_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(actividades.catalogo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(PeriodoSinAbrir.class);

        assertNoGuardoNada();
    }

    @Test
    void dadaUnaActividadFueraDelCatalogo_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);

        assertThatThrownBy(() -> servicio.asignar(solicitud("143-i", "4", null)))
                .isInstanceOf(ActividadFueraDelCatalogo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnDocenteQueNoEstaActivo_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(datos.docentesActivos()).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(DocenteNoActivo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoQueDocentesNoLoEncuentra_cuandoSeAsigna_entoncesNoEstaActivo() {
        conTodo(version(), NADA);
        when(actividades.proyectosYCargos(DOCENTE, PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(DocenteNoActivo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnFalloEntreProcesos_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), NADA);
        when(datos.docentesActivos()).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "4", null)))
                .isInstanceOf(ProcesoConError.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnProyectoValido_cuandoSeAsigna_entoncesGuardaSuCodigoYSuNombre() {
        conTodo(version(), conProyecto());

        servicio.asignar(solicitud("141-a", "6", PROYECTO));

        assertThat(guardada().proyecto()).isEqualTo(PROYECTO);
        assertThat(guardada().nombreProyecto()).isEqualTo("Monitoreo");
        assertThat(guardada().componente()).isEqualTo(Componente.INVESTIGACION);
        assertThat(cambio().detalle()).containsEntry("proyecto", PROYECTO);
    }

    @Test
    void dadaUnaActividadQueExigeProyecto_cuandoNoLlega_entoncesNoGuarda() {
        conTodo(version(), conProyecto());

        assertThatThrownBy(() -> servicio.asignar(solicitud("141-a", "6", null)))
                .isInstanceOf(ProyectoNoValido.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnProyectoAjeno_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), conProyecto());

        assertThatThrownBy(() -> servicio.asignar(solicitud("141-a", "6", "INV-2025-99")))
                .isInstanceOf(ProyectoNoValido.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnProyectoEnUnaActividadQueNoLoPide_cuandoSeAsigna_entoncesNoGuarda() {
        conTodo(version(), conProyecto());

        assertThatThrownBy(() -> servicio.asignar(solicitud("140-b", "6", PROYECTO)))
                .isInstanceOf(ProyectoNoValido.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoElCargoVigente_cuandoSeAsignaGestion_entoncesGuarda() {
        conTodo(
                version(),
                new ProyectosYCargosDelDocente(List.of(), List.of("COORDINADOR_CARRERA")));

        servicio.asignar(solicitud("143-d", "10", null));

        assertThat(guardada().componente()).isEqualTo(Componente.GESTION);
    }

    @Test
    void dadoUnDocenteSinElCargo_cuandoSeAsignaGestion_entoncesNoGuarda() {
        conTodo(version(), NADA);

        assertThatThrownBy(() -> servicio.asignar(solicitud("143-d", "10", null)))
                .isInstanceOf(CargoNoVigente.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnVicerrector_cuandoSeAsignaLaActividadDeAutoridad_entoncesGuarda() {
        conTodo(version(), new ProyectosYCargosDelDocente(List.of(), List.of("VICERRECTOR")));

        servicio.asignar(solicitud("143-a", "32", null));

        assertThat(guardada().actividad()).isEqualTo("143-a");
    }

    @Test
    void dadaUnaActividadDeGestionSinCargo_cuandoSeAsigna_entoncesGuardaSinPedirlo() {
        conTodo(version(), NADA);

        servicio.asignar(solicitud("143-k", "2", null));

        assertThat(guardada().actividad()).isEqualTo("143-k");
    }

    @Test
    void dadaUnaActividad_cuandoSeQuita_entoncesLaBorraYLoRegistra() {
        conTodo(version(actividad("140-b", "4", null)), NADA);
        when(distributivos.quitarActividad(VERSION, CARRERA, DOCENTE, "140-b", null))
                .thenReturn(true);

        servicio.quitar(quitada("140-b", null, TODAS));

        CambioDeDistributivo cambio = cambio();
        assertThat(cambio.accion()).isEqualTo(CambioDeDistributivo.Accion.ACTIVIDAD_QUITADA);
        assertThat(cambio.usuario()).isEqualTo(QUIEN);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("carrera", CARRERA)
                .containsEntry("docente", DOCENTE)
                .containsEntry("actividad", "140-b")
                .containsEntry("horasAnteriores", "4");
    }

    @Test
    void dadoUnDocenteQueYaNoEstaActivo_cuandoSeQuita_entoncesSePuede() {
        conTodo(version(actividad("140-b", "4", null)), NADA);
        when(datos.docentesActivos()).thenReturn(List.of());
        when(distributivos.quitarActividad(VERSION, CARRERA, DOCENTE, "140-b", null))
                .thenReturn(true);

        servicio.quitar(quitada("140-b", null, TODAS));

        verify(distributivos).quitarActividad(VERSION, CARRERA, DOCENTE, "140-b", null);
    }

    @Test
    void dadaUnaActividadQueNoTiene_cuandoSeQuita_entoncesActividadNoEncontrada() {
        conTodo(version(), NADA);
        when(distributivos.quitarActividad(VERSION, CARRERA, DOCENTE, "140-b", null))
                .thenReturn(false);

        assertThatThrownBy(() -> servicio.quitar(quitada("140-b", null, TODAS)))
                .isInstanceOf(ActividadNoEncontrada.class);

        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoSeQuita_entoncesActividadNoEncontrada() {
        conTodo(version(), NADA);
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.quitar(quitada("140-b", null, TODAS)))
                .isInstanceOf(ActividadNoEncontrada.class);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeQuita_entoncesNoBorra() {
        conTodo(version(actividad("140-b", "4", null)), NADA);
        when(datos.periodoListo(PERIODO))
                .thenReturn(Optional.of(new PeriodoListo(true, true, true)));

        assertThatThrownBy(() -> servicio.quitar(quitada("140-b", null, TODAS)))
                .isInstanceOf(PeriodoCerrado.class);

        verify(distributivos, never()).quitarActividad(any(), any(), any(), any(), any());
    }

    @Test
    void dadoUnParcialDeOtraCarrera_cuandoSeQuita_entoncesNoBorra() {
        conTodo(version(actividad("140-b", "4", null)), NADA);

        assertThatThrownBy(() -> servicio.quitar(quitada("140-b", null, Optional.of("AGRO"))))
                .isInstanceOf(OtraCarrera.class);

        verify(distributivos, never()).quitarActividad(any(), any(), any(), any(), any());
    }

    @Test
    void dadoUnParcialEnviado_cuandoSeQuita_entoncesNoBorra() {
        conTodo(enviado(), NADA);

        assertThatThrownBy(() -> servicio.quitar(quitada("140-b", null, TODAS)))
                .isInstanceOf(ParcialNoEditable.class);

        verify(distributivos, never()).quitarActividad(any(), any(), any(), any(), any());
    }

    @Test
    void dadaUnaActividadConProyecto_cuandoSeQuita_entoncesLoUsaEnLaLlave() {
        conTodo(version(actividad("141-a", "6", PROYECTO)), NADA);
        when(distributivos.quitarActividad(VERSION, CARRERA, DOCENTE, "141-a", PROYECTO))
                .thenReturn(true);

        servicio.quitar(quitada("141-a", PROYECTO, TODAS));

        verify(distributivos).quitarActividad(VERSION, CARRERA, DOCENTE, "141-a", PROYECTO);
    }

    private void conTodo(VersionDistributivo version, ProyectosYCargosDelDocente delDocente) {
        when(datos.periodoListo(PERIODO)).thenReturn(Optional.of(ABIERTO));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of()));
        when(datos.docentesActivos()).thenReturn(List.of(docente()));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(version));
        when(actividades.catalogo(PERIODO))
                .thenReturn(
                        Optional.of(
                                List.of(PLANIFICAR, INVESTIGAR, COORDINAR, AUTORIDAD, EVENTOS)));
        when(actividades.proyectosYCargos(DOCENTE, PERIODO)).thenReturn(Optional.of(delDocente));
    }

    private ActividadNoLectiva guardada() {
        ArgumentCaptor<ActividadNoLectiva> actividad =
                ArgumentCaptor.forClass(ActividadNoLectiva.class);
        verify(distributivos).guardarActividad(eq(VERSION), actividad.capture());
        return actividad.getValue();
    }

    private CambioDeDistributivo cambio() {
        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }

    private void assertNoGuardoNada() {
        verify(distributivos, never()).guardarActividad(any(), any());
        verify(distributivos, never()).agregarParcial(any(), any());
        verify(registro, never()).registrar(any());
    }

    private static SolicitudDeActividad solicitud(String actividad, String horas, String proyecto) {
        return new SolicitudDeActividad(
                PERIODO,
                CARRERA,
                DOCENTE,
                actividad,
                new BigDecimal(horas),
                proyecto,
                TODAS,
                QUIEN);
    }

    private static ActividadQuitada quitada(
            String actividad, String proyecto, Optional<String> carrera) {
        return new ActividadQuitada(PERIODO, CARRERA, DOCENTE, actividad, proyecto, carrera, QUIEN);
    }

    private static ProyectosYCargosDelDocente conProyecto() {
        return new ProyectosYCargosDelDocente(
                List.of(new ProyectoDelDocente(PROYECTO, "Monitoreo", "INVESTIGACION")), List.of());
    }

    private static VersionDistributivo version(ActividadNoLectiva... actividades) {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(ParcialCarrera.nuevo(CARRERA)),
                List.of(),
                List.of(actividades));
    }

    private static VersionDistributivo enviado() {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(new ParcialCarrera(CARRERA, EstadoDelParcial.ENVIADO)),
                List.of());
    }

    private static ActividadNoLectiva actividad(String codigo, String horas, String proyecto) {
        return new ActividadNoLectiva(
                CARRERA,
                DOCENTE,
                "PEREZ LUIS",
                codigo,
                "Una actividad",
                Componente.OTRAS_DOCENCIA,
                proyecto,
                proyecto == null ? null : "Monitoreo",
                new BigDecimal(horas),
                AHORA,
                QUIEN);
    }

    private static DocenteActivo docente() {
        return new DocenteActivo(
                DOCENTE,
                "PEREZ LUIS",
                Dedicacion.TC,
                40,
                new FormacionDelDocente(
                        new TituloDeTercerNivel("Título", "06", "061"),
                        List.of(),
                        List.of(),
                        List.of()));
    }
}
