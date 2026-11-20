package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * El plazo de la etapa actual del proceso (F12-H03, D-114): cuándo llegó el distributivo, qué
 * reinicia el plazo, cuándo vence y a quién se avisa. El plazo no bloquea nada (FR-003).
 */
class PlazoDelProcesoTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant ENVIO = Instant.parse("2026-11-03T15:00:00Z");
    private static final Instant SEGUNDO_ENVIO = Instant.parse("2026-11-04T15:00:00Z");
    private static final String HASH = "a".repeat(64);
    private static final PlazosDeEtapas PLAZOS = PlazosDeEtapas.de(PlazoTest.etapasPorDefecto());

    private static final Solicitante COORDINADOR_DS =
            new Solicitante("coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.of("DS"));
    private static final Solicitante COORDINADOR_TDS =
            new Solicitante("otra.coordinacion", Set.of("COORDINADOR_CARRERA"), Optional.of("TDS"));
    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty());

    // La llegada.

    @Test
    void dadoUnDistributivoRecienCreado_cuandoSePideLaLlegada_entoncesEsSuCreacion() {
        assertThat(nuevo("DS", "TDS").llegadaALaEtapa()).isEqualTo(CREADO);
    }

    @Test
    void dadoElEnvioDeUnParcialQueNoEsElUltimo_cuandoSePideLaLlegada_entoncesNoCambia() {
        ProcesoAprobacion proceso = nuevo("DS", "TDS");
        proceso.enviarParcial("DS", COORDINADOR_DS, 1, HASH, HASH, ENVIO);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(proceso.llegadaALaEtapa()).isEqualTo(CREADO);
    }

    @Test
    void dadoElUltimoParcial_cuandoPasaAConsolidacion_entoncesLaLlegadaEsEseEnvio() {
        ProcesoAprobacion proceso = enConsolidacion();

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.llegadaALaEtapa()).isEqualTo(SEGUNDO_ENVIO);
    }

    @Test
    void dadaUnaDevolucion_cuandoVuelveABorradorCarrera_entoncesElPlazoEmpiezaDeNuevo() {
        ProcesoAprobacion proceso = enConsolidacion();
        Instant devuelto = SEGUNDO_ENVIO.plus(Duration.ofDays(2));
        proceso.devolver(
                RESPONSABLE,
                List.of(DevolucionDeParcial.de("DS", "Falta el docente de una asignatura.")),
                1,
                HASH,
                devuelto);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(proceso.llegadaALaEtapa()).isEqualTo(devuelto);
    }

    @Test
    void dadoUnRechazoEnFirmaYLaVersionNueva_cuandoSePideLaLlegada_entoncesEsLaDelRechazo() {
        ProcesoAprobacion proceso = en(TipoDeEtapa.EN_REVISION_VRA, etapas("VICERRECTOR"));
        Instant aprobado = Instant.parse("2026-11-05T15:00:00Z");
        Instant rechazado = Instant.parse("2026-11-06T15:00:00Z");
        proceso.aprobarContenido(VICERRECTOR, 1, HASH, aprobado);
        proceso.rechazarEnFirma(
                1,
                new RechazoDelFirmante(
                        2,
                        "VICERRECTOR",
                        "vicerrectorado.demo",
                        rechazado.minusSeconds(1),
                        new Observacion("Falta la carga de un docente de Software.")),
                rechazado);
        proceso.registrarVersionNueva(1, 2, "b".repeat(64), rechazado.plusSeconds(1));

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(proceso.llegadaALaEtapa()).isEqualTo(rechazado);
    }

    @Test
    void dadaUnaReforma_cuandoVuelveAConsolidacion_entoncesElPlazoEmpiezaDeNuevo() {
        Instant publicada = Instant.parse("2026-11-08T15:00:00Z");
        Instant reformada = Instant.parse("2026-11-09T15:00:00Z");
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas("VICERRECTOR"),
                        TipoDeEtapa.PUBLICADO,
                        CREADO,
                        Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of(
                                new Transicion(
                                        TipoDeTransicion.PASO_DE_ETAPA,
                                        TipoDeEtapa.APROBADO,
                                        TipoDeEtapa.PUBLICADO,
                                        Optional.empty(),
                                        1,
                                        HASH,
                                        "secretaria.demo",
                                        publicada)));
        proceso.iniciarReforma(
                VICERRECTOR,
                new Observacion("Cambia el docente de Software por una licencia."),
                2,
                HASH,
                reformada);

        assertThat(proceso.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(proceso.llegadaALaEtapa()).isEqualTo(reformada);
    }

    // El plazo actual.

    @Test
    void dadoElDistributivoEnConsolidacion_cuandoSePideElPlazo_entoncesEsLaLlegadaMasCincoDias() {
        assertThat(enConsolidacion().plazoActual(PLAZOS))
                .contains(Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, SEGUNDO_ENVIO, 5));
    }

    @Test
    void dadoElDistributivoPublicado_cuandoSePideElPlazo_entoncesNoHay() {
        assertThat(en(TipoDeEtapa.PUBLICADO, etapas("VICERRECTOR")).plazoActual(PLAZOS)).isEmpty();
    }

    // El vencimiento.

    @Test
    void dadoElPlazoQueVenceHoy_cuandoSeRevisa_entoncesNoHayVencimiento() {
        // El plazo de EN_CONSOLIDACION vence el 2026-11-09, a la medianoche de Ecuador.
        Instant hoy = Instant.parse("2026-11-10T04:00:00Z");

        assertThat(enConsolidacion().vencimiento(PLAZOS, hoy)).isEmpty();
    }

    @Test
    void dadoElPlazoVencido_cuandoSeRevisa_entoncesHayUnVencimientoConSuAtraso() {
        Instant manana = Instant.parse("2026-11-10T12:00:00Z");

        Optional<VencimientoDePlazo> vencimiento = enConsolidacion().vencimiento(PLAZOS, manana);

        assertThat(vencimiento)
                .contains(
                        new VencimientoDePlazo(
                                "2026-I",
                                Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, SEGUNDO_ENVIO, 5),
                                manana,
                                1));
    }

    @Test
    void dadoElDistributivoPublicado_cuandoSeRevisa_entoncesNoVenceNunca() {
        assertThat(
                        en(TipoDeEtapa.PUBLICADO, etapas("VICERRECTOR"))
                                .vencimiento(PLAZOS, Instant.parse("2027-06-01T12:00:00Z")))
                .isEmpty();
    }

    @Test
    void dadaUnaEtapaVencida_cuandoSePidenLasAcciones_entoncesSonLasMismasQueATiempo() {
        // El plazo no bloquea nada (FR-003): ninguna acción ni validación consulta el reloj.
        ProcesoAprobacion proceso = enConsolidacion();

        assertThat(proceso.vencimiento(PLAZOS, Instant.parse("2026-11-30T12:00:00Z"))).isPresent();
        assertThat(proceso.acciones(RESPONSABLE))
                .containsExactly(
                        AccionDelProceso.ENVIAR_A_REVISION, AccionDelProceso.DEVOLVER_PARCIALES);
    }

    // A quién se avisa.

    @Test
    void dadoBorradorCarrera_cuandoVence_entoncesSoloLasCarrerasSinEnviarYElVicerrector() {
        Map<String, EstadoDeEnvio> parciales = new TreeMap<>();
        parciales.put("DS", EstadoDeEnvio.ENVIADO);
        parciales.put("SI", EstadoDeEnvio.DEVUELTO);
        parciales.put("TDS", EstadoDeEnvio.PENDIENTE);
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I",
                        etapas("VICERRECTOR"),
                        TipoDeEtapa.BORRADOR_CARRERA,
                        CREADO,
                        parciales,
                        List.of());

        assertThat(proceso.destinosDelVencimiento())
                .containsExactly(
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.of("SI")),
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.of("TDS")),
                        new DestinoDeNotificacion("VICERRECTOR", Optional.empty()));
    }

    @Test
    void dadaConsolidacion_cuandoVence_entoncesElResponsableYElVicerrector() {
        assertThat(enConsolidacion().destinosDelVencimiento())
                .containsExactly(
                        new DestinoDeNotificacion("RESPONSABLE_CONSOLIDACION", Optional.empty()),
                        new DestinoDeNotificacion("VICERRECTOR", Optional.empty()));
    }

    @Test
    void dadaLaRevisionDelVicerrectorado_cuandoVence_entoncesUnSoloDestino() {
        assertThat(en(TipoDeEtapa.EN_REVISION_VRA, etapas("VICERRECTOR")).destinosDelVencimiento())
                .containsExactly(new DestinoDeNotificacion("VICERRECTOR", Optional.empty()));
    }

    @Test
    void dadaOtraConfiguracionDeLaRevision_cuandoVenceEnOcs_entoncesUsaEseRol() {
        assertThat(en(TipoDeEtapa.EN_OCS, etapas("RECTOR")).destinosDelVencimiento())
                .containsExactly(
                        new DestinoDeNotificacion("SECRETARIA_GENERAL", Optional.empty()),
                        new DestinoDeNotificacion("RECTOR", Optional.empty()));
    }

    @Test
    void dadaLaEtapaEnFirma_cuandoVence_entoncesElRolDeLaEtapaYNoElFirmante() {
        assertThat(
                        en(TipoDeEtapa.CONGELADO_EN_FIRMA, etapas("VICERRECTOR"))
                                .destinosDelVencimiento())
                .containsExactly(
                        new DestinoDeNotificacion("RESPONSABLE_CONSOLIDACION", Optional.empty()),
                        new DestinoDeNotificacion("VICERRECTOR", Optional.empty()));
    }

    @Test
    void dadoUnVencimiento_cuandoSeArma_entoncesExigeAlMenosUnDiaDeAtraso() {
        Plazo plazo = Plazo.de(TipoDeEtapa.EN_OCS, CREADO, 3);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new VencimientoDePlazo("2026-I", plazo, CREADO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(LocalDate.ofInstant(CREADO, ProcesoAprobacion.ZONA).plusDays(3))
                .isEqualTo(plazo.fechaLimite());
    }

    private static ProcesoAprobacion nuevo(String... carreras) {
        return ProcesoAprobacion.nuevo("2026-I", etapas("VICERRECTOR"), CREADO, List.of(carreras));
    }

    private static ProcesoAprobacion enConsolidacion() {
        ProcesoAprobacion proceso = nuevo("DS", "TDS");
        proceso.enviarParcial("DS", COORDINADOR_DS, 1, HASH, HASH, ENVIO);
        proceso.enviarParcial("TDS", COORDINADOR_TDS, 1, HASH, HASH, SEGUNDO_ENVIO);
        return proceso;
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

    // Como la configuración por defecto, con el rol de EN_REVISION_VRA a elección.
    private static List<EtapaDelProceso> etapas(String rolDeLaRevision) {
        return Arrays.stream(TipoDeEtapa.values())
                .map(
                        tipo ->
                                new EtapaDelProceso(
                                        tipo,
                                        tipo != TipoDeEtapa.BORRADOR_UNIDADES
                                                && tipo != TipoDeEtapa.REMITIDO_ZONAL,
                                        "Nombre de " + tipo,
                                        switch (tipo) {
                                            case BORRADOR_CARRERA -> "COORDINADOR_CARRERA";
                                            case BORRADOR_UNIDADES -> "GESTOR_UNIDAD";
                                            case EN_CONSOLIDACION, CONGELADO_EN_FIRMA ->
                                                    "RESPONSABLE_CONSOLIDACION";
                                            case EN_REVISION_VRA -> rolDeLaRevision;
                                            case REMITIDO_ZONAL -> "VICERRECTOR";
                                            case EN_OCS, APROBADO, PUBLICADO ->
                                                    "SECRETARIA_GENERAL";
                                        },
                                        tipo == TipoDeEtapa.EN_CONSOLIDACION
                                                || tipo == TipoDeEtapa.EN_REVISION_VRA
                                                || tipo == TipoDeEtapa.CONGELADO_EN_FIRMA
                                                || tipo == TipoDeEtapa.EN_OCS))
                .toList();
    }
}
