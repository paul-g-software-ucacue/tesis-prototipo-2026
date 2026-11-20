package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * El aviso de un plazo vencido (F12-H03, D-114): el periodo, la etapa, la llegada, la fecha límite
 * y los días de atraso, con un enlace por destino.
 */
class MensajesDePlazoTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    // 2026-11-04 a las 10:00 de Ecuador; el plazo de 5 días vence el 2026-11-09.
    private static final Instant LLEGADA = Instant.parse("2026-11-04T15:00:00Z");
    private static final Instant DETECTADO = Instant.parse("2026-11-12T12:00:00Z");

    @Test
    void dadaConsolidacionVencida_cuandoSeArmanLosMensajes_entoncesUnoPorDestinoConSuEnlace() {
        ProcesoAprobacion proceso = en(TipoDeEtapa.EN_CONSOLIDACION, Map.of());

        List<MensajeDeTransicion> mensajes =
                MensajesDePlazo.de(
                        proceso, vencimiento(TipoDeEtapa.EN_CONSOLIDACION, 5, 3), Map.of());

        assertThat(mensajes)
                .extracting(MensajeDeTransicion::destino, MensajeDeTransicion::enlace)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                new DestinoDeNotificacion(
                                        "RESPONSABLE_CONSOLIDACION", Optional.empty()),
                                "/consolidado"),
                        org.assertj.core.groups.Tuple.tuple(
                                new DestinoDeNotificacion("VICERRECTOR", Optional.empty()),
                                "/consolidado"));
        assertThat(mensajes)
                .allSatisfy(
                        mensaje -> {
                            assertThat(mensaje.titulo())
                                    .isEqualTo("Venció el plazo de «Nombre de EN_CONSOLIDACION»");
                            assertThat(mensaje.detalle())
                                    .isEqualTo(
                                            "El distributivo de 2026-I está en «Nombre de"
                                                    + " EN_CONSOLIDACION» desde el 04/11/2026. Su"
                                                    + " plazo de 5 días venció el 09/11/2026; lleva"
                                                    + " 3 días de atraso.");
                        });
    }

    @Test
    void
            dadaResolucionVencida_cuandoSeArman_entoncesLaSecretariaASuPantallaYElVicerrectorAlConsolidado() {
        ProcesoAprobacion proceso = en(TipoDeEtapa.EN_OCS, Map.of());

        assertThat(MensajesDePlazo.de(proceso, vencimiento(TipoDeEtapa.EN_OCS, 10, 1), Map.of()))
                .extracting(mensaje -> mensaje.destino().rol(), MensajeDeTransicion::enlace)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "SECRETARIA_GENERAL", "/resolucion-ocs"),
                        org.assertj.core.groups.Tuple.tuple("VICERRECTOR", "/consolidado"));
    }

    @Test
    void dadoBorradorCarreraVencido_cuandoSeArman_entoncesCadaCarreraConSuNombre() {
        Map<String, EstadoDeEnvio> parciales = new TreeMap<>();
        parciales.put("DS", EstadoDeEnvio.ENVIADO);
        parciales.put("TDS", EstadoDeEnvio.PENDIENTE);
        parciales.put("SI", EstadoDeEnvio.DEVUELTO);
        ProcesoAprobacion proceso = en(TipoDeEtapa.BORRADOR_CARRERA, parciales);

        List<MensajeDeTransicion> mensajes =
                MensajesDePlazo.de(
                        proceso,
                        vencimiento(TipoDeEtapa.BORRADOR_CARRERA, 15, 1),
                        Map.of("TDS", "Tecnología en Desarrollo de Software"));

        assertThat(mensajes).hasSize(3);
        assertThat(mensajes.get(0).destino())
                .isEqualTo(new DestinoDeNotificacion("COORDINADOR_CARRERA", Optional.of("SI")));
        assertThat(mensajes.get(0).detalle())
                .contains("La carrera SI todavía no envió su parcial.");
        assertThat(mensajes.get(0).enlace()).isEqualTo("/oferta");
        assertThat(mensajes.get(1).detalle())
                .contains(
                        "La carrera Tecnología en Desarrollo de Software todavía no envió su"
                                + " parcial.");
        assertThat(mensajes.get(2).destino().rol()).isEqualTo("VICERRECTOR");
        assertThat(mensajes.get(2).detalle()).doesNotContain("La carrera");
        assertThat(mensajes.get(2).enlace()).isEqualTo("/consolidado");
    }

    @Test
    void dadoUnDiaDeAtraso_cuandoSeArma_entoncesDiceUnDiaEnSingular() {
        ProcesoAprobacion proceso = en(TipoDeEtapa.APROBADO, Map.of());

        assertThat(MensajesDePlazo.de(proceso, vencimiento(TipoDeEtapa.APROBADO, 1, 1), Map.of()))
                .first()
                .extracting(MensajeDeTransicion::detalle)
                .asString()
                .contains("Su plazo de 1 día venció el 05/11/2026; lleva 1 día de atraso.");
    }

    @Test
    void dadoUnNombreDeEtapaLargo_cuandoSeArma_entoncesElTituloCabeEnElContrato() {
        String largo = "E".repeat(250);
        List<EtapaDelProceso> etapas =
                etapas().stream()
                        .map(
                                etapa ->
                                        etapa.tipo() == TipoDeEtapa.EN_OCS
                                                ? new EtapaDelProceso(
                                                        etapa.tipo(),
                                                        true,
                                                        largo,
                                                        etapa.rol(),
                                                        etapa.admiteDevolucion())
                                                : etapa)
                        .toList();
        ProcesoAprobacion proceso =
                ProcesoAprobacion.de(
                        "2026-I", etapas, TipoDeEtapa.EN_OCS, CREADO, Map.of(), List.of());

        assertThat(MensajesDePlazo.de(proceso, vencimiento(TipoDeEtapa.EN_OCS, 10, 2), Map.of()))
                .allSatisfy(
                        mensaje -> {
                            assertThat(mensaje.titulo()).hasSizeLessThanOrEqualTo(200);
                            assertThat(mensaje.detalle()).hasSizeLessThanOrEqualTo(1000);
                        });
    }

    private static VencimientoDePlazo vencimiento(TipoDeEtapa etapa, int dias, int atraso) {
        return new VencimientoDePlazo("2026-I", Plazo.de(etapa, LLEGADA, dias), DETECTADO, atraso);
    }

    private static ProcesoAprobacion en(TipoDeEtapa etapa, Map<String, EstadoDeEnvio> parciales) {
        return ProcesoAprobacion.de("2026-I", etapas(), etapa, CREADO, parciales, List.of());
    }

    private static List<EtapaDelProceso> etapas() {
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
                                            case EN_REVISION_VRA, REMITIDO_ZONAL -> "VICERRECTOR";
                                            case EN_OCS, APROBADO, PUBLICADO ->
                                                    "SECRETARIA_GENERAL";
                                        },
                                        false))
                .toList();
    }
}
