package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * El registro de cada descarga en el proceso salida, con PostgreSQL real (F20-H01, F20-H02, D-115,
 * D-117): el evento de la bitácora queda en {@code eventos_salida.event_publication} dentro de la
 * transacción de la descarga, y no queda si la transacción se revierte. Identidad no está: la
 * entrega queda pendiente y el reenvío la repetirá (ADR-018).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class RegistroEnBitacoraIntegrationTest {

    @Autowired private RegistroEnBitacora registro;
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
    void dadaUnaDescargaDelGeneral_cuandoTermina_entoncesSuEventoQuedaSinDocente() {
        String periodo = periodo();

        transaccion.ejecutar(
                () -> {
                    registro.registrar(
                            DescargaDeReporte.general(
                                    "rectorado.demo", Instant.now(), periodo, 1, Formato.PDF));
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
                .contains("REPORTE_GENERAL_DESCARGADO")
                .contains("PDF")
                .doesNotContain("identificacion");
    }

    @Test
    void dadoUnCsvParaSigaConfirmado_cuandoTermina_entoncesSuEventoQuedaSinDatosPersonales() {
        String periodo = periodo();

        transaccion.ejecutar(
                () -> {
                    registro.registrar(
                            new CsvGenerado("secretaria.demo", Instant.now(), periodo, 1, 0));
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
                .contains("CSV_SIGA_GENERADO")
                .contains("layout")
                .doesNotContain("identificacion");
    }

    @Test
    void dadoUnLayoutCargado_cuandoTermina_entoncesSuEventoQuedaConSuHuella() {
        String huella = java.util.HexFormat.of().toHexDigits(System.nanoTime()) + "a".repeat(48);

        transaccion.ejecutar(
                () -> {
                    registro.registrar(
                            new LayoutCargado("secretaria.demo", Instant.now(), 1, 13, huella));
                    return null;
                });

        String evento =
                jdbc.queryForObject(
                        "SELECT serialized_event FROM eventos_salida.event_publication"
                                + " WHERE serialized_event LIKE ?",
                        String.class,
                        "%" + huella + "%");
        assertThat(evento).contains("LAYOUT_SIGA_CARGADO").contains("secretaria.demo");
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
        return DescargaDeReporte.individual(
                "rectorado.demo", Instant.now(), periodo, 1, "0109990010", Formato.CSV);
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
