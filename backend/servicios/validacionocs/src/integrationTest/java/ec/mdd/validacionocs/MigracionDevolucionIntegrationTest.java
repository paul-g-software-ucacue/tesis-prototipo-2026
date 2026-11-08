package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La migración V4 sobre un proceso que guardó F12-H02, sin {@code admiteDevolucion} en la copia de
 * sus etapas (F12-H05, research §4): queda con el valor de la plantilla para cada etapa.
 */
class MigracionDevolucionIntegrationTest {

    private static PostgreSQLContainer postgres;
    private static DataSource dataSource;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnProcesoDeF12H02_cuandoSeMigraAV4_entoncesSuCopiaAdmiteDevolucionComoLaPlantilla() {
        flyway("3").migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        jdbc.sql(
                        "INSERT INTO validacionocs.proceso_aprobacion (periodo, estado, etapas,"
                                + " distributivo_creado_en, creado_en) VALUES ('2026-I',"
                                + " 'EN_CONSOLIDACION', CAST(:etapas AS jsonb), now(), now())")
                .param("etapas", etapasComoF12H02())
                .update();

        flyway(null).migrate();

        Map<TipoDeEtapa, Boolean> admite =
                new RepositorioProcesoJdbc(jdbc)
                        .de("2026-I").orElseThrow().etapas().stream()
                                .collect(
                                        Collectors.toMap(
                                                EtapaDelProceso::tipo,
                                                EtapaDelProceso::admiteDevolucion));
        assertThat(admite)
                .containsEntry(TipoDeEtapa.BORRADOR_CARRERA, false)
                .containsEntry(TipoDeEtapa.BORRADOR_UNIDADES, false)
                .containsEntry(TipoDeEtapa.EN_CONSOLIDACION, true)
                .containsEntry(TipoDeEtapa.EN_REVISION_VRA, true)
                .containsEntry(TipoDeEtapa.CONGELADO_EN_FIRMA, true)
                .containsEntry(TipoDeEtapa.EN_OCS, true)
                .containsEntry(TipoDeEtapa.REMITIDO_ZONAL, false)
                .containsEntry(TipoDeEtapa.APROBADO, false)
                .containsEntry(TipoDeEtapa.PUBLICADO, false);
    }

    private static Flyway flyway(String hasta) {
        var configuracion =
                Flyway.configure()
                        .dataSource(dataSource)
                        .schemas("validacionocs")
                        .locations("classpath:db/migration/validacionocs");
        if (hasta != null) {
            configuracion = configuracion.target(hasta);
        }
        return configuracion.load();
    }

    // Las 9 etapas tal como las guardaba F12-H02: tipo, activa, nombre y rol.
    private static String etapasComoF12H02() {
        StringBuilder json = new StringBuilder("[");
        for (TipoDeEtapa tipo : TipoDeEtapa.values()) {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append("{\"tipo\":\"")
                    .append(tipo.name())
                    .append("\",\"activa\":true,\"nombre\":\"")
                    .append(tipo.name())
                    .append("\",\"rol\":\"VICERRECTOR\"}");
        }
        return json.append(']').toString();
    }
}
