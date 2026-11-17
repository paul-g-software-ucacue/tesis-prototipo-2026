package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cada transición deja su {@code validacionocs.NotificacionesSolicitadas} en {@code
 * eventos_distributivo}, en la misma transacción (F16-H01, FR-005, FR-011 y FR-013): si se
 * revierte, no queda nada; si se confirma con {@code identidad} caída, no hay error y queda
 * pendiente para el reenvío.
 *
 * <p>La entrega a {@code identidad} falla aquí, porque no está: es lo que se comprueba. El reenvío
 * con {@code identidad} de vuelta lo recorre {@code deploy/probar_documento_firmable.sh}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class NotificacionesPendientesIntegrationTest {

    @Autowired private NotificacionDeTransiciones notificacion;
    @Autowired private Transaccion transaccionValidacionocs;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void conIdentidadCaida_cuandoSeConfirmaUnaTransicion_entoncesQuedaPendienteSinError()
            throws InterruptedException {
        String periodo = periodo();

        transaccionValidacionocs.ejecutar(
                () -> {
                    notificacion.notificar(proceso(periodo), List.of(paso()));
                    return periodo;
                });

        assertThat(publicaciones(periodo)).isOne();
        Instant limite = Instant.now().plusSeconds(15);
        while (!"FAILED".equals(estado(periodo)) && Instant.now().isBefore(limite)) {
            Thread.sleep(200);
        }
        assertThat(estado(periodo)).isEqualTo("FAILED");
    }

    @Test
    void unaTransicionRevertida_cuandoSeNotifica_entoncesNoQuedaNingunEvento() {
        String periodo = periodo();

        assertThatThrownBy(
                        () ->
                                transaccionValidacionocs.ejecutar(
                                        () -> {
                                            notificacion.notificar(
                                                    proceso(periodo), List.of(paso()));
                                            throw new IllegalStateException("se revierte");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(publicaciones(periodo)).isZero();
    }

    private static String periodo() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Transicion paso() {
        return new Transicion(
                TipoDeTransicion.PASO_DE_ETAPA,
                TipoDeEtapa.EN_CONSOLIDACION,
                TipoDeEtapa.EN_REVISION_VRA,
                Optional.empty(),
                1,
                "a".repeat(64),
                "consolidacion.demo",
                Instant.now());
    }

    private static ProcesoAprobacion proceso(String periodo) {
        return ProcesoAprobacion.de(
                periodo,
                Arrays.stream(TipoDeEtapa.values())
                        .map(
                                tipo ->
                                        new EtapaDelProceso(
                                                tipo,
                                                tipo != TipoDeEtapa.BORRADOR_UNIDADES
                                                        && tipo != TipoDeEtapa.REMITIDO_ZONAL,
                                                "Nombre de " + tipo,
                                                "VICERRECTOR",
                                                false))
                        .toList(),
                TipoDeEtapa.EN_REVISION_VRA,
                Instant.now(),
                Map.of("DS", EstadoDeEnvio.ENVIADO),
                List.of());
    }

    private String estado(String periodo) {
        return jdbc
                .queryForList(
                        "SELECT status FROM eventos_distributivo.event_publication"
                                + " WHERE serialized_event LIKE ?",
                        String.class,
                        "%" + periodo + "%")
                .stream()
                .findFirst()
                .orElse("SIN_PUBLICACION");
    }

    private int publicaciones(String periodo) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM eventos_distributivo.event_publication"
                        + " WHERE serialized_event LIKE ? AND serialized_event LIKE ?",
                Integer.class,
                "%validacionocs.NotificacionesSolicitadas%",
                "%" + periodo + "%");
    }
}
