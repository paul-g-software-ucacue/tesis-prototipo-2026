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

    @ParameterizedTest
    @EnumSource(
            value = TipoDeEtapa.class,
            names = {"BORRADOR_UNIDADES"},
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
                                                        etapa.rol())
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
                                        rolDe(tipo)))
                .toList();
    }

    private static List<EtapaDelProceso> etapasTodasActivas() {
        return Arrays.stream(TipoDeEtapa.values())
                .map(tipo -> new EtapaDelProceso(tipo, true, "Nombre de " + tipo, rolDe(tipo)))
                .toList();
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
}
