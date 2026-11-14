package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.usuarios.adaptadores.salida.RepositorioBitacoraJdbc;
import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La bitácora contra PostgreSQL real (F17-H01): guarda cada entrada tal como llegó, y nadie la
 * modifica ni la borra (FR-010, SC-005). Migra con un rol que no es superusuario y es dueño del
 * esquema, como el rol {@code identidad} del perfil consolidado.
 */
class RepositorioBitacoraIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String ROL = "identidad_prueba";
    private static final String CLAVE = "clave-de-prueba";

    private static JdbcClient comoProceso;
    private static JdbcClient comoSuperusuario;
    private static RepositorioBitacoraJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource superusuario =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        comoSuperusuario = JdbcClient.create(superusuario);
        comoSuperusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        comoSuperusuario.sql("create schema usuarios authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new UsuariosFlywayConfig().flywayUsuarios(proceso).migrate();
        comoProceso = JdbcClient.create(proceso);
        repositorio = new RepositorioBitacoraJdbc(comoProceso);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoUnaEntrada_cuandoSeRegistra_entoncesQuedaConSusCamposYSusJsonTalComoLlegaron() {
        EntradaBitacora entrada = entrada("2026-I", true, true);

        repositorio.registrar(entrada);

        Map<String, Object> fila =
                comoProceso
                        .sql(
                                "select servicio, accion, usuario, momento, periodo,"
                                        + " objeto::text as objeto, anterior->>'horas' as antes,"
                                        + " nuevo->>'horas' as despues, recibida_en"
                                        + " from usuarios.bitacora where id_evento = :id")
                        .param("id", entrada.idEvento())
                        .query()
                        .singleRow();
        assertThat(fila)
                .containsEntry("servicio", "generacion")
                .containsEntry("accion", "ACTIVIDAD_GUARDADA")
                .containsEntry("usuario", "coordinacion.demo")
                .containsEntry("periodo", "2026-I")
                .containsEntry("antes", "4")
                .containsEntry("despues", "6");
        assertThat(fila.get("objeto").toString()).contains("\"actividad\": \"TUTORIAS\"");
        assertThat(((java.sql.Timestamp) fila.get("momento")).toInstant())
                .isEqualTo(Instant.parse("2026-11-06T15:00:00Z"));
        assertThat(fila.get("recibida_en")).isNotNull();
    }

    // F13-H02: Firma también registra en la bitácora.
    @Test
    void dadaUnaEntradaDeFirma_cuandoSeRegistra_entoncesLaBaseLaAdmite() {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("usuario", "sistema");
        contenido.put("accion", "DOCUMENTO_FIRMABLE_GENERADO");
        contenido.put("momento", "2026-11-11T15:00:00Z");
        contenido.put("periodo", "2026-I");
        contenido.put("objeto", Map.of("periodo", "2026-I", "version", 1));
        contenido.put("nuevo", Map.of("estado", "GENERADO"));
        EntradaBitacora entrada = EntradaBitacora.de(UUID.randomUUID(), "firma", contenido);

        repositorio.registrar(entrada);

        assertThat(
                        comoProceso
                                .sql("select servicio from usuarios.bitacora where id_evento = :id")
                                .param("id", entrada.idEvento())
                                .query(String.class)
                                .single())
                .isEqualTo("firma");
    }

    @Test
    void dadoUnaEntradaSinPeriodoNiAnterior_cuandoSeRegistra_entoncesQuedanNulos() {
        EntradaBitacora entrada = entrada(null, false, true);

        repositorio.registrar(entrada);

        Map<String, Object> fila =
                comoProceso
                        .sql(
                                "select periodo, anterior, nuevo is not null as con_nuevo"
                                        + " from usuarios.bitacora where id_evento = :id")
                        .param("id", entrada.idEvento())
                        .query()
                        .singleRow();
        assertThat(fila.get("periodo")).isNull();
        assertThat(fila.get("anterior")).isNull();
        assertThat(fila).containsEntry("con_nuevo", true);
    }

    @Test
    void dadoUnaEntrada_cuandoElRolDelProcesoIntentaModificarlaBorrarlaOVaciarla_entoncesFalla() {
        EntradaBitacora entrada = entrada("2026-I", true, true);
        repositorio.registrar(entrada);

        for (String sentencia :
                List.of(
                        "update usuarios.bitacora set usuario = 'otro' where id_evento = :id",
                        "delete from usuarios.bitacora where id_evento = :id")) {
            assertThatThrownBy(
                            () ->
                                    comoProceso
                                            .sql(sentencia)
                                            .param("id", entrada.idEvento())
                                            .update())
                    .as(sentencia)
                    .isInstanceOf(DataAccessException.class);
        }
        assertThatThrownBy(() -> comoProceso.sql("truncate usuarios.bitacora").update())
                .isInstanceOf(DataAccessException.class);
        assertThat(usuarioDe(entrada)).isEqualTo("coordinacion.demo");
    }

    @Test
    void
            dadoUnaEntrada_cuandoUnSuperusuarioIntentaModificarlaOBorrarla_entoncesElDisparadorLoRechaza() {
        EntradaBitacora entrada = entrada("2026-I", true, true);
        repositorio.registrar(entrada);

        assertThatThrownBy(
                        () ->
                                comoSuperusuario
                                        .sql(
                                                "update usuarios.bitacora set usuario = 'otro'"
                                                        + " where id_evento = :id")
                                        .param("id", entrada.idEvento())
                                        .update())
                .rootCause()
                .hasMessageContaining("La bitácora no se puede modificar ni borrar.");
        assertThatThrownBy(
                        () ->
                                comoSuperusuario
                                        .sql("delete from usuarios.bitacora where id_evento = :id")
                                        .param("id", entrada.idEvento())
                                        .update())
                .rootCause()
                .hasMessageContaining("La bitácora no se puede modificar ni borrar.");
        assertThatThrownBy(() -> comoSuperusuario.sql("truncate usuarios.bitacora").update())
                .rootCause()
                .hasMessageContaining("La bitácora no se puede modificar ni borrar.");
        assertThat(usuarioDe(entrada)).isEqualTo("coordinacion.demo");
    }

    @Test
    void dadoElMismoIdEvento_cuandoSeRegistraDosVeces_entoncesLaSegundaFalla() {
        EntradaBitacora entrada = entrada("2026-I", true, true);
        repositorio.registrar(entrada);

        assertThatThrownBy(() -> repositorio.registrar(entrada))
                .isInstanceOf(DataAccessException.class);
    }

    private static String usuarioDe(EntradaBitacora entrada) {
        return comoProceso
                .sql("select usuario from usuarios.bitacora where id_evento = :id")
                .param("id", entrada.idEvento())
                .query(String.class)
                .single();
    }

    private static EntradaBitacora entrada(String periodo, boolean anterior, boolean nuevo) {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("usuario", "coordinacion.demo");
        contenido.put("accion", "ACTIVIDAD_GUARDADA");
        contenido.put("momento", "2026-11-06T15:00:00Z");
        if (periodo != null) {
            contenido.put("periodo", periodo);
        }
        contenido.put("objeto", Map.of("periodo", "2026-I", "actividad", "TUTORIAS"));
        if (anterior) {
            contenido.put("anterior", Map.of("horas", "4"));
        }
        if (nuevo) {
            contenido.put("nuevo", Map.of("horas", "6"));
        }
        return EntradaBitacora.de(UUID.randomUUID(), "generacion", contenido);
    }
}
