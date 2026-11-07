package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.entrada.DatosDeRol;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Un cambio de Roles llega a la bitácora de Usuarios dentro de {@code identidad}, sin HTTP
 * (F17-H01, ADR-024): el evento queda en {@code eventos_identidad} con la transacción de Roles, y
 * el oyente lo entrega después al receptor local, que guarda la entrada en la suya.
 *
 * <p>La base es un PostgreSQL de Testcontainers que se descarta al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ErrorTecnicoIntegrationTest.Contenedores.class)
class BitacoraDesdeRolesIntegrationTest {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    @Autowired private GestionarRoles gestionar;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void unRolCreado_cuandoSeConfirma_entoncesSuEntradaLlegaALaBitacoraUnaVez() throws Exception {
        String nombre = "Revisor " + UUID.randomUUID().toString().substring(0, 8);

        String identificador =
                gestionar
                        .crear(
                                new DatosDeRol(
                                        nombre,
                                        "Revisa docentes.",
                                        List.of(
                                                new Permiso(
                                                        "DOCENTES",
                                                        TipoDePermiso.LECTURA,
                                                        Alcance.TODO))),
                                "admin.demo")
                        .rol()
                        .identificador();

        Map<String, Object> entrada = esperarEntrada(identificador);
        assertThat(entrada)
                .containsEntry("servicio", "roles")
                .containsEntry("accion", "CREAR_ROL")
                .containsEntry("usuario", "admin.demo");
        assertThat((String) entrada.get("nuevo")).contains(nombre);
        assertThat(entrada.get("anterior")).isNull();
        // La publicación se completa: no queda nada que reenviar.
        Instant limite = Instant.now().plus(ESPERA);
        while (completadas(identificador) == 0 && Instant.now().isBefore(limite)) {
            Thread.sleep(200);
        }
        assertThat(completadas(identificador)).isOne();
    }

    private int completadas(String identificador) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM eventos_identidad.event_publication"
                        + " WHERE serialized_event LIKE ? AND completion_date IS NOT NULL",
                Integer.class,
                "%" + identificador + "%");
    }

    private Map<String, Object> esperarEntrada(String identificador) throws InterruptedException {
        Instant limite = Instant.now().plus(ESPERA);
        while (Instant.now().isBefore(limite)) {
            List<Map<String, Object>> filas =
                    jdbc.queryForList(
                            "SELECT servicio, accion, usuario, anterior::text AS anterior,"
                                    + " nuevo::text AS nuevo FROM usuarios.bitacora"
                                    + " WHERE objeto ->> 'identificador' = ?",
                            identificador);
            if (!filas.isEmpty()) {
                assertThat(filas).hasSize(1);
                return filas.getFirst();
            }
            Thread.sleep(200);
        }
        throw new AssertionError("La entrada de " + identificador + " no llegó a la bitácora.");
    }
}
