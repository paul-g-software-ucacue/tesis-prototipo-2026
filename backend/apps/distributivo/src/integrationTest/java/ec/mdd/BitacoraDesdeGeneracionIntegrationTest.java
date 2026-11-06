package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cada cambio de Generación deja su {@code generacion.EntradaBitacora} en {@code
 * eventos_distributivo}, en la misma transacción (F17-H01, FR-005): si el cambio se revierte, no
 * queda ni la fila local ni el evento.
 *
 * <p>La entrega a {@code identidad} falla aquí, porque no está; la publicación queda pendiente, que
 * es justamente lo que se comprueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class BitacoraDesdeGeneracionIntegrationTest {

    @Autowired private RegistroDeCambios registro;
    @Autowired private Transaccion transaccion;
    @Autowired private JdbcTemplate jdbc;

    private static String usuario() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static CambioDeDistributivo cambio(String usuario) {
        return new CambioDeDistributivo(
                Instant.now(),
                usuario,
                CambioDeDistributivo.Accion.ACTIVIDAD_GUARDADA,
                Map.of("periodo", "2026-I", "actividad", "TUTORIAS", "horas", "6"),
                Map.of("horas", "4"),
                Map.of("horas", "6"));
    }

    @Test
    void unCambioConfirmado_cuandoSeRegistra_entoncesQuedanLaFilaLocalYSuEvento() {
        String usuario = usuario();

        transaccion.ejecutar(
                () -> {
                    registro.registrar(cambio(usuario));
                    return usuario;
                });

        assertThat(filasLocales(usuario)).isOne();
        assertThat(publicaciones(usuario)).isOne();
    }

    @Test
    void unCambioRevertido_cuandoSeRegistra_entoncesNoQuedaNiLaFilaNiElEvento() {
        String usuario = usuario();

        assertThatThrownBy(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            registro.registrar(cambio(usuario));
                                            throw new IllegalStateException("se revierte");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(filasLocales(usuario)).isZero();
        assertThat(publicaciones(usuario)).isZero();
    }

    private int filasLocales(String usuario) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM generacion.registro_cambio WHERE usuario = ?",
                Integer.class,
                usuario);
    }

    private int publicaciones(String usuario) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM eventos_distributivo.event_publication"
                        + " WHERE serialized_event LIKE ? AND serialized_event LIKE ?",
                Integer.class,
                "%generacion.EntradaBitacora%",
                "%" + usuario + "%");
    }
}
