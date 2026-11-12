package ec.mdd.firma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.infraestructura.FirmaFlywayConfig;
import java.sql.Timestamp;
import java.time.Instant;
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
 * Las tablas del documento firmable (F13-H02, data-model.md), migradas por un rol del proceso que
 * no es superusuario, como en producción: sus restricciones y que el documento registrado no se
 * cambia ni se borra (principio VII).
 */
class DocumentoFirmableEsquemaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String ROL = "distributivo_prueba";
    private static final String CLAVE = "clave-de-prueba";
    private static final String HASH = "a".repeat(64);
    private static final String SHA = "b".repeat(64);
    private static final String CADENA =
            "[{\"orden\":1,\"etiqueta\":\"Elaborado\",\"rol\":\"RESPONSABLE_CONSOLIDACION\"}]";

    private static JdbcClient comoProceso;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource superusuario =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcClient comoSuperusuario = JdbcClient.create(superusuario);
        comoSuperusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        comoSuperusuario.sql("create schema firma authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new FirmaFlywayConfig().flywayFirma(proceso).migrate();
        comoProceso = JdbcClient.create(proceso);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadaUnaGeneracionYSuDocumento_cuandoSeRegistran_entoncesQuedanUnoPorVersion() {
        generacion("P1", 1, "GENERADA", null);
        documento("P1", 1, UUID.randomUUID());

        assertThat(
                        comoProceso
                                .sql(
                                        "select count(*) from firma.documento_firmable where periodo = 'P1'")
                                .query(Integer.class)
                                .single())
                .isEqualTo(1);
        assertThatThrownBy(() -> documento("P1", 1, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnDocumentoRegistrado_cuandoElProcesoLoCambiaOLoBorra_entoncesSeRechaza() {
        generacion("P2", 1, "GENERADA", null);
        documento("P2", 1, UUID.randomUUID());

        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update firma.documento_firmable set archivo ="
                                                        + " 'otro' where periodo = 'P2'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "delete from firma.documento_firmable where periodo = 'P2'")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaGeneracionFallida_cuandoNoTieneMotivo_entoncesSeRechaza() {
        assertThatThrownBy(() -> generacion("P3", 1, "FALLIDA", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> generacion("P3", 1, "GENERADA", "No debería tener motivo."))
                .isInstanceOf(DataAccessException.class);
        generacion("P3", 1, "FALLIDA", "Falta el nombre del Rector.");
    }

    @Test
    void dadoUnEstadoOUnHashInvalidos_cuandoSeGuardan_entoncesSeRechazan() {
        assertThatThrownBy(() -> generacion("P4", 1, "OTRO", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "insert into firma.generacion_documento (periodo,"
                                                        + " version, tipo, hash_contenido, cadena,"
                                                        + " rol_de_etapa, estado, pedida_por,"
                                                        + " actualizada_en) values ('P4', 1,"
                                                        + " 'CONSOLIDADO', 'XYZ', cast(:cadena as"
                                                        + " jsonb), 'RESPONSABLE_CONSOLIDACION',"
                                                        + " 'EN_CURSO', 'sistema', now())")
                                        .param("cadena", CADENA)
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    private static void generacion(String periodo, int version, String estado, String motivo) {
        comoProceso
                .sql(
                        "insert into firma.generacion_documento (periodo, version, tipo,"
                                + " hash_contenido, cadena, rol_de_etapa, estado, motivo,"
                                + " pedida_por, actualizada_en) values (:periodo, :version,"
                                + " 'CONSOLIDADO', :hash, cast(:cadena as jsonb),"
                                + " 'RESPONSABLE_CONSOLIDACION', :estado, :motivo, 'sistema',"
                                + " :momento)")
                .param("periodo", periodo)
                .param("version", version)
                .param("hash", HASH)
                .param("cadena", CADENA)
                .param("estado", estado)
                .param("motivo", motivo)
                .param("momento", Timestamp.from(Instant.now()))
                .update();
    }

    private static void documento(String periodo, int version, UUID id) {
        comoProceso
                .sql(
                        "insert into firma.documento_firmable (id, periodo, version, tipo,"
                                + " hash_contenido, sha256_original, archivo, tamano, generado_por,"
                                + " generado_en) values (:id, :periodo, :version, 'CONSOLIDADO',"
                                + " :hash, :sha, :archivo, 1024, 'sistema', :momento)")
                .param("id", id)
                .param("periodo", periodo)
                .param("version", version)
                .param("hash", HASH)
                .param("sha", SHA)
                .param("archivo", "documentos/" + SHA + ".pdf")
                .param("momento", Timestamp.from(Instant.now()))
                .update();
    }
}
