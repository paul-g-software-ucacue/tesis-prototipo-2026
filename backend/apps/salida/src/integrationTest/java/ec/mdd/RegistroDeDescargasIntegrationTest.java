package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.puertos.salida.RegistroDeDescargas;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * El registro de cada descarga en el proceso salida, con PostgreSQL real (F20-H01, D-115): el
 * evento de la bitácora queda en {@code eventos_salida.event_publication} dentro de la transacción
 * de la descarga, y no queda si la transacción se revierte. Identidad no está: la entrega queda
 * pendiente y el reenvío la repetirá (ADR-018).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class RegistroDeDescargasIntegrationTest {

    @Autowired private RegistroDeDescargas registro;
    @Autowired private Transaccion transaccion;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void dadaUnaDescargaConfirmada_cuandoTermina_entoncesSuEventoQuedaEnElRegistro() {
        String periodo = periodo();

        transaccion.ejecutar(
                () -> {
                    registro.registrar(descarga(periodo));
                    return null;
                });

        assertThat(eventos(periodo)).isOne();
        String evento =
                jdbc.queryForObject(
                        "SELECT serialized_event FROM eventos_salida.event_publication"
                                + " WHERE serialized_event LIKE ?",
                        String.class,
                        "%" + periodo + "%");
        assertThat(evento)
                .contains("reporting.EntradaBitacora")
                .contains("REPORTE_INDIVIDUAL_DESCARGADO")
                .contains("0109990010")
                .contains("CSV")
                .doesNotContain("PEREZ");
    }

    @Test
    void dadaUnaDescargaRevertida_cuandoTermina_entoncesNoQuedaNingunEvento() {
        String periodo = periodo();

        assertThatThrownBy(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            registro.registrar(descarga(periodo));
                                            throw new IllegalStateException("se revierte");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(eventos(periodo)).isZero();
    }

    private Integer eventos(String periodo) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM eventos_salida.event_publication"
                        + " WHERE serialized_event LIKE ?",
                Integer.class,
                "%" + periodo + "%");
    }

    private static DescargaDeReporte descarga(String periodo) {
        return new DescargaDeReporte(
                "rectorado.demo", Instant.now(), periodo, 1, "0109990010", Formato.CSV);
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
