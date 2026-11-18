package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.puertos.salida.FirmaPendienteSolicitada;
import ec.mdd.firma.puertos.salida.NotificacionDeFirmaPendiente;
import ec.mdd.firma.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La notificación de firma pendiente deja su {@code firma.NotificacionesSolicitadas} en {@code
 * eventos_distributivo}, en la transacción de la firma (F16-H02, FR-007, FR-008 y FR-010): si se
 * revierte, no queda nada; si se confirma con {@code identidad} caído, no hay error y queda
 * pendiente para el reenvío.
 *
 * <p>La entrega a {@code identidad} falla aquí, porque no está: es lo que se comprueba. El reenvío
 * con {@code identidad} de vuelta lo recorre {@code deploy/probar_documento_firmable.sh}, paso 19.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class FirmaPendienteSinIdentidadIntegrationTest {

    @Autowired private NotificacionDeFirmaPendiente notificacion;
    @Autowired private Transaccion transaccionFirma;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void conIdentidadCaido_cuandoSeConfirmaUnaFirma_entoncesQuedaPendienteSinError()
            throws InterruptedException {
        String periodo = periodo();

        transaccionFirma.ejecutar(
                () -> {
                    notificacion.notificar(solicitud(periodo));
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
    void unaFirmaRevertida_cuandoSeNotifica_entoncesNoQuedaNingunEvento() {
        String periodo = periodo();

        assertThatThrownBy(
                        () ->
                                transaccionFirma.ejecutar(
                                        () -> {
                                            notificacion.notificar(solicitud(periodo));
                                            throw new IllegalStateException("se revierte");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(publicaciones(periodo)).isZero();
    }

    private static String periodo() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static FirmaPendienteSolicitada solicitud(String periodo) {
        return new FirmaPendienteSolicitada(
                periodo,
                1,
                "docente.demo",
                Instant.now(),
                "Revisado por",
                FirmanteEsperado.porIdentificacion(2, "VICERRECTOR", "0109990028", "Vicerrector"),
                "Elaborado por",
                "Docente (demo)");
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
                "%firma.NotificacionesSolicitadas%",
                "%" + periodo + "%");
    }
}
