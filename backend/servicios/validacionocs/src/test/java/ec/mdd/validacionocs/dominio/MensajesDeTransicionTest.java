package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * A quién toca cada transición y con qué mensaje (F16-H01, D-108, contracts/eventos.md): el rol de
 * la etapa de llegada en la copia de la configuración, con su nombre, la versión y el enlace.
 */
class MensajesDeTransicionTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T15:00:00Z");
    private static final String HASH = "a".repeat(64);

    @Test
    void dadoElEnvioDeUnParcial_cuandoSeNotifica_entoncesVaAlRolDeLaSiguienteEtapaActiva() {
        Transicion envio =
                transicion(
                        TipoDeTransicion.ENVIO_PARCIAL,
                        "BORRADOR_CARRERA",
                        "BORRADOR_CARRERA",
                        Optional.of("DS"));

        List<MensajeDeTransicion> sinUnidades =
                MensajesDeTransicion.de(proceso(etapas(false)), envio, Map.of());
        List<MensajeDeTransicion> conUnidades =
                MensajesDeTransicion.de(proceso(etapas(true)), envio, Map.of());

        assertThat(sinUnidades)
                .singleElement()
                .satisfies(
                        mensaje -> {
                            assertThat(mensaje.destino())
                                    .isEqualTo(
                                            new DestinoDeNotificacion(
                                                    "RESPONSABLE_CONSOLIDACION", Optional.empty()));
                            assertThat(mensaje.titulo())
                                    .isEqualTo("La carrera DS envió su parcial");
                            assertThat(mensaje.detalle()).isEqualTo("Periodo 2026-I, versión 2.");
                            assertThat(mensaje.enlace()).isEqualTo("/consolidado");
                        });
        assertThat(conUnidades.get(0).destino().rol()).isEqualTo("GESTOR_UNIDAD");
        assertThat(conUnidades.get(0).enlace()).isEqualTo("/");
    }

    @Test
    void
            dadoUnPasoDeEtapa_cuandoSeNotifica_entoncesVaAlRolDeLaEtapaDeLlegadaConSuNombreYSuEnlace() {
        Map<TipoDeEtapa, String> enlaces =
                Map.of(
                        TipoDeEtapa.EN_CONSOLIDACION, "/consolidado",
                        TipoDeEtapa.EN_REVISION_VRA, "/consolidado",
                        TipoDeEtapa.CONGELADO_EN_FIRMA, "/firmas-pendientes",
                        TipoDeEtapa.EN_OCS, "/resolucion-ocs",
                        TipoDeEtapa.APROBADO, "/resolucion-ocs");
        enlaces.forEach(
                (hacia, enlace) -> {
                    Transicion paso =
                            transicion(
                                    TipoDeTransicion.PASO_DE_ETAPA,
                                    "BORRADOR_CARRERA",
                                    hacia.name(),
                                    Optional.empty());

                    MensajeDeTransicion mensaje =
                            MensajesDeTransicion.de(proceso(etapas(false)), paso, Map.of())
                                    .getFirst();

                    assertThat(mensaje.destino().rol()).as(hacia.name()).isEqualTo(rolDe(hacia));
                    assertThat(mensaje.titulo())
                            .isEqualTo("El distributivo llegó a «Nombre de " + hacia + "»");
                    assertThat(mensaje.detalle()).isEqualTo("Periodo 2026-I, versión 2.");
                    assertThat(mensaje.enlace()).as(hacia.name()).isEqualTo(enlace);
                });
    }

    @Test
    void dadaUnaConfiguracionConOtroRol_cuandoElDistributivoLlega_entoncesVaAEseRol() {
        List<EtapaDelProceso> etapas =
                etapas(false, tipo -> tipo == TipoDeEtapa.EN_REVISION_VRA ? "RECTOR" : rolDe(tipo));
        Transicion paso =
                transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        "EN_CONSOLIDACION",
                        "EN_REVISION_VRA",
                        Optional.empty());

        assertThat(
                        MensajesDeTransicion.de(proceso(etapas), paso, Map.of())
                                .getFirst()
                                .destino()
                                .rol())
                .isEqualTo("RECTOR");
    }

    @Test
    void
            dadaUnaDevolucionDeDosCarreras_cuandoSeNotifica_entoncesUnMensajePorCarreraConSuObservacion() {
        Transicion devolucion =
                new Transicion(
                        TipoDeTransicion.DEVOLUCION,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.empty(),
                        2,
                        HASH,
                        "consolidacion.demo",
                        AHORA,
                        Map.of(
                                "DS", new Observacion("Falta el docente de Redes II"),
                                "TDS", new Observacion("Revisar las horas de tutoría")));

        List<MensajeDeTransicion> mensajes =
                MensajesDeTransicion.de(proceso(etapas(false)), devolucion, Map.of());

        assertThat(mensajes)
                .extracting(MensajeDeTransicion::destino)
                .containsExactly(
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.of("DS")),
                        new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.of("TDS")));
        assertThat(mensajes.getFirst().titulo())
                .isEqualTo("El parcial de DS fue devuelto a «Nombre de BORRADOR_CARRERA»");
        assertThat(mensajes.getFirst().detalle())
                .isEqualTo("Observación: «Falta el docente de Redes II»");
        assertThat(mensajes.get(1).detalle())
                .isEqualTo("Observación: «Revisar las horas de tutoría»");
        assertThat(mensajes)
                .allSatisfy(mensaje -> assertThat(mensaje.enlace()).isEqualTo("/oferta"));
    }

    @Test
    void
            dadoUnRechazoEnFirma_cuandoSeNotifica_entoncesVaAlRolDeLaEtapaDeVueltaConElPasoYLaObservacion() {
        String observacion = "o".repeat(500);
        Transicion rechazo =
                new Transicion(
                        TipoDeTransicion.RECHAZO_EN_FIRMA,
                        TipoDeEtapa.CONGELADO_EN_FIRMA,
                        TipoDeEtapa.EN_REVISION_VRA,
                        Optional.empty(),
                        2,
                        HASH,
                        ProcesoAprobacion.SISTEMA,
                        AHORA,
                        Map.of(),
                        Optional.of(
                                new RechazoDelFirmante(
                                        3,
                                        "RECTOR",
                                        "rectorado.demo",
                                        AHORA,
                                        new Observacion(observacion))));

        MensajeDeTransicion mensaje =
                MensajesDeTransicion.de(proceso(etapas(false)), rechazo, Map.of()).getFirst();

        assertThat(mensaje.destino().rol()).isEqualTo("VICERRECTOR");
        assertThat(mensaje.titulo())
                .isEqualTo(
                        "La firma F3 fue rechazada: el distributivo volvió a «Nombre de EN_REVISION_VRA»");
        assertThat(mensaje.detalle())
                .isEqualTo("Rechazó el paso F3 (RECTOR): «" + observacion + "»");
        assertThat(mensaje.detalle()).hasSizeLessThanOrEqualTo(1000);
        assertThat(mensaje.titulo()).hasSizeLessThanOrEqualTo(200);
        assertThat(mensaje.enlace()).isEqualTo("/consolidado");
    }

    @Test
    void dadaUnaVersionNueva_cuandoSeNotifica_entoncesDiceSuNumeroYLaAnulada() {
        Transicion nueva =
                transicion(
                        TipoDeTransicion.VERSION_NUEVA,
                        "EN_REVISION_VRA",
                        "EN_REVISION_VRA",
                        Optional.empty());

        MensajeDeTransicion mensaje =
                MensajesDeTransicion.de(proceso(etapas(false)), nueva, Map.of()).getFirst();

        assertThat(mensaje.destino().rol()).isEqualTo("VICERRECTOR");
        assertThat(mensaje.titulo()).isEqualTo("Se creó la versión 2 del distributivo");
        assertThat(mensaje.detalle())
                .isEqualTo("La versión 1 quedó anulada por el rechazo en firma.");
        assertThat(mensaje.enlace()).isEqualTo("/consolidado");
    }

    @Test
    void dadosLosNombresDeLasCarreras_cuandoSeNotifica_entoncesLosTitulosNombranLaCarrera() {
        Map<String, String> nombres = Map.of("DS", "DESARROLLO DE SOFTWARE");
        Transicion envio =
                transicion(
                        TipoDeTransicion.ENVIO_PARCIAL,
                        "BORRADOR_CARRERA",
                        "BORRADOR_CARRERA",
                        Optional.of("DS"));
        Transicion devolucion =
                new Transicion(
                        TipoDeTransicion.DEVOLUCION,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.empty(),
                        2,
                        HASH,
                        "consolidacion.demo",
                        AHORA,
                        Map.of(
                                "DS", new Observacion("Falta el docente de Redes II"),
                                "TDS", new Observacion("Revisar las horas de tutoría")));

        assertThat(
                        MensajesDeTransicion.de(proceso(etapas(false)), envio, nombres)
                                .getFirst()
                                .titulo())
                .isEqualTo("La carrera DESARROLLO DE SOFTWARE envió su parcial");
        // Sin el nombre de una carrera, su código; el destino sigue siendo el código.
        assertThat(MensajesDeTransicion.de(proceso(etapas(false)), devolucion, nombres))
                .extracting(MensajeDeTransicion::titulo, mensaje -> mensaje.destino().carrera())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "El parcial de DESARROLLO DE SOFTWARE fue devuelto a «Nombre de"
                                        + " BORRADOR_CARRERA»",
                                Optional.of("DS")),
                        org.assertj.core.groups.Tuple.tuple(
                                "El parcial de TDS fue devuelto a «Nombre de BORRADOR_CARRERA»",
                                Optional.of("TDS")));
    }

    @Test
    void dadaUnaCarreraDeNombreLargo_cuandoSeDevuelve_entoncesElTituloNoPasaDeDoscientos() {
        // El nombre de una carrera llega a 150 caracteres y el de una etapa a 80.
        String nombre = "C".repeat(150);
        List<EtapaDelProceso> etapas =
                etapas(false).stream()
                        .map(
                                etapa ->
                                        new EtapaDelProceso(
                                                etapa.tipo(),
                                                etapa.activa(),
                                                "E".repeat(80),
                                                etapa.rol(),
                                                false))
                        .toList();
        Transicion devolucion =
                new Transicion(
                        TipoDeTransicion.DEVOLUCION,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.empty(),
                        2,
                        HASH,
                        "consolidacion.demo",
                        AHORA,
                        Map.of("DS", new Observacion("Falta el docente de Redes II")));

        String titulo =
                MensajesDeTransicion.de(proceso(etapas), devolucion, Map.of("DS", nombre))
                        .getFirst()
                        .titulo();

        assertThat(titulo).hasSize(200).startsWith("El parcial de CCC").contains("C… fue devuelto");
        assertThat(titulo).endsWith("«" + "E".repeat(80) + "»");
    }

    @Test
    void dadaCadaTransicion_cuandoSePideSuDestino_entoncesElAgregadoLoDa() {
        ProcesoAprobacion proceso = proceso(etapas(false));
        Transicion paso =
                transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        "EN_CONSOLIDACION",
                        "EN_REVISION_VRA",
                        Optional.empty());

        assertThat(proceso.destinoDe(paso))
                .containsExactly(new DestinoDeNotificacion("VICERRECTOR", Optional.empty()));
    }

    private static Transicion transicion(
            TipoDeTransicion tipo, String desde, String hacia, Optional<String> carrera) {
        return new Transicion(
                tipo,
                TipoDeEtapa.valueOf(desde),
                TipoDeEtapa.valueOf(hacia),
                carrera,
                2,
                HASH,
                "consolidacion.demo",
                AHORA);
    }

    private static ProcesoAprobacion proceso(List<EtapaDelProceso> etapas) {
        return ProcesoAprobacion.de(
                "2026-I",
                etapas,
                TipoDeEtapa.BORRADOR_CARRERA,
                AHORA,
                Map.of("DS", EstadoDeEnvio.PENDIENTE),
                List.of());
    }

    private static List<EtapaDelProceso> etapas(boolean conUnidades) {
        return etapas(conUnidades, MensajesDeTransicionTest::rolDe);
    }

    private static List<EtapaDelProceso> etapas(
            boolean conUnidades, Function<TipoDeEtapa, String> rol) {
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
                                        rol.apply(tipo),
                                        false))
                .toList();
    }

    // Como la plantilla (flujo-por-defecto.yaml).
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
