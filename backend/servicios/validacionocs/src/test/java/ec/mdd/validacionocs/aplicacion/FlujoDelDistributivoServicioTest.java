package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.AccionDelProceso;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.DevolucionDeParcial;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaConOperacionPropia;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteDevolucion;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteRevision;
import ec.mdd.validacionocs.dominio.ParcialNoEncontrado;
import ec.mdd.validacionocs.dominio.ParcialYaEnviado;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.DistributivoActual;
import ec.mdd.validacionocs.puertos.salida.Distributivo.ParcialEnviadoEnGeneracion;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El flujo del distributivo (F12-H02, US1 a US3): consultar, enviar el parcial y el avance
 * genérico, con Generación y la bitácora reemplazadas, y los procesos en memoria.
 */
class FlujoDelDistributivoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-07T15:00:00Z");
    private static final String HASH_DS = "a".repeat(64);
    private static final String HASH_TDS = "c".repeat(64);
    private static final String HASH_TODO = "b".repeat(64);
    private static final Solicitante COORDINADOR_DS =
            new Solicitante("coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.of("DS"));
    private static final Solicitante COORDINADOR_TDS =
            new Solicitante("otra.coordinacion", Set.of("COORDINADOR_CARRERA"), Optional.of("TDS"));
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty());

    private final Distributivo distributivo = mock(Distributivo.class);
    private final RepositorioConfiguracion configuraciones = mock(RepositorioConfiguracion.class);
    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ProcesosEnMemoria procesos = new ProcesosEnMemoria();
    private final List<String> eventos = new ArrayList<>();
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    eventos.add("transaccion");
                    return trabajo.get();
                }
            };
    private final FlujoDelDistributivoServicio servicio =
            new FlujoDelDistributivoServicio(
                    distributivo,
                    procesos,
                    new EtapasAlCrearse(
                            configuraciones, new LectorYaml(), new PlantillaEnRecursos()),
                    datos,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    private final Map<String, Boolean> enviadosEnGeneracion = new LinkedHashMap<>();

    @BeforeEach
    void conDosCarreras() {
        enviadosEnGeneracion.put("DS", false);
        enviadosEnGeneracion.put("TDS", false);
        when(distributivo.actual(PERIODO))
                .thenAnswer(
                        uso ->
                                Optional.of(
                                        new DistributivoActual(1, CREADO, enviadosEnGeneracion)));
        when(distributivo.marcarEnviado(PERIODO, "DS"))
                .thenAnswer(
                        uso -> {
                            eventos.add("marcar DS");
                            enviadosEnGeneracion.put("DS", true);
                            return new ParcialEnviadoEnGeneracion(1, HASH_DS);
                        });
        when(distributivo.marcarEnviado(PERIODO, "TDS"))
                .thenAnswer(
                        uso -> {
                            enviadosEnGeneracion.put("TDS", true);
                            return new ParcialEnviadoEnGeneracion(1, HASH_TDS);
                        });
        when(distributivo.hash(PERIODO)).thenReturn(HASH_TODO);
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(configuraciones.vigenteEn(PERIODO, CREADO)).thenReturn(Optional.empty());
    }

    @Test
    void dadoUnDistributivoSinProceso_cuandoSeConsulta_entoncesLoCreaEnBorradorCarrera() {
        FlujoDelDistributivo flujo = servicio.flujo(PERIODO, COORDINADOR_DS);

        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(flujo.nombreDeEtapa()).isEqualTo("Propuesta de carrera");
        assertThat(flujo.version()).isEqualTo(1);
        assertThat(flujo.parciales())
                .extracting(
                        FlujoDelDistributivo.Parcial::carrera,
                        FlujoDelDistributivo.Parcial::enviado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DS", false),
                        org.assertj.core.groups.Tuple.tuple("TDS", false));
        assertThat(flujo.acciones()).containsExactly(AccionDelProceso.ENVIAR_PARCIAL);
        assertThat(servicio.flujo(PERIODO, VICERRECTOR).acciones()).isEmpty();
    }

    @Test
    void dadaUnaCargaAnteriorALaCreacion_cuandoSeCreaElProceso_entoncesUsaEsaConfiguracion() {
        // FR-003: la que regía cuando se creó el distributivo, no la de ahora.
        String conUnidades =
                new PlantillaEnRecursos()
                        .texto()
                        .replaceFirst("(BORRADOR_UNIDADES:\\s*\\n\\s*activa:) false", "$1 true");
        when(configuraciones.vigenteEn(PERIODO, CREADO))
                .thenReturn(Optional.of(new CargaGuardada(1, conUnidades, "admin.demo", CREADO)));

        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);
        servicio.enviar(PERIODO, "TDS", COORDINADOR_TDS);

        assertThat(servicio.flujo(PERIODO, VICERRECTOR).etapa())
                .isEqualTo(TipoDeEtapa.BORRADOR_UNIDADES);
    }

    @Test
    void
            dadoElParcialDeMiCarrera_cuandoLoEnvio_entoncesGeneracionPrimeroYDespuesLaTransicionYLaBitacora() {
        FlujoDelDistributivo flujo = servicio.enviar(PERIODO, "DS", COORDINADOR_DS);

        assertThat(eventos).containsSubsequence("marcar DS", "transaccion");
        assertThat(flujo.parciales().getFirst().enviado()).isTrue();
        assertThat(flujo.parciales().getFirst().enviadoPor()).contains("coordinacion.demo");
        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        CambioDeFlujo cambio = registrado().getFirst();
        assertThat(cambio.accion()).isEqualTo(CambioDeFlujo.Accion.PARCIAL_ENVIADO);
        assertThat(cambio.usuario()).isEqualTo("coordinacion.demo");
        assertThat(cambio.momento()).isEqualTo(AHORA);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("carrera", "DS")
                .containsEntry("version", 1)
                .containsEntry("hash", HASH_DS);
        assertThat(cambio.anterior())
                .isEqualTo(Map.of("estado", "BORRADOR_CARRERA", "parcial", "PENDIENTE"));
        assertThat(cambio.nuevo())
                .isEqualTo(Map.of("estado", "BORRADOR_CARRERA", "parcial", "ENVIADO"));
    }

    @Test
    void dadoElUltimoParcial_cuandoSeEnvia_entoncesPasaDeEtapaYRegistraLasDosTransiciones() {
        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);

        FlujoDelDistributivo flujo = servicio.enviar(PERIODO, "TDS", COORDINADOR_TDS);

        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        List<CambioDeFlujo> cambios = registrado();
        assertThat(cambios)
                .extracting(CambioDeFlujo::accion)
                .containsExactly(
                        CambioDeFlujo.Accion.PARCIAL_ENVIADO,
                        CambioDeFlujo.Accion.PARCIAL_ENVIADO,
                        CambioDeFlujo.Accion.ETAPA_AVANZADA);
        CambioDeFlujo paso = cambios.get(2);
        assertThat(paso.usuario()).isEqualTo("otra.coordinacion");
        assertThat(paso.detalle()).containsEntry("hash", HASH_TODO).doesNotContainKey("carrera");
        assertThat(paso.anterior()).isEqualTo(Map.of("estado", "BORRADOR_CARRERA"));
        assertThat(paso.nuevo()).isEqualTo(Map.of("estado", "EN_CONSOLIDACION"));
    }

    @Test
    void dadoUnEnvioRechazado_cuandoSeIntenta_entoncesNoTocaGeneracionNiRegistra() {
        assertThatThrownBy(() -> servicio.enviar(PERIODO, "DS", VICERRECTOR))
                .isInstanceOf(RolDeOtraEtapa.class);
        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);
        assertThatThrownBy(() -> servicio.enviar(PERIODO, "DS", COORDINADOR_DS))
                .isInstanceOf(ParcialYaEnviado.class);

        verify(distributivo, org.mockito.Mockito.times(1)).marcarEnviado(anyString(), anyString());
        assertThat(registrado()).hasSize(1);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeEnvia_entoncesPeriodoCerradoSinLlamarAGeneracion() {
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThatThrownBy(() -> servicio.enviar(PERIODO, "DS", COORDINADOR_DS))
                .isInstanceOf(PeriodoCerrado.class);
        verify(distributivo, never()).marcarEnviado(anyString(), anyString());
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeConsulta_entoncesSinDistributivo() {
        when(distributivo.actual(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.flujo(PERIODO, COORDINADOR_DS))
                .isInstanceOf(SinDistributivo.class);
    }

    @Test
    void dadaLaTransicionQueFalloAlRegistrarse_cuandoSeReenvia_entoncesSeCompletaConElMismoHash() {
        // research §2: Generación ya lo marcó; Validación OCS no llegó a registrarlo.
        doThrow(new IllegalStateException("se cayó la base")).when(registro).registrar(any());
        assertThatThrownBy(() -> servicio.enviar(PERIODO, "DS", COORDINADOR_DS))
                .isInstanceOf(IllegalStateException.class);
        procesos.revertir();
        FlujoDelDistributivo intermedio = servicio.flujo(PERIODO, COORDINADOR_DS);
        assertThat(intermedio.parciales().getFirst().enviado()).isFalse();
        assertThat(intermedio.parciales().getFirst().pendienteDeConfirmar()).isTrue();
        assertThat(intermedio.acciones()).containsExactly(AccionDelProceso.ENVIAR_PARCIAL);
        org.mockito.Mockito.reset(registro);

        FlujoDelDistributivo flujo = servicio.enviar(PERIODO, "DS", COORDINADOR_DS);

        assertThat(flujo.parciales().getFirst().enviado()).isTrue();
        assertThat(flujo.parciales().getFirst().pendienteDeConfirmar()).isFalse();
        assertThat(registrado().getFirst().detalle()).containsEntry("hash", HASH_DS);
    }

    @Test
    void dadoElProcesoDeUnDistributivoAnterior_cuandoSeConsulta_entoncesSeRecrea() {
        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);
        Instant otro = CREADO.plusSeconds(86_400);
        when(distributivo.actual(PERIODO))
                .thenReturn(
                        Optional.of(
                                new DistributivoActual(
                                        1, otro, Map.of("DS", false, "TDS", false))));
        when(configuraciones.vigenteEn(PERIODO, otro)).thenReturn(Optional.empty());

        FlujoDelDistributivo flujo = servicio.flujo(PERIODO, COORDINADOR_DS);

        assertThat(flujo.parciales()).allMatch(una -> !una.enviado());
        assertThat(flujo.transiciones()).isEmpty();
    }

    @Test
    void dadoBorradorCarrera_cuandoSeUsaElAvanceGenerico_entoncesEtapaConOperacionPropia() {
        assertThatThrownBy(() -> servicio.avanzar(PERIODO, COORDINADOR_DS))
                .isInstanceOf(EtapaConOperacionPropia.class);
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoBorradorUnidades_cuandoElGestorAvanza_entoncesPasaYLoRegistra() {
        String conUnidades =
                new PlantillaEnRecursos()
                        .texto()
                        .replaceFirst("(BORRADOR_UNIDADES:\\s*\\n\\s*activa:) false", "$1 true");
        when(configuraciones.vigenteEn(PERIODO, CREADO))
                .thenReturn(Optional.of(new CargaGuardada(1, conUnidades, "admin.demo", CREADO)));
        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);
        servicio.enviar(PERIODO, "TDS", COORDINADOR_TDS);
        Solicitante gestor =
                new Solicitante("gestion.demo", Set.of("GESTOR_UNIDAD"), Optional.empty());

        FlujoDelDistributivo flujo = servicio.avanzar(PERIODO, gestor);

        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        CambioDeFlujo paso = registrado().getLast();
        assertThat(paso.accion()).isEqualTo(CambioDeFlujo.Accion.ETAPA_AVANZADA);
        assertThat(paso.usuario()).isEqualTo("gestion.demo");
        assertThat(paso.anterior()).isEqualTo(Map.of("estado", "BORRADOR_UNIDADES"));
    }

    // Enviar a revisión (F10-H03, research §2, contracts/bitacora.md).

    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final String HUELLA = "d".repeat(64);
    private static final CondicionesDeRevision LISTO =
            new CondicionesDeRevision(true, false, Optional.of(HUELLA), 0, 0);

    private void enConsolidacion() {
        servicio.enviar(PERIODO, "DS", COORDINADOR_DS);
        servicio.enviar(PERIODO, "TDS", COORDINADOR_TDS);
        org.mockito.Mockito.clearInvocations(registro);
        eventos.clear();
    }

    private void conCondiciones(CondicionesDeRevision condiciones) {
        when(distributivo.condicionesDeRevision(PERIODO))
                .thenAnswer(
                        uso -> {
                            eventos.add("condiciones");
                            return condiciones;
                        });
    }

    @Test
    void dadoUnConsolidadoListo_cuandoSeEnviaARevision_entoncesPasaYRegistraUnaEntrada() {
        enConsolidacion();
        conCondiciones(LISTO);

        FlujoDelDistributivo flujo = servicio.enviarARevision(PERIODO, RESPONSABLE);

        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(procesos.de(PERIODO).orElseThrow().estado())
                .isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        // Las condiciones, fuera de una transacción; el registro, en la suya (research §2).
        assertThat(eventos.subList(eventos.size() - 2, eventos.size()))
                .containsExactly("condiciones", "transaccion");
        assertThat(registrado()).hasSize(1);
        CambioDeFlujo envio = registrado().getFirst();
        assertThat(envio.accion()).isEqualTo(CambioDeFlujo.Accion.CONSOLIDADO_ENVIADO_A_REVISION);
        assertThat(envio.usuario()).isEqualTo("consolidacion.demo");
        assertThat(envio.momento()).isEqualTo(AHORA);
        assertThat(envio.detalle())
                .isEqualTo(
                        Map.of(
                                "periodo", PERIODO, "version", 1, "hash", HASH_TODO, "informe",
                                HUELLA));
        assertThat(envio.anterior()).isEqualTo(Map.of("estado", "EN_CONSOLIDACION"));
        assertThat(envio.nuevo()).isEqualTo(Map.of("estado", "EN_REVISION_VRA"));
    }

    @Test
    void dadoElDistributivoEnBorradorCarrera_cuandoSeEnviaARevision_entoncesNoPideCondiciones() {
        assertThatThrownBy(() -> servicio.enviarARevision(PERIODO, RESPONSABLE))
                .isInstanceOf(EtapaNoAdmiteRevision.class);
        verify(distributivo, never()).condicionesDeRevision(anyString());
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeEnviaARevision_entoncesPeriodoCerradoSinPedirCondiciones() {
        enConsolidacion();
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThatThrownBy(() -> servicio.enviarARevision(PERIODO, RESPONSABLE))
                .isInstanceOf(PeriodoCerrado.class);
        verify(distributivo, never()).condicionesDeRevision(anyString());
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoQueDatosNoResponde_cuandoSeEnviaARevision_entoncesPropagaSinTransicionNiEntrada() {
        enConsolidacion();
        IllegalStateException sinRespuesta = new IllegalStateException("Datos no responde");
        when(distributivo.condicionesDeRevision(PERIODO)).thenThrow(sinRespuesta);

        assertThatThrownBy(() -> servicio.enviarARevision(PERIODO, RESPONSABLE))
                .isSameAs(sinRespuesta);
        assertThat(registrado()).isEmpty();
        assertThat(procesos.de(PERIODO).orElseThrow().estado())
                .isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
    }

    @Test
    void dadaUnaConsulta_cuandoDespuesSeEnviaARevision_entoncesPideLasCondicionesAlEnviar() {
        enConsolidacion();
        conCondiciones(LISTO);
        servicio.flujo(PERIODO, RESPONSABLE);
        verify(distributivo, never()).condicionesDeRevision(anyString());

        servicio.enviarARevision(PERIODO, RESPONSABLE);

        verify(distributivo, org.mockito.Mockito.times(1)).condicionesDeRevision(PERIODO);
    }

    // Devolver (F12-H05, research §2, contracts/bitacora.md).

    private static final List<DevolucionDeParcial> DEVOLVER_DS_Y_TDS =
            List.of(
                    DevolucionDeParcial.de("DS", "Faltan docentes en primer nivel."),
                    DevolucionDeParcial.de("TDS", "Un docente sin clase en segundo nivel."));

    private void marcarDevueltosEnGeneracion() {
        when(distributivo.marcarDevueltos(anyString(), anyList()))
                .thenAnswer(
                        uso -> {
                            eventos.add("marcar devueltos");
                            List<String> carreras = uso.getArgument(1);
                            carreras.forEach(carrera -> enviadosEnGeneracion.put(carrera, false));
                            return 1;
                        });
    }

    @Test
    void dadoElDistributivoEnConsolidacion_cuandoDevuelvo_entoncesVuelveABorradorConUnaEntrada() {
        enConsolidacion();
        marcarDevueltosEnGeneracion();

        FlujoDelDistributivo flujo = servicio.devolver(PERIODO, DEVOLVER_DS_Y_TDS, RESPONSABLE);

        assertThat(flujo.etapa()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        // Primero Generación, después la transacción de Aprobación (research §2).
        assertThat(eventos.subList(eventos.size() - 2, eventos.size()))
                .containsExactly("marcar devueltos", "transaccion");
        verify(distributivo).marcarDevueltos(PERIODO, List.of("DS", "TDS"));
        assertThat(registrado()).hasSize(1);
        CambioDeFlujo devolucion = registrado().getFirst();
        assertThat(devolucion.accion()).isEqualTo(CambioDeFlujo.Accion.DISTRIBUTIVO_DEVUELTO);
        assertThat(devolucion.usuario()).isEqualTo("consolidacion.demo");
        assertThat(devolucion.detalle())
                .isEqualTo(
                        Map.of(
                                "periodo",
                                PERIODO,
                                "version",
                                1,
                                "hash",
                                HASH_TODO,
                                "devueltos",
                                Map.of(
                                        "DS", "Faltan docentes en primer nivel.",
                                        "TDS", "Un docente sin clase en segundo nivel.")));
        assertThat(devolucion.anterior()).isEqualTo(Map.of("estado", "EN_CONSOLIDACION"));
        assertThat(devolucion.nuevo()).isEqualTo(Map.of("estado", "BORRADOR_CARRERA"));
    }

    @Test
    void dadaUnaDevolucion_cuandoSeConsulta_entoncesCadaParcialDevueltoDiceQuienCuandoYPorQue() {
        enConsolidacion();
        marcarDevueltosEnGeneracion();
        servicio.devolver(PERIODO, DEVOLVER_DS_Y_TDS.subList(0, 1), RESPONSABLE);

        FlujoDelDistributivo flujo = servicio.flujo(PERIODO, COORDINADOR_DS);

        FlujoDelDistributivo.Parcial ds = parcial(flujo, "DS");
        assertThat(ds.enviado()).isFalse();
        assertThat(ds.pendienteDeConfirmar()).isFalse();
        assertThat(ds.devolucion())
                .contains(
                        new FlujoDelDistributivo.Devolucion(
                                "consolidacion.demo", AHORA, "Faltan docentes en primer nivel."));
        assertThat(parcial(flujo, "TDS").devolucion()).isEmpty();
        assertThat(parcial(flujo, "TDS").enviado()).isTrue();
    }

    @Test
    void dadaUnaDevolucionQueNoCabe_cuandoSeIntenta_entoncesNoTocaGeneracionNiRegistra() {
        assertThatThrownBy(() -> servicio.devolver(PERIODO, DEVOLVER_DS_Y_TDS, RESPONSABLE))
                .isInstanceOf(EtapaNoAdmiteDevolucion.class);
        verify(distributivo, never()).marcarDevueltos(anyString(), anyList());
        assertThat(registrado()).isEmpty();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoDevuelvo_entoncesPeriodoCerradoSinTocarGeneracion() {
        enConsolidacion();
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThatThrownBy(() -> servicio.devolver(PERIODO, DEVOLVER_DS_Y_TDS, RESPONSABLE))
                .isInstanceOf(PeriodoCerrado.class);
        verify(distributivo, never()).marcarDevueltos(anyString(), anyList());
    }

    @Test
    void dadoQueGeneracionNoEncuentraUnParcial_cuandoDevuelvo_entoncesNoRegistraNada() {
        enConsolidacion();
        when(distributivo.marcarDevueltos(anyString(), anyList()))
                .thenThrow(new ParcialNoEncontrado());

        assertThatThrownBy(() -> servicio.devolver(PERIODO, DEVOLVER_DS_Y_TDS, RESPONSABLE))
                .isInstanceOf(ParcialNoEncontrado.class);
        assertThat(registrado()).isEmpty();
        assertThat(procesos.de(PERIODO).orElseThrow().estado())
                .isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
    }

    private static FlujoDelDistributivo.Parcial parcial(
            FlujoDelDistributivo flujo, String carrera) {
        return flujo.parciales().stream()
                .filter(uno -> uno.carrera().equals(carrera))
                .findFirst()
                .orElseThrow();
    }

    private List<CambioDeFlujo> registrado() {
        ArgumentCaptor<CambioDeFlujo> cambios = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro, org.mockito.Mockito.atLeast(0)).registrar(cambios.capture());
        return cambios.getAllValues();
    }

    /**
     * Los procesos en memoria. {@link #revertir()} deshace lo guardado en la última transacción.
     */
    private static final class ProcesosEnMemoria implements RepositorioProceso {

        private final Map<String, ProcesoAprobacion> guardados = new HashMap<>();
        private final Map<String, ProcesoAprobacion> antes = new HashMap<>();

        @Override
        public Optional<ProcesoAprobacion> de(String periodo) {
            return Optional.ofNullable(guardados.get(periodo)).map(ProcesosEnMemoria::copia);
        }

        @Override
        public Optional<ProcesoAprobacion> bloquear(String periodo) {
            return de(periodo);
        }

        @Override
        public void crear(ProcesoAprobacion proceso, Instant ahora) {
            guardados.put(proceso.periodo(), copia(proceso));
        }

        @Override
        public void guardar(ProcesoAprobacion proceso, List<Transicion> nuevas) {
            antes.put(proceso.periodo(), guardados.get(proceso.periodo()));
            guardados.put(proceso.periodo(), copia(proceso));
        }

        void revertir() {
            guardados.putAll(antes);
        }

        private static ProcesoAprobacion copia(ProcesoAprobacion proceso) {
            return ProcesoAprobacion.de(
                    proceso.periodo(),
                    proceso.etapas(),
                    proceso.estado(),
                    proceso.distributivoCreadoEn(),
                    proceso.parciales(),
                    proceso.transiciones());
        }
    }
}
