package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Las reglas del proceso de aprobación (F12-H02, data-model.md, RF-34). */
class ProcesoAprobacionTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-07T15:00:00Z");
    private static final String HASH_PARCIAL = "a".repeat(64);
    private static final String HASH_DISTRIBUTIVO = "b".repeat(64);
    private static final Solicitante COORDINADOR_DS =
            new Solicitante("coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.of("DS"));
    private static final Solicitante COORDINADOR_TDS =
            new Solicitante("otra.coordinacion", Set.of("COORDINADOR_CARRERA"), Optional.of("TDS"));
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty());
    private static final Solicitante GESTOR =
            new Solicitante("gestion.demo", Set.of("GESTOR_UNIDAD"), Optional.empty());

    @Test
    void
            dadoUnDistributivoNuevo_cuandoSeCreaSuProceso_entoncesEstaEnBorradorCarreraConTodoPendiente() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(proceso.parciales())
                .isEqualTo(Map.of("DS", EstadoDeEnvio.PENDIENTE, "TDS", EstadoDeEnvio.PENDIENTE));
        assertThat(proceso.transiciones()).isEmpty();
        assertThat(proceso.distributivoCreadoEn()).isEqualTo(CREADO);
    }

    @Test
    void dadaBorradorUnidadesDesactivada_cuandoSeBuscaLaSiguiente_entoncesEsEnConsolidacion() {
        assertThat(nuevo(false, "DS").siguienteActiva(TipoDeEtapa.BORRADOR_CARRERA))
                .isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevo(true, "DS").siguienteActiva(TipoDeEtapa.BORRADOR_CARRERA))
                .isEqualTo(TipoDeEtapa.BORRADOR_UNIDADES);
    }

    @Test
    void dadoElParcialDeMiCarrera_cuandoLoEnvio_entoncesQuedaEnviadoConSuTransicion() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");

        List<Transicion> nuevas = enviar(proceso, COORDINADOR_DS, "DS");

        assertThat(proceso.parciales()).containsEntry("DS", EstadoDeEnvio.ENVIADO);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.ENVIO_PARCIAL,
                                TipoDeEtapa.BORRADOR_CARRERA,
                                TipoDeEtapa.BORRADOR_CARRERA,
                                Optional.of("DS"),
                                1,
                                HASH_PARCIAL,
                                "coordinacion.demo",
                                AHORA));
        assertThat(proceso.transiciones()).isEqualTo(nuevas);
    }

    @Test
    void dadoElUltimoParcial_cuandoSeEnvia_entoncesTambienPasaALaSiguienteEtapaActiva() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");
        enviar(proceso, COORDINADOR_DS, "DS");

        List<Transicion> nuevas = enviar(proceso, COORDINADOR_TDS, "TDS");

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevas).hasSize(2);
        assertThat(nuevas.get(1))
                .isEqualTo(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.BORRADOR_CARRERA,
                                TipoDeEtapa.EN_CONSOLIDACION,
                                Optional.empty(),
                                1,
                                HASH_DISTRIBUTIVO,
                                "otra.coordinacion",
                                AHORA));
        assertThat(proceso.transiciones()).hasSize(3);
    }

    @Test
    void dadaBorradorUnidadesActiva_cuandoSeEnviaElUltimo_entoncesPasaABorradorUnidades() {
        ProcesoAprobacion proceso = nuevo(true, "DS");

        enviar(proceso, COORDINADOR_DS, "DS");

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_UNIDADES);
    }

    @Test
    void dadoOtroRol_cuandoEnvia_entoncesRolDeOtraEtapaSinTransicion() {
        ProcesoAprobacion proceso = nuevo(false, "DS");

        assertThatThrownBy(() -> enviar(proceso, VICERRECTOR, "DS"))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.transiciones()).isEmpty();
        assertThat(proceso.parciales()).containsEntry("DS", EstadoDeEnvio.PENDIENTE);
    }

    @Test
    void dadoElParcialDeOtraCarrera_cuandoLoEnvio_entoncesParcialDeOtraCarrera() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");

        assertThatThrownBy(() -> enviar(proceso, COORDINADOR_DS, "TDS"))
                .isInstanceOf(ParcialDeOtraCarrera.class);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoUnCoordinadorSinAlcancePropio_cuandoEnvia_entoncesPuedeEnviarCualquiera() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");
        Solicitante sinAlcance =
                new Solicitante(
                        "coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.empty());

        enviar(proceso, sinAlcance, "TDS");

        assertThat(proceso.parciales()).containsEntry("TDS", EstadoDeEnvio.ENVIADO);
    }

    @Test
    void dadoUnParcialYaEnviado_cuandoSeEnviaOtraVez_entoncesParcialYaEnviadoSinOtraTransicion() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");
        enviar(proceso, COORDINADOR_DS, "DS");

        assertThatThrownBy(() -> enviar(proceso, COORDINADOR_DS, "DS"))
                .isInstanceOf(ParcialYaEnviado.class);
        assertThat(proceso.transiciones()).hasSize(1);
    }

    @Test
    void dadoElDistributivoFueraDeBorradorCarrera_cuandoSeEnvia_entoncesEtapaNoAdmiteEnvio() {
        ProcesoAprobacion conOtraCarrera =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas(false),
                        TipoDeEtapa.EN_CONSOLIDACION,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO, "TDS", EstadoDeEnvio.PENDIENTE),
                        List.of());

        assertThatThrownBy(() -> enviar(conOtraCarrera, COORDINADOR_TDS, "TDS"))
                .isInstanceOf(EtapaNoAdmiteEnvio.class);
    }

    @Test
    void dadoUnEnvioQueNoCabe_cuandoSeValida_entoncesFallaComoElEnvioSinCambiarNada() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");

        assertThatThrownBy(() -> proceso.validarEnvio("DS", VICERRECTOR))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThatThrownBy(() -> proceso.validarEnvio("TDS", COORDINADOR_DS))
                .isInstanceOf(ParcialDeOtraCarrera.class);
        proceso.validarEnvio("DS", COORDINADOR_DS);
        assertThat(proceso.parciales()).containsEntry("DS", EstadoDeEnvio.PENDIENTE);
        assertThat(proceso.transiciones()).isEmpty();
        enviar(proceso, COORDINADOR_DS, "DS");
        assertThatThrownBy(() -> proceso.validarEnvio("DS", COORDINADOR_DS))
                .isInstanceOf(ParcialYaEnviado.class);
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeEnvia_entoncesParcialNoEncontrado() {
        Solicitante sinAlcance =
                new Solicitante(
                        "coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.empty());

        assertThatThrownBy(() -> enviar(nuevo(false, "DS"), sinAlcance, "AGRO"))
                .isInstanceOf(ParcialNoEncontrado.class);
    }

    @Test
    void dadaUnaCarreraNueva_cuandoSeSumanLosParcialesDeGeneracion_entoncesQuedaPendiente() {
        ProcesoAprobacion proceso = nuevo(false, "DS");

        proceso.sumarParciales(List.of("DS", "TDS"));

        assertThat(proceso.parciales()).containsEntry("TDS", EstadoDeEnvio.PENDIENTE);
    }

    @Test
    void dadoBorradorUnidades_cuandoElGestorAvanza_entoncesPasaALaSiguienteActiva() {
        ProcesoAprobacion proceso = enBorradorUnidades();

        List<Transicion> nuevas = proceso.avanzar(GESTOR, 1, HASH_DISTRIBUTIVO, AHORA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.BORRADOR_UNIDADES,
                                TipoDeEtapa.EN_CONSOLIDACION,
                                Optional.empty(),
                                1,
                                HASH_DISTRIBUTIVO,
                                "gestion.demo",
                                AHORA));
    }

    @Test
    void dadoBorradorUnidades_cuandoOtroRolAvanza_entoncesRolDeOtraEtapa() {
        ProcesoAprobacion proceso = enBorradorUnidades();

        assertThatThrownBy(() -> proceso.avanzar(VICERRECTOR, 1, HASH_DISTRIBUTIVO, AHORA))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_UNIDADES);
    }

    // EN_OCS tiene su propio rechazo desde F15-H01: RequiereResolucion (prueba aparte).
    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"BORRADOR_UNIDADES", "EN_OCS"},
            mode = EnumSource.Mode.EXCLUDE)
    void dadaUnaEtapaConOperacionPropia_cuandoSeUsaElAvanceGenerico_entoncesSeRechaza(
            TipoDeEtapa etapa) {
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapasTodasActivas(),
                        etapa,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of());
        Solicitante conTodosLosRoles =
                new Solicitante(
                        "todo.demo",
                        Arrays.stream(TipoDeEtapa.values())
                                .map(una -> rolDe(una))
                                .collect(java.util.stream.Collectors.toSet()),
                        Optional.empty());

        assertThatThrownBy(() -> proceso.avanzar(conTodosLosRoles, 1, HASH_DISTRIBUTIVO, AHORA))
                .isInstanceOf(EtapaConOperacionPropia.class);
    }

    @Test
    void dadoElCoordinador_cuandoSeVenSusAcciones_entoncesPuedeEnviarSuParcialPendiente() {
        ProcesoAprobacion proceso = nuevo(false, "DS", "TDS");

        assertThat(proceso.acciones(COORDINADOR_DS))
                .containsExactly(AccionDelProceso.ENVIAR_PARCIAL);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
        enviar(proceso, COORDINADOR_DS, "DS");
        assertThat(proceso.acciones(COORDINADOR_DS)).isEmpty();
    }

    @Test
    void dadoBorradorUnidades_cuandoSeVenLasAccionesDelGestor_entoncesPuedeAvanzar() {
        assertThat(enBorradorUnidades().acciones(GESTOR)).containsExactly(AccionDelProceso.AVANZAR);
        assertThat(enBorradorUnidades().acciones(VICERRECTOR)).isEmpty();
    }

    @Test
    void dadaLaCopiaDeLaConfiguracion_cuandoSePideElNombreDeLaEtapa_entoncesEsElDeEsaCopia() {
        assertThat(nuevo(false, "DS").nombreDeLaEtapa()).isEqualTo("Nombre de BORRADOR_CARRERA");
    }

    private static List<Transicion> enviar(
            ProcesoAprobacion proceso, Solicitante quien, String carrera) {
        return proceso.enviarParcial(carrera, quien, 1, HASH_PARCIAL, HASH_DISTRIBUTIVO, AHORA);
    }

    // Enviar a revisión (F10-H03, data-model.md, D-97).

    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final CondicionesDeRevision LISTO =
            new CondicionesDeRevision(true, false, Optional.of("c".repeat(64)), 0, 0);

    @Test
    void dadoUnConsolidadoListo_cuandoLoEnvioARevision_entoncesPasaAEnRevisionVra() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        List<Transicion> nuevas = revisar(proceso, LISTO);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.EN_CONSOLIDACION,
                                TipoDeEtapa.EN_REVISION_VRA,
                                Optional.empty(),
                                2,
                                HASH_DISTRIBUTIVO,
                                "consolidacion.demo",
                                AHORA));
        assertThat(proceso.transiciones()).isEqualTo(nuevas);
    }

    @Test
    void dadaEnRevisionVraDesactivada_cuandoLoEnvioARevision_entoncesPasaALaSiguienteActiva() {
        List<EtapaDelProceso> sinRevision =
                etapas(false).stream()
                        .map(
                                etapa ->
                                        etapa.tipo() == TipoDeEtapa.EN_REVISION_VRA
                                                ? new EtapaDelProceso(
                                                        etapa.tipo(),
                                                        false,
                                                        etapa.nombre(),
                                                        etapa.rol(),
                                                        etapa.admiteDevolucion())
                                                : etapa)
                        .toList();
        ProcesoAprobacion proceso = enConsolidacion(sinRevision);

        revisar(proceso, LISTO);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.CONGELADO_EN_FIRMA);
    }

    @Test
    void dadoElDistributivoYaEnRevision_cuandoLoEnvioOtraVez_entoncesEtapaNoAdmiteRevision() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));
        revisar(proceso, LISTO);

        assertThatThrownBy(() -> revisar(proceso, LISTO)).isInstanceOf(EtapaNoAdmiteRevision.class);
        assertThatThrownBy(() -> proceso.validarRevision(RESPONSABLE))
                .isInstanceOf(EtapaNoAdmiteRevision.class);
        assertThat(proceso.transiciones()).hasSize(1);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
    }

    @Test
    void
            dadoElDistributivoEnBorradorCarrera_cuandoLoEnvioARevision_entoncesEtapaNoAdmiteRevision() {
        ProcesoAprobacion proceso = nuevo(false, "DS");

        assertThatThrownBy(() -> proceso.validarRevision(RESPONSABLE))
                .isInstanceOf(EtapaNoAdmiteRevision.class);
        assertThatThrownBy(() -> revisar(proceso, LISTO)).isInstanceOf(EtapaNoAdmiteRevision.class);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void
            dadoUnParcialPendienteEnConsolidacion_cuandoLoEnvioARevision_entoncesParcialesPendientes() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));
        proceso.sumarParciales(List.of("NUEVA"));

        assertThatThrownBy(() -> proceso.validarRevision(RESPONSABLE))
                .isInstanceOf(ParcialesPendientes.class);
        assertThatThrownBy(() -> revisar(proceso, LISTO)).isInstanceOf(ParcialesPendientes.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoUnConsolidadoListo_cuandoSoloSeValida_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        proceso.validarRevision(RESPONSABLE);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.transiciones()).isEmpty();
    }

    // US2: las inconsistencias abiertas (MD-07).

    @Test
    void dadaUnaDuraAbierta_cuandoLoEnvioARevision_entoncesInconsistenciasAbiertasConUna() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(() -> revisar(proceso, abiertas(1, 0)))
                .isInstanceOfSatisfying(
                        InconsistenciasAbiertas.class,
                        error -> {
                            assertThat(error.duras()).isEqualTo(1);
                            assertThat(error.blandas()).isZero();
                        });
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadasDosDurasYUnaBlanda_cuandoLoEnvioARevision_entoncesDiceLasTres() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(() -> revisar(proceso, abiertas(2, 1)))
                .isInstanceOfSatisfying(
                        InconsistenciasAbiertas.class,
                        error -> assertThat(error.total()).isEqualTo(3));
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadasSoloBlandasJustificadas_cuandoLoEnvioARevision_entoncesPasa() {
        // Las justificadas no están en las abiertas: el panel ya las descuenta (MD-07).
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        revisar(proceso, abiertas(0, 0));

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
    }

    // US3: el consolidado al día y con su informe, en el orden de research §8.

    @Test
    void dadoUnDistributivoSinConsolidar_cuandoLoEnvioARevision_entoncesSinConsolidar() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(
                        () ->
                                revisar(
                                        proceso,
                                        new CondicionesDeRevision(
                                                false, false, Optional.of("c".repeat(64)), 0, 0)))
                .isInstanceOf(SinConsolidar.class);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void
            dadoUnConsolidadoDesactualizado_cuandoLoEnvioARevision_entoncesConsolidadoDesactualizado() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(
                        () ->
                                revisar(
                                        proceso,
                                        new CondicionesDeRevision(
                                                true, true, Optional.of("c".repeat(64)), 0, 0)))
                .isInstanceOf(ConsolidadoDesactualizado.class);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoUnConsolidadoSinInforme_cuandoLoEnvioARevision_entoncesSinInforme() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(
                        () ->
                                revisar(
                                        proceso,
                                        new CondicionesDeRevision(
                                                true, false, Optional.empty(), 0, 0)))
                .isInstanceOf(SinInforme.class);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoQueFaltaTodo_cuandoLoEnvioARevision_entoncesRespondeElPrimeroDelOrden() {
        CondicionesDeRevision nada = new CondicionesDeRevision(false, true, Optional.empty(), 2, 1);
        ProcesoAprobacion conPendiente = enConsolidacion(etapas(false));
        conPendiente.sumarParciales(List.of("NUEVA"));

        assertThatThrownBy(() -> revisar(conPendiente, nada))
                .isInstanceOf(ParcialesPendientes.class);
        assertThatThrownBy(() -> revisar(enConsolidacion(etapas(false)), nada))
                .isInstanceOf(SinConsolidar.class);
        assertThatThrownBy(
                        () ->
                                revisar(
                                        enConsolidacion(etapas(false)),
                                        new CondicionesDeRevision(
                                                true, true, Optional.empty(), 2, 1)))
                .isInstanceOf(ConsolidadoDesactualizado.class);
        assertThatThrownBy(
                        () ->
                                revisar(
                                        enConsolidacion(etapas(false)),
                                        new CondicionesDeRevision(
                                                true, false, Optional.empty(), 2, 1)))
                .isInstanceOf(SinInforme.class);
    }

    // US4: solo el rol de EN_CONSOLIDACION (FR-003, FR-013).

    @Test
    void dadoOtroRol_cuandoEnviaARevision_entoncesRolDeOtraEtapaSinCambiarNada() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThatThrownBy(() -> proceso.validarRevision(VICERRECTOR))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThatThrownBy(
                        () ->
                                proceso.enviarARevision(
                                        VICERRECTOR, LISTO, 2, HASH_DISTRIBUTIVO, AHORA))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoOtroRolConfiguradoParaConsolidacion_cuandoEnvia_entoncesValeElDeLaConfiguracion() {
        List<EtapaDelProceso> conVicerrector =
                etapas(false).stream()
                        .map(
                                etapa ->
                                        etapa.tipo() == TipoDeEtapa.EN_CONSOLIDACION
                                                ? new EtapaDelProceso(
                                                        etapa.tipo(),
                                                        true,
                                                        etapa.nombre(),
                                                        "VICERRECTOR",
                                                        etapa.admiteDevolucion())
                                                : etapa)
                        .toList();

        assertThatThrownBy(() -> revisar(enConsolidacion(conVicerrector), LISTO))
                .isInstanceOf(RolDeOtraEtapa.class);
        ProcesoAprobacion proceso = enConsolidacion(conVicerrector);
        proceso.enviarARevision(VICERRECTOR, LISTO, 2, HASH_DISTRIBUTIVO, AHORA);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
    }

    @Test
    void dadoElResponsableEnConsolidacion_cuandoConsulta_entoncesVeEnviarARevisionYOtroNo() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThat(proceso.acciones(RESPONSABLE)).contains(AccionDelProceso.ENVIAR_A_REVISION);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
        assertThat(nuevo(false, "DS").acciones(RESPONSABLE)).isEmpty();
    }

    // Devolver (F12-H05, data-model.md, D-98).

    private static final DevolucionDeParcial DEVOLVER_DS =
            DevolucionDeParcial.de("DS", "Faltan docentes en primer nivel.");
    private static final DevolucionDeParcial DEVOLVER_TDS =
            DevolucionDeParcial.de("TDS", "Un docente sin clase en segundo nivel.");

    @Test
    void dadoElDistributivoEnConsolidacion_cuandoDevuelvoDosCarreras_entoncesVuelveABorrador() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS", "EL");

        List<Transicion> nuevas = devolver(proceso, DEVOLVER_DS, DEVOLVER_TDS);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(proceso.parciales())
                .isEqualTo(
                        Map.of(
                                "DS", EstadoDeEnvio.DEVUELTO,
                                "TDS", EstadoDeEnvio.DEVUELTO,
                                "EL", EstadoDeEnvio.ENVIADO));
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.DEVOLUCION,
                                TipoDeEtapa.EN_CONSOLIDACION,
                                TipoDeEtapa.BORRADOR_CARRERA,
                                Optional.empty(),
                                2,
                                HASH_DISTRIBUTIVO,
                                "consolidacion.demo",
                                AHORA,
                                Map.of(
                                        "DS", DEVOLVER_DS.observacion(),
                                        "TDS", DEVOLVER_TDS.observacion())));
        assertThat(proceso.transiciones()).containsExactlyElementsOf(nuevas);
    }

    @Test
    void dadaUnaDevolucion_cuandoSeRegistra_entoncesLlevaLaVersionVigenteSinCrearOtra() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        Transicion devolucion = devolver(proceso, DEVOLVER_DS).getFirst();

        // FR-014: la versión es la que llega, la vigente; devolver no la cambia.
        assertThat(devolucion.version()).isEqualTo(2);
    }

    @Test
    void dadasTodasLasCarreras_cuandoLasDevuelvo_entoncesSePermite() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        devolver(proceso, DEVOLVER_DS, DEVOLVER_TDS);

        assertThat(proceso.parciales().values()).containsOnly(EstadoDeEnvio.DEVUELTO);
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoDevuelvo_entoncesNoSeDevuelveNinguna() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        assertThatThrownBy(
                        () ->
                                devolver(
                                        proceso,
                                        DEVOLVER_DS,
                                        DevolucionDeParcial.de("OTRA", "Una carrera que no está.")))
                .isInstanceOf(ParcialNoEncontrado.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.parciales().values()).containsOnly(EstadoDeEnvio.ENVIADO);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoUnParcialPendiente_cuandoLoDevuelvo_entoncesParcialNoEnviadoConSuCarrera() {
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas(false),
                        TipoDeEtapa.EN_CONSOLIDACION,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO, "TDS", EstadoDeEnvio.PENDIENTE),
                        List.of());

        assertThatThrownBy(() -> devolver(proceso, DEVOLVER_DS, DEVOLVER_TDS))
                .isInstanceOf(ParcialNoEnviado.class)
                .extracting(error -> ((ParcialNoEnviado) error).carrera())
                .isEqualTo("TDS");
        assertThat(proceso.parciales()).containsEntry("DS", EstadoDeEnvio.ENVIADO);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadaUnaCarreraRepetida_cuandoDevuelvo_entoncesCarreraRepetida() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        assertThatThrownBy(
                        () ->
                                devolver(
                                        proceso,
                                        DEVOLVER_DS,
                                        DevolucionDeParcial.de("DS", "Otra observación igual.")))
                .isInstanceOf(CarreraRepetida.class)
                .extracting(error -> ((CarreraRepetida) error).carrera())
                .isEqualTo("DS");
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadaUnaDevolucionQueCabe_cuandoSoloSeValida_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        proceso.validarDevolucion(RESPONSABLE, List.of(DEVOLVER_DS));

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.parciales().values()).containsOnly(EstadoDeEnvio.ENVIADO);
        assertThat(proceso.transiciones()).isEmpty();
    }

    // Corregir y reenviar (F12-H05, US2, research §5).

    @Test
    void dadoUnParcialDevuelto_cuandoSuCoordinadorLoReenvia_entoncesSeAceptaYElEnviadoNo() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");
        devolver(proceso, DEVOLVER_DS);

        proceso.validarEnvio("DS", COORDINADOR_DS);
        assertThatThrownBy(() -> proceso.validarEnvio("TDS", COORDINADOR_TDS))
                .isInstanceOf(ParcialYaEnviado.class);
        assertThat(proceso.acciones(COORDINADOR_DS))
                .containsExactly(AccionDelProceso.ENVIAR_PARCIAL);
        assertThat(proceso.acciones(COORDINADOR_TDS)).isEmpty();
    }

    @Test
    void
            dadasDosCarrerasDevueltas_cuandoReenvianUnaYOtra_entoncesConLaUltimaVuelveAConsolidacion() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");
        devolver(proceso, DEVOLVER_DS, DEVOLVER_TDS);

        enviar(proceso, COORDINADOR_DS, "DS");
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);

        List<Transicion> nuevas = enviar(proceso, COORDINADOR_TDS, "TDS");
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevas)
                .extracting(Transicion::tipo)
                .containsExactly(TipoDeTransicion.ENVIO_PARCIAL, TipoDeTransicion.PASO_DE_ETAPA);
        assertThat(proceso.parciales().values()).containsOnly(EstadoDeEnvio.ENVIADO);
    }

    @Test
    void dadaBorradorUnidadesActiva_cuandoReenviaLaUltimaDevuelta_entoncesPasaABorradorUnidades() {
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas(true),
                        TipoDeEtapa.EN_CONSOLIDACION,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of());
        devolver(proceso, DEVOLVER_DS);

        enviar(proceso, COORDINADOR_DS, "DS");

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_UNIDADES);
    }

    @Test
    void dadoUnParcialDevueltoEnConsolidacion_cuandoSeEnviaARevision_entoncesParcialesPendientes() {
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas(false),
                        TipoDeEtapa.EN_CONSOLIDACION,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO, "TDS", EstadoDeEnvio.DEVUELTO),
                        List.of());

        assertThatThrownBy(() -> proceso.validarRevision(RESPONSABLE))
                .isInstanceOf(ParcialesPendientes.class);
    }

    @Test
    void dadaUnaCarreraReenviada_cuandoSeDevuelveOtraVez_entoncesSePermiteYValeLaUltima() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS");
        devolver(proceso, DEVOLVER_DS);
        enviar(proceso, COORDINADOR_DS, "DS");

        devolver(proceso, DevolucionDeParcial.de("DS", "Todavía falta un docente."));

        assertThat(proceso.transiciones())
                .filteredOn(una -> una.tipo() == TipoDeTransicion.DEVOLUCION)
                .extracting(una -> una.observaciones().get("DS").texto())
                .containsExactly("Faltan docentes en primer nivel.", "Todavía falta un docente.");
    }

    // Solo el rol de la etapa devuelve (F12-H05, US4, research §10).

    @Test
    void dadoOtroRol_cuandoDevuelve_entoncesRolDeOtraEtapaSinCambiarNada() {
        ProcesoAprobacion proceso = enConsolidacionCon("DS", "TDS");

        assertThatThrownBy(
                        () ->
                                proceso.devolver(
                                        VICERRECTOR,
                                        List.of(DEVOLVER_DS),
                                        2,
                                        HASH_DISTRIBUTIVO,
                                        AHORA))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThatThrownBy(() -> proceso.validarDevolucion(COORDINADOR_DS, List.of(DEVOLVER_DS)))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoOtroRolConfiguradoParaConsolidacion_cuandoDevuelve_entoncesValeElDeLaConfiguracion() {
        ProcesoAprobacion proceso = enConsolidacion(conConsolidacion("VICERRECTOR", true));

        assertThatThrownBy(() -> proceso.validarDevolucion(RESPONSABLE, List.of(DEVOLVER_DS)))
                .isInstanceOf(RolDeOtraEtapa.class);
        proceso.validarDevolucion(VICERRECTOR, List.of(DEVOLVER_DS));
    }

    @Test
    void dadoElDistributivoFueraDeConsolidacion_cuandoDevuelvo_entoncesEtapaNoAdmiteDevolucion() {
        ProcesoAprobacion borrador = nuevo(false, "DS");
        ProcesoAprobacion enRevision = enConsolidacion(etapas(false));
        revisar(enRevision, LISTO);

        assertThatThrownBy(() -> devolver(borrador, DEVOLVER_DS))
                .isInstanceOf(EtapaNoAdmiteDevolucion.class);
        assertThatThrownBy(() -> devolver(enRevision, DEVOLVER_DS))
                .isInstanceOf(EtapaNoAdmiteDevolucion.class);
        ProcesoAprobacion devuelto = enConsolidacionCon("DS");
        devolver(devuelto, DEVOLVER_DS);
        assertThatThrownBy(() -> devolver(devuelto, DEVOLVER_DS))
                .isInstanceOf(EtapaNoAdmiteDevolucion.class);
    }

    @Test
    void dadaConsolidacionSinDevolucionEnLaCopia_cuandoDevuelvo_entoncesDevolucionDesactivada() {
        ProcesoAprobacion proceso =
                enConsolidacion(conConsolidacion("RESPONSABLE_CONSOLIDACION", false));

        assertThatThrownBy(() -> devolver(proceso, DEVOLVER_DS))
                .isInstanceOf(DevolucionDesactivada.class);
        assertThat(proceso.acciones(RESPONSABLE))
                .containsExactly(AccionDelProceso.ENVIAR_A_REVISION);
    }

    @Test
    void dadoQueFaltaTodo_cuandoDevuelvo_entoncesRespondeElPrimeroDelOrden() {
        // Etapa antes que la carrera repetida y que el parcial: research §10.
        ProcesoAprobacion borrador = nuevo(false, "DS");
        assertThatThrownBy(
                        () ->
                                devolver(
                                        borrador,
                                        DEVOLVER_DS,
                                        DevolucionDeParcial.de("DS", "Otra observación igual.")))
                .isInstanceOf(EtapaNoAdmiteDevolucion.class);
        // Carrera repetida antes que una sin parcial.
        assertThatThrownBy(
                        () ->
                                devolver(
                                        enConsolidacionCon("DS"),
                                        DevolucionDeParcial.de("OTRA", "Una carrera que no está."),
                                        DEVOLVER_DS,
                                        DEVOLVER_DS))
                .isInstanceOf(CarreraRepetida.class);
    }

    @Test
    void dadoElResponsableEnConsolidacion_cuandoConsulta_entoncesVeDevolverYOtroNo() {
        ProcesoAprobacion proceso = enConsolidacion(etapas(false));

        assertThat(proceso.acciones(RESPONSABLE))
                .containsExactly(
                        AccionDelProceso.ENVIAR_A_REVISION, AccionDelProceso.DEVOLVER_PARCIALES);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
    }

    private static List<EtapaDelProceso> conConsolidacion(String rol, boolean admiteDevolucion) {
        return etapas(false).stream()
                .map(
                        etapa ->
                                etapa.tipo() == TipoDeEtapa.EN_CONSOLIDACION
                                        ? new EtapaDelProceso(
                                                etapa.tipo(),
                                                true,
                                                etapa.nombre(),
                                                rol,
                                                admiteDevolucion)
                                        : etapa)
                .toList();
    }

    private static List<Transicion> devolver(
            ProcesoAprobacion proceso, DevolucionDeParcial... devoluciones) {
        return proceso.devolver(RESPONSABLE, List.of(devoluciones), 2, HASH_DISTRIBUTIVO, AHORA);
    }

    private static ProcesoAprobacion enConsolidacionCon(String... carreras) {
        Map<String, EstadoDeEnvio> enviados = new java.util.TreeMap<>();
        for (String carrera : carreras) {
            enviados.put(carrera, EstadoDeEnvio.ENVIADO);
        }
        return ProcesoAprobacion.de(
                "2026-I", etapas(false), TipoDeEtapa.EN_CONSOLIDACION, CREADO, enviados, List.of());
    }

    private static CondicionesDeRevision abiertas(long duras, long blandas) {
        return new CondicionesDeRevision(true, false, Optional.of("c".repeat(64)), duras, blandas);
    }

    private static List<Transicion> revisar(
            ProcesoAprobacion proceso, CondicionesDeRevision condiciones) {
        return proceso.enviarARevision(RESPONSABLE, condiciones, 2, HASH_DISTRIBUTIVO, AHORA);
    }

    private static ProcesoAprobacion enConsolidacion(List<EtapaDelProceso> etapas) {
        return ProcesoAprobacion.de(
                "2026-I",
                etapas,
                TipoDeEtapa.EN_CONSOLIDACION,
                CREADO,
                Map.of("DS", EstadoDeEnvio.ENVIADO, "TDS", EstadoDeEnvio.ENVIADO),
                List.of());
    }

    private static ProcesoAprobacion nuevo(boolean conUnidades, String... carreras) {
        return ProcesoAprobacion.nuevo("2026-I", etapas(conUnidades), CREADO, List.of(carreras));
    }

    // Aprobar el contenido y congelar la versión (F13-H01, data-model.md, D-100).

    private static final Solicitante RECTOR =
            new Solicitante("rectorado.demo", Set.of("RECTOR"), Optional.empty());
    private static final String HASH_CONGELADO = "d".repeat(64);

    @Test
    void dadoElDistributivoEnRevision_cuandoElVicerrectorAprueba_entoncesPasaACongeladoEnFirma() {
        ProcesoAprobacion proceso = enRevision(etapas(false));

        List<Transicion> nuevas = proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.CONGELADO_EN_FIRMA);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.EN_REVISION_VRA,
                                TipoDeEtapa.CONGELADO_EN_FIRMA,
                                Optional.empty(),
                                2,
                                HASH_CONGELADO,
                                "vicerrectorado.demo",
                                AHORA));
        assertThat(proceso.transiciones()).isEqualTo(nuevas);
    }

    @Test
    void dadoOtroRol_cuandoAprueba_entoncesRolDeOtraEtapaSinCambiarNada() {
        for (Solicitante otro : List.of(RECTOR, RESPONSABLE, COORDINADOR_DS)) {
            ProcesoAprobacion proceso = enRevision(etapas(false));

            assertThatThrownBy(() -> proceso.validarAprobacion(otro))
                    .isInstanceOf(RolDeOtraEtapa.class);
            assertThatThrownBy(() -> proceso.aprobarContenido(otro, 2, HASH_CONGELADO, AHORA))
                    .isInstanceOf(RolDeOtraEtapa.class);
            assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
            assertThat(proceso.transiciones()).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"BORRADOR_CARRERA", "EN_CONSOLIDACION", "CONGELADO_EN_FIRMA", "EN_OCS"})
    void dadoElDistributivoEnOtraEtapa_cuandoElVicerrectorAprueba_entoncesEtapaNoAdmiteAprobacion(
            TipoDeEtapa etapa) {
        ProcesoAprobacion proceso = en(etapa, etapas(false));

        assertThatThrownBy(() -> proceso.validarAprobacion(VICERRECTOR))
                .isInstanceOf(EtapaNoAdmiteAprobacion.class);
        assertThatThrownBy(() -> proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA))
                .isInstanceOf(EtapaNoAdmiteAprobacion.class);
        assertThat(proceso.estado()).isEqualTo(etapa);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadaUnaAprobacionQueCabe_cuandoSoloSeValida_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = enRevision(etapas(false));

        proceso.validarAprobacion(VICERRECTOR);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadasDosAprobaciones_cuandoLlegaLaSegunda_entoncesEtapaNoAdmiteAprobacion() {
        ProcesoAprobacion proceso = enRevision(etapas(false));
        proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA);

        assertThatThrownBy(() -> proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA))
                .isInstanceOf(EtapaNoAdmiteAprobacion.class);
        assertThat(proceso.transiciones()).hasSize(1);
    }

    @Test
    void dadoElDistributivoEnRevision_cuandoSeVenLasAcciones_entoncesSoloElVicerrectorAprueba() {
        ProcesoAprobacion proceso = enRevision(etapas(false));

        assertThat(proceso.acciones(VICERRECTOR))
                .containsExactly(AccionDelProceso.APROBAR_CONTENIDO);
        assertThat(proceso.acciones(RESPONSABLE)).isEmpty();
        assertThat(proceso.acciones(RECTOR)).isEmpty();
        assertThat(proceso.acciones(COORDINADOR_DS)).isEmpty();
    }

    @Test
    void dadoElDistributivoCongelado_cuandoSeVenLasAccionesDelVicerrector_entoncesNoHayNinguna() {
        ProcesoAprobacion proceso = enRevision(etapas(false));
        proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA);

        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
    }

    @Test
    void dadaLaRevisionActiva_cuandoSePreguntaSiCongelaAlAvanzar_entoncesSoloDesdeLaRevision() {
        assertThat(enRevision(etapas(false)).congelaAlAvanzar()).isTrue();
        assertThat(enConsolidacion(etapas(false)).congelaAlAvanzar()).isFalse();
        assertThat(nuevo(false, "DS").congelaAlAvanzar()).isFalse();
        assertThat(en(TipoDeEtapa.CONGELADO_EN_FIRMA, etapas(false)).congelaAlAvanzar()).isFalse();
        assertThat(en(TipoDeEtapa.PUBLICADO, etapas(false)).congelaAlAvanzar()).isFalse();
    }

    @Test
    void dadaLaRevisionDesactivada_cuandoSePreguntaSiCongelaAlAvanzar_entoncesDesdeConsolidacion() {
        assertThat(enConsolidacion(sinRevision()).congelaAlAvanzar()).isTrue();
    }

    @Test
    void dadoUnProcesoSinPasoAFirma_cuandoSePideLaCongelacion_entoncesNoHay() {
        assertThat(enRevision(etapas(false)).congelacion()).isEmpty();
    }

    @Test
    void dadoElPasoAFirma_cuandoSePideLaCongelacion_entoncesEsEsaTransicion() {
        ProcesoAprobacion proceso = enRevision(etapas(false));
        Transicion paso = proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA).get(0);

        assertThat(proceso.congelacion()).contains(paso);
    }

    @Test
    void dadaLaRevisionDesactivada_cuandoSeEnviaARevision_entoncesEseEnvioEsLaCongelacion() {
        ProcesoAprobacion proceso = enConsolidacion(sinRevision());
        Transicion paso = revisar(proceso, LISTO).get(0);

        assertThat(proceso.congelacion()).contains(paso);
    }

    private static List<EtapaDelProceso> sinRevision() {
        return etapas(false).stream()
                .map(
                        etapa ->
                                etapa.tipo() == TipoDeEtapa.EN_REVISION_VRA
                                        ? new EtapaDelProceso(
                                                etapa.tipo(),
                                                false,
                                                etapa.nombre(),
                                                etapa.rol(),
                                                etapa.admiteDevolucion())
                                        : etapa)
                .toList();
    }

    private static ProcesoAprobacion enRevision(List<EtapaDelProceso> etapas) {
        return en(TipoDeEtapa.EN_REVISION_VRA, etapas);
    }

    private static ProcesoAprobacion en(TipoDeEtapa etapa, List<EtapaDelProceso> etapas) {
        return ProcesoAprobacion.de(
                "2026-I",
                etapas,
                etapa,
                CREADO,
                Map.of("DS", EstadoDeEnvio.ENVIADO, "TDS", EstadoDeEnvio.ENVIADO),
                List.of());
    }

    private static ProcesoAprobacion enBorradorUnidades() {
        return ProcesoAprobacion.de(
                "2026-I",
                etapas(true),
                TipoDeEtapa.BORRADOR_UNIDADES,
                CREADO,
                Map.of("DS", EstadoDeEnvio.ENVIADO),
                List.of());
    }

    // Como la configuración por defecto (flujo-por-defecto.yaml), con BORRADOR_UNIDADES a elección.
    private static List<EtapaDelProceso> etapas(boolean conUnidades) {
        return Arrays.stream(TipoDeEtapa.values())
                .map(
                        tipo ->
                                new EtapaDelProceso(
                                        tipo,
                                        switch (tipo) {
                                            case BORRADOR_UNIDADES -> conUnidades;
                                            case REMITIDO_ZONAL -> false;
                                            default -> true;
                                        },
                                        "Nombre de " + tipo,
                                        rolDe(tipo),
                                        admiteDevolucion(tipo)))
                .toList();
    }

    private static List<EtapaDelProceso> etapasTodasActivas() {
        return Arrays.stream(TipoDeEtapa.values())
                .map(
                        tipo ->
                                new EtapaDelProceso(
                                        tipo,
                                        true,
                                        "Nombre de " + tipo,
                                        rolDe(tipo),
                                        admiteDevolucion(tipo)))
                .toList();
    }

    // Como la plantilla (flujo-por-defecto.yaml).
    private static boolean admiteDevolucion(TipoDeEtapa tipo) {
        return switch (tipo) {
            case EN_CONSOLIDACION, EN_REVISION_VRA, CONGELADO_EN_FIRMA, EN_OCS -> true;
            default -> false;
        };
    }

    private static String rolDe(TipoDeEtapa tipo) {
        return switch (tipo) {
            case BORRADOR_CARRERA -> "COORDINADOR_CARRERA";
            case BORRADOR_UNIDADES -> "GESTOR_UNIDAD";
            case EN_CONSOLIDACION, CONGELADO_EN_FIRMA -> "RESPONSABLE_CONSOLIDACION";
            case EN_REVISION_VRA, REMITIDO_ZONAL -> "VICERRECTOR";
            case EN_OCS, APROBADO, PUBLICADO -> "SECRETARIA_GENERAL";
        };
    }

    // La cadena de firmas completa pasa a EN_OCS (F14-H03, D-104).

    private static final Instant FIRMADA = AHORA.plusSeconds(3600);

    private static ProcesoAprobacion enFirma() {
        ProcesoAprobacion proceso = enRevision(etapas(false));
        proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA);
        return proceso;
    }

    @Test
    void dadoElDistributivoEnFirma_cuandoSeCompletanLasFirmas_entoncesPasaAEnOcsPorElSistema() {
        ProcesoAprobacion proceso = enFirma();

        List<Transicion> nuevas = proceso.completarFirmas(2, FIRMADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_OCS);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.CONGELADO_EN_FIRMA,
                                TipoDeEtapa.EN_OCS,
                                Optional.empty(),
                                2,
                                HASH_CONGELADO,
                                "sistema",
                                FIRMADA));
    }

    @Test
    void dadoElAvisoRepetido_cuandoYaEstaEnOcs_entoncesNoAgregaOtraTransicion() {
        ProcesoAprobacion proceso = enFirma();
        proceso.completarFirmas(2, FIRMADA);

        assertThat(proceso.completarFirmas(2, FIRMADA.plusSeconds(5))).isEmpty();
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_OCS);
        assertThat(proceso.transiciones()).hasSize(2);
    }

    @Test
    void dadoOtraVersion_cuandoSeCompletanLasFirmas_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = enFirma();

        assertThat(proceso.completarFirmas(3, FIRMADA)).isEmpty();
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.CONGELADO_EN_FIRMA);
    }

    @Test
    void dadoElDistributivoEnOtraEtapa_cuandoSeCompletanLasFirmas_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = enRevision(etapas(false));

        assertThat(proceso.completarFirmas(2, FIRMADA)).isEmpty();
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.transiciones()).isEmpty();
    }

    // El rechazo en firma vuelve a la etapa activa anterior (F14-H04, D-105).

    private static final Instant RECHAZADA = FIRMADA.plusSeconds(60);
    private static final RechazoDelFirmante RECHAZO =
            new RechazoDelFirmante(
                    2,
                    "VICERRECTOR",
                    "vicerrectorado.demo",
                    RECHAZADA.minusSeconds(1),
                    new Observacion("Falta la carga de un docente de Software."));

    @Test
    void dadoElDistributivoEnFirma_cuandoSeRechazaEnFirma_entoncesVuelveARevisionPorElSistema() {
        ProcesoAprobacion proceso = enFirma();

        List<Transicion> nuevas = proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.RECHAZO_EN_FIRMA,
                                TipoDeEtapa.CONGELADO_EN_FIRMA,
                                TipoDeEtapa.EN_REVISION_VRA,
                                Optional.empty(),
                                2,
                                HASH_CONGELADO,
                                "sistema",
                                RECHAZADA,
                                Map.of(),
                                Optional.of(RECHAZO)));
        assertThat(proceso.rechazoPendiente()).contains(nuevas.get(0));
    }

    @Test
    void dadaLaRevisionDesactivada_cuandoSeRechazaEnFirma_entoncesVuelveALaEtapaActivaAnterior() {
        ProcesoAprobacion proceso = enConsolidacion(sinRevision());
        revisar(proceso, LISTO);

        List<Transicion> nuevas = proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevas)
                .singleElement()
                .extracting(Transicion::hash)
                .isEqualTo(HASH_DISTRIBUTIVO);
    }

    @Test
    void dadoElAvisoRepetido_cuandoYaVolvioARevision_entoncesNoAgregaOtraTransicion() {
        ProcesoAprobacion proceso = enFirma();
        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        assertThat(proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA.plusSeconds(5))).isEmpty();
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.transiciones()).hasSize(2);
    }

    @Test
    void dadaOtraVersionUOtraEtapa_cuandoSeRechazaEnFirma_entoncesNoCambiaNada() {
        ProcesoAprobacion enFirma = enFirma();
        assertThat(enFirma.rechazarEnFirma(3, RECHAZO, RECHAZADA)).isEmpty();
        assertThat(enFirma.estado()).isEqualTo(TipoDeEtapa.CONGELADO_EN_FIRMA);

        ProcesoAprobacion enOcs = enFirma();
        enOcs.completarFirmas(2, FIRMADA);
        assertThat(enOcs.rechazarEnFirma(2, RECHAZO, RECHAZADA)).isEmpty();
        assertThat(enOcs.estado()).isEqualTo(TipoDeEtapa.EN_OCS);

        ProcesoAprobacion enRevision = enRevision(etapas(false));
        assertThat(enRevision.rechazarEnFirma(2, RECHAZO, RECHAZADA)).isEmpty();
        assertThat(enRevision.transiciones()).isEmpty();
    }

    @Test
    void dadoUnRechazoPendiente_cuandoElVicerrectorAprueba_entoncesRequiereVersionNueva() {
        ProcesoAprobacion proceso = enFirma();
        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        assertThatThrownBy(() -> proceso.validarAprobacion(VICERRECTOR))
                .isInstanceOf(RequiereVersionNueva.class);
        assertThatThrownBy(() -> proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA))
                .isInstanceOf(RequiereVersionNueva.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.transiciones()).hasSize(2);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
    }

    @Test
    void dadoUnRechazoPendienteSinRevision_cuandoSeEnviaARevision_entoncesRequiereVersionNueva() {
        ProcesoAprobacion proceso = enConsolidacion(sinRevision());
        revisar(proceso, LISTO);
        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        assertThatThrownBy(() -> proceso.validarRevision(RESPONSABLE))
                .isInstanceOf(RequiereVersionNueva.class);
        assertThatThrownBy(() -> revisar(proceso, LISTO)).isInstanceOf(RequiereVersionNueva.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
    }

    @Test
    void dadoUnProcesoSinRechazo_cuandoSePideElRechazoPendiente_entoncesNoHay() {
        assertThat(enFirma().rechazoPendiente()).isEmpty();
        assertThat(enRevision(etapas(false)).rechazoPendiente()).isEmpty();
    }

    // La versión nueva tras el rechazo en firma (F12-H04, D-106).

    private static final String HASH_NUEVA = "e".repeat(64);
    private static final Instant NUEVA = RECHAZADA.plusSeconds(1);

    private static ProcesoAprobacion rechazado() {
        ProcesoAprobacion proceso = enFirma();
        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);
        return proceso;
    }

    @Test
    void
            dadoUnRechazoEnFirma_cuandoSeRegistraLaVersionNueva_entoncesQuedaSuTransicionPorElSistema() {
        ProcesoAprobacion proceso = rechazado();

        List<Transicion> nuevas = proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);

        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.VERSION_NUEVA,
                                TipoDeEtapa.EN_REVISION_VRA,
                                TipoDeEtapa.EN_REVISION_VRA,
                                Optional.empty(),
                                3,
                                HASH_NUEVA,
                                "sistema",
                                NUEVA));
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.transiciones()).hasSize(3).endsWith(nuevas.get(0));
    }

    @Test
    void dadaLaVersionNueva_cuandoElVicerrectorAprueba_entoncesYaNoRequiereVersionNueva() {
        ProcesoAprobacion proceso = rechazado();
        proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);

        assertThat(proceso.rechazoPendiente()).isEmpty();
        assertThat(proceso.acciones(VICERRECTOR))
                .containsExactly(AccionDelProceso.APROBAR_CONTENIDO);
        proceso.validarAprobacion(VICERRECTOR);
        List<Transicion> aprobada =
                proceso.aprobarContenido(VICERRECTOR, 3, HASH_NUEVA, NUEVA.plusSeconds(60));

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.CONGELADO_EN_FIRMA);
        assertThat(aprobada).singleElement().extracting(Transicion::version).isEqualTo(3);
    }

    @Test
    void dadaLaVersionNuevaYaRegistrada_cuandoSeRepite_entoncesNoAgregaOtra() {
        ProcesoAprobacion proceso = rechazado();
        proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);

        assertThat(proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA.plusSeconds(5))).isEmpty();
        assertThat(proceso.transiciones()).hasSize(3);
    }

    @Test
    void dadoQueNoHayRechazoDeEsaVersion_cuandoSeRegistraLaVersionNueva_entoncesNoCambiaNada() {
        ProcesoAprobacion sinRechazo = enFirma();
        assertThat(sinRechazo.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA)).isEmpty();
        assertThat(sinRechazo.transiciones()).hasSize(1);

        ProcesoAprobacion otraVersion = rechazado();
        assertThat(otraVersion.registrarVersionNueva(1, 2, HASH_NUEVA, NUEVA)).isEmpty();
        assertThat(otraVersion.transiciones()).hasSize(2);
    }

    @Test
    void
            dadaLaRevisionDesactivada_cuandoSeRegistraLaVersionNueva_entoncesQuedaEnLaEtapaALaQueVolvio() {
        ProcesoAprobacion proceso = enConsolidacion(sinRevision());
        revisar(proceso, LISTO);
        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);

        List<Transicion> nuevas = proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);

        assertThat(nuevas)
                .singleElement()
                .satisfies(
                        una -> {
                            assertThat(una.desde()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
                            assertThat(una.hacia()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
                        });
        proceso.validarRevision(RESPONSABLE);
    }

    @Test
    void dadoUnRechazo_cuandoSePideElRechazoVigente_entoncesEstaHastaLaCongelacionSiguiente() {
        ProcesoAprobacion proceso = enFirma();
        assertThat(proceso.rechazoVigente()).isEmpty();

        proceso.rechazarEnFirma(2, RECHAZO, RECHAZADA);
        Transicion rechazo = proceso.transiciones().getLast();
        assertThat(proceso.rechazoVigente()).contains(rechazo);

        proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);
        assertThat(proceso.rechazoVigente()).contains(rechazo);

        proceso.aprobarContenido(VICERRECTOR, 3, HASH_NUEVA, NUEVA.plusSeconds(60));
        assertThat(proceso.rechazoVigente()).isEmpty();
    }

    @Test
    void dadaLaVersionNuevaAprobada_cuandoSeRechazaOtraVez_entoncesHayOtraVersionNueva() {
        ProcesoAprobacion proceso = rechazado();
        proceso.registrarVersionNueva(2, 3, HASH_NUEVA, NUEVA);
        proceso.aprobarContenido(VICERRECTOR, 3, HASH_NUEVA, NUEVA.plusSeconds(60));
        Instant otraVez = NUEVA.plusSeconds(120);

        assertThat(proceso.rechazarEnFirma(3, RECHAZO, otraVez)).hasSize(1);
        Transicion segundo = proceso.transiciones().getLast();
        assertThat(proceso.registrarVersionNueva(3, 4, "f".repeat(64), otraVez))
                .singleElement()
                .extracting(Transicion::version)
                .isEqualTo(4);

        assertThat(proceso.rechazoVigente()).contains(segundo);
        assertThat(proceso.acciones(VICERRECTOR))
                .containsExactly(AccionDelProceso.APROBAR_CONTENIDO);
    }

    @Test
    void dadoElProceso_cuandoSePreguntaSiAdmiteElRechazo_entoncesSoloEnFirmaConLaCongelada() {
        ProcesoAprobacion enFirma = enFirma();
        assertThat(enFirma.admiteRechazoEnFirma(2)).isTrue();
        assertThat(enFirma.admiteRechazoEnFirma(3)).isFalse();
        assertThat(rechazado().admiteRechazoEnFirma(2)).isFalse();
        assertThat(enRevision(etapas(false)).admiteRechazoEnFirma(2)).isFalse();
    }

    // La resolución del OCS (F15-H01, D-107). En firma con AHORA (2026-11-07, 10:00 en Guayaquil);
    // a EN_OCS con FIRMADA, una hora después: el mismo día en la institución.

    private static final Solicitante SECRETARIA =
            new Solicitante("secretaria.demo", Set.of("SECRETARIA_GENERAL"), Optional.empty());
    private static final java.time.LocalDate DIA_EN_OCS = java.time.LocalDate.of(2026, 11, 7);
    private static final Instant TRES_DIAS_DESPUES = Instant.parse("2026-11-10T15:00:00Z");

    private static ProcesoAprobacion enOcs() {
        ProcesoAprobacion proceso = enFirma();
        proceso.completarFirmas(2, FIRMADA);
        return proceso;
    }

    private static ResolucionOcs resolucionDel(java.time.LocalDate fecha) {
        return new ResolucionOcs("RPC-SO-12-No.123-2026", fecha, "b".repeat(64), "r.pdf", 2048);
    }

    @Test
    void dadoElDistributivoEnOcs_cuandoLaSecretariaRegistraLaResolucion_entoncesPasaAAprobado() {
        ProcesoAprobacion proceso = enOcs();
        ResolucionOcs resolucion = resolucionDel(DIA_EN_OCS.plusDays(2));

        List<Transicion> nuevas =
                proceso.registrarResolucion(SECRETARIA, resolucion, TRES_DIAS_DESPUES);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.APROBADO);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.EN_OCS,
                                TipoDeEtapa.APROBADO,
                                Optional.empty(),
                                2,
                                HASH_CONGELADO,
                                "secretaria.demo",
                                TRES_DIAS_DESPUES,
                                Map.of(),
                                Optional.empty(),
                                Optional.of(resolucion)));
        assertThat(proceso.resolucion()).contains(nuevas.get(0));
        // F18-H01: en APROBADO, la Secretaría ya puede publicar.
        assertThat(proceso.acciones(SECRETARIA)).containsExactly(AccionDelProceso.PUBLICAR);
    }

    @Test
    void dadaLaFechaDelDiaDeEntradaODeHoy_cuandoSeRegistra_entoncesSeAdmite() {
        assertThat(
                        enOcs().registrarResolucion(
                                        SECRETARIA, resolucionDel(DIA_EN_OCS), TRES_DIAS_DESPUES))
                .hasSize(1);
        assertThat(
                        enOcs().registrarResolucion(
                                        SECRETARIA,
                                        resolucionDel(java.time.LocalDate.of(2026, 11, 10)),
                                        TRES_DIAS_DESPUES))
                .hasSize(1);
    }

    @Test
    void dadaUnaFechaAnteriorALaEntradaOFutura_cuandoSeRegistra_entoncesFueraDeRangoSinCambiar() {
        ProcesoAprobacion proceso = enOcs();

        assertThatThrownBy(
                        () ->
                                proceso.registrarResolucion(
                                        SECRETARIA,
                                        resolucionDel(DIA_EN_OCS.minusDays(1)),
                                        TRES_DIAS_DESPUES))
                .isInstanceOf(ResolucionFueraDeRango.class);
        assertThatThrownBy(
                        () ->
                                proceso.registrarResolucion(
                                        SECRETARIA,
                                        resolucionDel(java.time.LocalDate.of(2026, 11, 11)),
                                        TRES_DIAS_DESPUES))
                .isInstanceOf(ResolucionFueraDeRango.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_OCS);
    }

    @Test
    void
            dadaLaNocheEnGuayaquil_cuandoSeRegistraConLaFechaDeUtc_entoncesEsFuturaParaLaInstitucion() {
        // 2026-11-08T03:00Z son las 22:00 del 7 en Guayaquil: el 8 todavía es futuro.
        Instant noche = Instant.parse("2026-11-08T03:00:00Z");

        assertThatThrownBy(
                        () ->
                                enOcs().registrarResolucion(
                                                SECRETARIA,
                                                resolucionDel(java.time.LocalDate.of(2026, 11, 8)),
                                                noche))
                .isInstanceOf(ResolucionFueraDeRango.class);
        assertThat(enOcs().registrarResolucion(SECRETARIA, resolucionDel(DIA_EN_OCS), noche))
                .hasSize(1);
    }

    @Test
    void dadoOtroRolUOtraEtapa_cuandoSeRegistra_entoncesSeRechazaSinCambiar() {
        ProcesoAprobacion enOcs = enOcs();
        assertThatThrownBy(
                        () ->
                                enOcs.registrarResolucion(
                                        VICERRECTOR, resolucionDel(DIA_EN_OCS), TRES_DIAS_DESPUES))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(enOcs.estado()).isEqualTo(TipoDeEtapa.EN_OCS);

        ProcesoAprobacion enFirma = enFirma();
        assertThatThrownBy(
                        () ->
                                enFirma.registrarResolucion(
                                        SECRETARIA, resolucionDel(DIA_EN_OCS), TRES_DIAS_DESPUES))
                .isInstanceOf(EtapaNoAdmiteResolucion.class);

        ProcesoAprobacion aprobado = enOcs();
        aprobado.registrarResolucion(SECRETARIA, resolucionDel(DIA_EN_OCS), TRES_DIAS_DESPUES);
        assertThatThrownBy(
                        () ->
                                aprobado.registrarResolucion(
                                        SECRETARIA, resolucionDel(DIA_EN_OCS), TRES_DIAS_DESPUES))
                .as("la segunda, ya en APROBADO")
                .isInstanceOf(EtapaNoAdmiteResolucion.class);
    }

    @Test
    void dadoElDistributivoEnOcs_cuandoSeUsaElAvanceGenerico_entoncesRequiereResolucion() {
        ProcesoAprobacion proceso = enOcs();

        assertThatThrownBy(() -> proceso.validarAvance(SECRETARIA))
                .isInstanceOf(RequiereResolucion.class);
        assertThatThrownBy(() -> proceso.avanzar(SECRETARIA, 2, HASH_CONGELADO, TRES_DIAS_DESPUES))
                .isInstanceOf(RequiereResolucion.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_OCS);
    }

    @Test
    void dadoElDistributivoEnOcs_cuandoSeVenLasAcciones_entoncesSoloLaSecretariaRegistra() {
        ProcesoAprobacion proceso = enOcs();

        assertThat(proceso.acciones(SECRETARIA))
                .containsExactly(AccionDelProceso.REGISTRAR_RESOLUCION);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
        assertThat(enFirma().acciones(SECRETARIA)).isEmpty();
        assertThat(proceso.resolucion()).isEmpty();
    }

    // Publicar (F18-H01, D-111): desde APROBADO, con el rol de esa etapa, con la versión congelada.

    private static final Instant PUBLICADA = TRES_DIAS_DESPUES.plusSeconds(3600);

    private static ProcesoAprobacion aprobado() {
        ProcesoAprobacion proceso = enOcs();
        proceso.registrarResolucion(
                SECRETARIA, resolucionDel(DIA_EN_OCS.plusDays(2)), TRES_DIAS_DESPUES);
        return proceso;
    }

    @Test
    void dadoElDistributivoAprobado_cuandoLaSecretariaLoPublica_entoncesPasaAPublicado() {
        ProcesoAprobacion proceso = aprobado();

        List<Transicion> nuevas = proceso.publicar(SECRETARIA, PUBLICADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.APROBADO,
                                TipoDeEtapa.PUBLICADO,
                                Optional.empty(),
                                2,
                                HASH_CONGELADO,
                                "secretaria.demo",
                                PUBLICADA));
        assertThat(proceso.publicacion()).contains(nuevas.getFirst());
        assertThat(proceso.acciones(SECRETARIA)).isEmpty();
        // F18-H02, D-113: publicado, el Vicerrector puede iniciar una reforma; nadie edita.
        assertThat(proceso.acciones(VICERRECTOR)).containsExactly(AccionDelProceso.INICIAR_REFORMA);
    }

    @Test
    void dadaLaPublicacion_cuandoSePideSuDestino_entoncesLosTresRolesSinCarreraNiSecretaria() {
        ProcesoAprobacion proceso = aprobado();
        Transicion publicacion = proceso.publicar(SECRETARIA, PUBLICADA).getFirst();

        assertThat(proceso.destinoDe(publicacion))
                .containsExactly(
                        new DestinoDeNotificacion("DOCENTE", Optional.empty()),
                        new DestinoDeNotificacion("OPERADOR_DATOS", Optional.empty()),
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.empty()));
    }

    @Test
    void dadoElDistributivoAprobado_cuandoSeConsultanLasAcciones_entoncesSoloLaSecretariaPublica() {
        ProcesoAprobacion proceso = aprobado();

        assertThat(proceso.acciones(SECRETARIA)).containsExactly(AccionDelProceso.PUBLICAR);
        assertThat(proceso.acciones(VICERRECTOR)).isEmpty();
        assertThat(proceso.publicacion()).isEmpty();
    }

    // F18-H01, US3: publicado, la resolución ya no se registra, ni otra vez.
    @Test
    void
            dadoElDistributivoPublicado_cuandoSeRegistraLaResolucion_entoncesEtapaNoAdmiteResolucion() {
        ProcesoAprobacion proceso = aprobado();
        proceso.publicar(SECRETARIA, PUBLICADA);
        int antes = proceso.transiciones().size();

        assertThatThrownBy(
                        () ->
                                proceso.registrarResolucion(
                                        SECRETARIA,
                                        resolucionDel(DIA_EN_OCS.plusDays(2)),
                                        PUBLICADA.plusSeconds(60)))
                .isInstanceOf(EtapaNoAdmiteResolucion.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
        assertThat(proceso.transiciones()).hasSize(antes);
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"APROBADO"},
            mode = EnumSource.Mode.EXCLUDE)
    void dadoElDistributivoEnOtraEtapa_cuandoSePublica_entoncesSeRechazaSinCambiar(
            TipoDeEtapa etapa) {
        ProcesoAprobacion proceso = en(etapa, etapasTodasActivas());

        assertThatThrownBy(() -> proceso.publicar(SECRETARIA, PUBLICADA))
                .isInstanceOf(EtapaNoAdmitePublicacion.class);
        assertThat(proceso.estado()).isEqualTo(etapa);
        assertThat(proceso.publicacion()).isEmpty();
    }

    @Test
    void dadoElDistributivoPublicado_cuandoSePublicaOtraVez_entoncesSeRechaza() {
        ProcesoAprobacion proceso = aprobado();
        proceso.publicar(SECRETARIA, PUBLICADA);

        assertThatThrownBy(() -> proceso.publicar(SECRETARIA, PUBLICADA.plusSeconds(1)))
                .isInstanceOf(EtapaNoAdmitePublicacion.class);
    }

    @Test
    void dadoOtroRol_cuandoPublica_entoncesRolDeOtraEtapaSinCambiar() {
        ProcesoAprobacion proceso = aprobado();

        assertThatThrownBy(() -> proceso.publicar(VICERRECTOR, PUBLICADA))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.APROBADO);
    }

    @Test
    void dadaOtraConfiguracionDeAprobado_cuandoSePublica_entoncesPublicaSuRol() {
        List<EtapaDelProceso> etapas =
                etapas(false).stream()
                        .map(
                                una ->
                                        una.tipo() == TipoDeEtapa.APROBADO
                                                ? new EtapaDelProceso(
                                                        una.tipo(),
                                                        una.activa(),
                                                        una.nombre(),
                                                        "MIEMBRO_OCS",
                                                        una.admiteDevolucion())
                                                : una)
                        .toList();
        ProcesoAprobacion proceso = enRevision(etapas);
        proceso.aprobarContenido(VICERRECTOR, 2, HASH_CONGELADO, AHORA);
        proceso.completarFirmas(2, FIRMADA);
        proceso.registrarResolucion(
                SECRETARIA, resolucionDel(DIA_EN_OCS.plusDays(2)), TRES_DIAS_DESPUES);
        Solicitante ocs = new Solicitante("ocs.demo", Set.of("MIEMBRO_OCS"), Optional.empty());

        assertThatThrownBy(() -> proceso.publicar(SECRETARIA, PUBLICADA))
                .isInstanceOf(RolDeOtraEtapa.class);
        assertThat(proceso.publicar(ocs, PUBLICADA)).hasSize(1);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
    }

    // La reforma (F18-H02, D-113): desde PUBLICADO, el rol de EN_REVISION_VRA, con su motivo; la
    // versión publicada sigue vigente hasta que se publique la de la reforma.

    private static final Instant REFORMADA = PUBLICADA.plusSeconds(3600);
    private static final String HASH_REFORMA = "f".repeat(64);
    private static final Observacion MOTIVO =
            new Observacion("Cambia el docente de Software por una licencia.");

    private static ProcesoAprobacion publicado() {
        ProcesoAprobacion proceso = aprobado();
        proceso.publicar(SECRETARIA, PUBLICADA);
        return proceso;
    }

    private static Transicion pasoA(TipoDeEtapa desde, TipoDeEtapa hacia, int version, Instant en) {
        return new Transicion(
                TipoDeTransicion.PASO_DE_ETAPA,
                desde,
                hacia,
                Optional.empty(),
                version,
                HASH_CONGELADO,
                "secretaria.demo",
                en);
    }

    // Publicado con otra configuración: solo la transición a PUBLICADO, que es lo que se lee.
    private static ProcesoAprobacion publicadoCon(List<EtapaDelProceso> etapas) {
        return ProcesoAprobacion.de(
                "2026-I",
                etapas,
                TipoDeEtapa.PUBLICADO,
                CREADO,
                Map.of("DS", EstadoDeEnvio.ENVIADO),
                List.of(pasoA(TipoDeEtapa.APROBADO, TipoDeEtapa.PUBLICADO, 2, PUBLICADA)));
    }

    @Test
    void
            dadoElDistributivoPublicado_cuandoElVicerrectorIniciaUnaReforma_entoncesVuelveAConsolidacion() {
        ProcesoAprobacion proceso = publicado();
        Transicion publicacion = proceso.publicacion().orElseThrow();

        List<Transicion> nuevas =
                proceso.iniciarReforma(VICERRECTOR, MOTIVO, 3, HASH_REFORMA, REFORMADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(nuevas)
                .containsExactly(
                        new Transicion(
                                TipoDeTransicion.REFORMA,
                                TipoDeEtapa.PUBLICADO,
                                TipoDeEtapa.EN_CONSOLIDACION,
                                Optional.empty(),
                                3,
                                HASH_REFORMA,
                                "vicerrectorado.demo",
                                REFORMADA,
                                Map.of(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(new Reforma(2, MOTIVO))));
        assertThat(proceso.transiciones()).endsWith(nuevas.getFirst());
        assertThat(proceso.reformaEnCurso()).contains(nuevas.getFirst());
        // La publicada sigue vigente hasta que se publique la de la reforma.
        assertThat(proceso.publicacion()).contains(publicacion);
    }

    @Test
    void dadoElDistributivoPublicado_cuandoSeValidaLaReforma_entoncesNoCambiaNada() {
        ProcesoAprobacion proceso = publicado();
        int antes = proceso.transiciones().size();

        proceso.validarReforma(VICERRECTOR);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
        assertThat(proceso.transiciones()).hasSize(antes);
        assertThat(proceso.reformaEnCurso()).isEmpty();
    }

    @Test
    void dadoOtroRol_cuandoIniciaUnaReforma_entoncesRolDeOtraEtapaSinCambiar() {
        for (Solicitante quien : List.of(SECRETARIA, RECTOR, RESPONSABLE)) {
            ProcesoAprobacion proceso = publicado();
            int antes = proceso.transiciones().size();

            assertThatThrownBy(() -> proceso.validarReforma(quien))
                    .as(quien.usuario())
                    .isInstanceOf(RolDeOtraEtapa.class);
            assertThatThrownBy(
                            () -> proceso.iniciarReforma(quien, MOTIVO, 3, HASH_REFORMA, REFORMADA))
                    .isInstanceOf(RolDeOtraEtapa.class);
            assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
            assertThat(proceso.transiciones()).hasSize(antes);
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"PUBLICADO"},
            mode = EnumSource.Mode.EXCLUDE)
    void dadoElDistributivoEnOtraEtapa_cuandoSeIniciaUnaReforma_entoncesSeRechazaSinCambiar(
            TipoDeEtapa etapa) {
        ProcesoAprobacion proceso = en(etapa, etapasTodasActivas());

        assertThatThrownBy(() -> proceso.validarReforma(VICERRECTOR))
                .isInstanceOf(EtapaNoAdmiteReforma.class);
        assertThatThrownBy(
                        () ->
                                proceso.iniciarReforma(
                                        VICERRECTOR, MOTIVO, 3, HASH_REFORMA, REFORMADA))
                .isInstanceOf(EtapaNoAdmiteReforma.class);
        assertThat(proceso.estado()).isEqualTo(etapa);
        assertThat(proceso.transiciones()).isEmpty();
    }

    @Test
    void dadoOtroRolEnLaRevisionDeLaConfiguracion_cuandoIniciaUnaReforma_entoncesLoUsa() {
        List<EtapaDelProceso> etapas =
                etapas(false).stream()
                        .map(
                                una ->
                                        una.tipo() == TipoDeEtapa.EN_REVISION_VRA
                                                ? new EtapaDelProceso(
                                                        una.tipo(),
                                                        una.activa(),
                                                        una.nombre(),
                                                        "RECTOR",
                                                        una.admiteDevolucion())
                                                : una)
                        .toList();

        assertThatThrownBy(() -> publicadoCon(etapas).validarReforma(VICERRECTOR))
                .isInstanceOf(RolDeOtraEtapa.class);
        ProcesoAprobacion proceso = publicadoCon(etapas);
        proceso.iniciarReforma(RECTOR, MOTIVO, 3, HASH_REFORMA, REFORMADA);
        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
    }

    @Test
    void dadaLaConsolidacionDesactivada_cuandoSeIniciaUnaReforma_entoncesVaALaSiguienteActiva() {
        List<EtapaDelProceso> etapas =
                etapas(false).stream()
                        .map(
                                una ->
                                        una.tipo() == TipoDeEtapa.EN_CONSOLIDACION
                                                ? new EtapaDelProceso(
                                                        una.tipo(),
                                                        false,
                                                        una.nombre(),
                                                        una.rol(),
                                                        una.admiteDevolucion())
                                                : una)
                        .toList();
        ProcesoAprobacion proceso = publicadoCon(etapas);

        List<Transicion> nuevas =
                proceso.iniciarReforma(VICERRECTOR, MOTIVO, 3, HASH_REFORMA, REFORMADA);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(nuevas.getFirst().hacia()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
    }

    @Test
    void dadaLaReformaPublicada_cuandoSeConsulta_entoncesYaNoEstaEnCursoYSePuedeReformarOtraVez() {
        Transicion reforma =
                new Transicion(
                        TipoDeTransicion.REFORMA,
                        TipoDeEtapa.PUBLICADO,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        Optional.empty(),
                        3,
                        HASH_REFORMA,
                        "vicerrectorado.demo",
                        REFORMADA,
                        Map.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new Reforma(2, MOTIVO)));
        Transicion segunda =
                pasoA(TipoDeEtapa.APROBADO, TipoDeEtapa.PUBLICADO, 3, REFORMADA.plusSeconds(3600));
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas(false),
                        TipoDeEtapa.PUBLICADO,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of(
                                pasoA(TipoDeEtapa.APROBADO, TipoDeEtapa.PUBLICADO, 2, PUBLICADA),
                                reforma,
                                segunda));

        assertThat(proceso.reformaEnCurso()).isEmpty();
        assertThat(proceso.publicacion()).contains(segunda);

        // Una reforma de la reforma: la publicada es la 3, y sigue vigente durante la nueva.
        List<Transicion> nuevas =
                proceso.iniciarReforma(
                        VICERRECTOR, MOTIVO, 4, HASH_REFORMA, REFORMADA.plusSeconds(7200));
        assertThat(nuevas.getFirst().reforma()).contains(new Reforma(3, MOTIVO));
        assertThat(proceso.publicacion()).contains(segunda);
        assertThat(proceso.reformaEnCurso()).contains(nuevas.getFirst());
    }

    @Test
    void
            dadoElDistributivoPublicado_cuandoSeConsultanLasAcciones_entoncesSoloElVicerrectorReforma() {
        ProcesoAprobacion proceso = publicado();

        assertThat(proceso.acciones(VICERRECTOR)).containsExactly(AccionDelProceso.INICIAR_REFORMA);
        for (Solicitante quien : List.of(SECRETARIA, RECTOR, RESPONSABLE, COORDINADOR_DS)) {
            assertThat(proceso.acciones(quien)).as(quien.usuario()).isEmpty();
        }
    }

    @Test
    void dadaLaReforma_cuandoSePideSuDestino_entoncesLaLlegadaYLosTresDeLaPublicacion() {
        ProcesoAprobacion proceso = publicado();
        Transicion reforma =
                proceso.iniciarReforma(VICERRECTOR, MOTIVO, 3, HASH_REFORMA, REFORMADA).getFirst();

        assertThat(proceso.destinoDe(reforma))
                .containsExactly(
                        new DestinoDeNotificacion("RESPONSABLE_CONSOLIDACION", Optional.empty()),
                        new DestinoDeNotificacion("DOCENTE", Optional.empty()),
                        new DestinoDeNotificacion("OPERADOR_DATOS", Optional.empty()),
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.empty()));
    }
}
