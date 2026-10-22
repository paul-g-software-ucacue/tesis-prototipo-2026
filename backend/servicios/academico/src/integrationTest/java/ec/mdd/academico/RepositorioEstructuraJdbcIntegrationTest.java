package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.adaptadores.salida.RepositorioEstructuraJdbc;
import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** La estructura orgánica se guarda y se lee igual, campo por campo (F03-H01, T008). */
class RepositorioEstructuraJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioEstructuraJdbc repositorio;

    private static final UnidadOrganica MATRIZ =
            UnidadOrganica.nueva(
                    "MATRIZ", "Sede matriz", TipoDeUnidad.SEDE_MATRIZ, null, null, null);
    private static final UnidadOrganica RECT =
            UnidadOrganica.nueva("RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null);
    private static final UnidadOrganica COMISION =
            UnidadOrganica.nueva(
                    "COM-EVAL",
                    "Comisión de Evaluación",
                    TipoDeUnidad.COMISION,
                    "RECT",
                    null,
                    true);

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioEstructuraJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        // Un solo DELETE: la clave foránea del padre se comprueba al final de la sentencia.
        jdbc.sql("DELETE FROM academico.unidad_organica").update();
    }

    @Test
    void dadasUnidadesNuevas_cuandoSeRegistran_entoncesSeLeenIgualConQuienYCuando() {
        assertThat(repositorio.registrar(MATRIZ, "admin.demo", AHORA)).isTrue();
        assertThat(repositorio.registrar(RECT, "admin.demo", AHORA)).isTrue();
        assertThat(repositorio.registrar(COMISION, "admin.demo", AHORA)).isTrue();

        assertThat(repositorio.todas()).containsExactly(COMISION, MATRIZ, RECT);
        assertThat(
                        jdbc.sql(
                                        "SELECT registrada_por || ' ' || (registrada_en = :ahora)"
                                                + " FROM academico.unidad_organica"
                                                + " WHERE codigo = 'RECT'")
                                .param("ahora", Timestamp.from(AHORA))
                                .query(String.class)
                                .single())
                .isEqualTo("admin.demo true");
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entoncesNoGuardaNada() {
        repositorio.registrar(MATRIZ, "admin.demo", AHORA);

        UnidadOrganica repetida =
                UnidadOrganica.nueva("MATRIZ", "Otra", TipoDeUnidad.SEDE_MATRIZ, null, null, null);
        assertThat(repositorio.registrar(repetida, "otro", AHORA)).isFalse();
        assertThat(repositorio.todas()).containsExactly(MATRIZ);
    }

    @Test
    void dadaUnaUnidadCorregidaEInactivada_cuandoSeGuarda_entoncesSeLeeConSusCambios() {
        repositorio.registrar(MATRIZ, "admin.demo", AHORA);
        repositorio.registrar(RECT, "admin.demo", AHORA);
        repositorio.registrar(COMISION, "admin.demo", AHORA);
        UnidadOrganica corregida =
                COMISION.corregida("Comisión de Evaluación Interna", "MATRIZ", null, false)
                        .unidad()
                        .conEstado(false);
        UnidadOrganica rectorSinGestion =
                RECT.corregida("Rectorado", "MATRIZ", null, null).unidad();

        repositorio.guardar(corregida);
        repositorio.guardar(rectorSinGestion);

        assertThat(repositorio.todas()).containsExactly(corregida, MATRIZ, rectorSinGestion);
    }

    @Test
    void dadoElCatalogo_cuandoSeLee_entoncesTraeLas21PorCodigo() {
        assertThat(repositorio.catalogo())
                .hasSize(21)
                .startsWith(new GestionAutorizada("G01", "Rector"))
                .endsWith(new GestionAutorizada("G21", "Gestor Unidad de Estadísticas"));
    }

    @Test
    void dadaLaEstructuraBloqueada_cuandoOtraTransaccionLaBloquea_entoncesEsperaHastaQueTermine()
            throws Exception {
        TransactionTemplate transaccion =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CompletableFuture<Void> liberar = new CompletableFuture<>();
        CompletableFuture<Void> primera =
                CompletableFuture.runAsync(
                        () ->
                                transaccion.executeWithoutResult(
                                        estado -> {
                                            repositorio.bloquear();
                                            liberar.join();
                                        }));
        Thread.sleep(500);

        CompletableFuture<Void> segunda =
                CompletableFuture.runAsync(
                        () -> transaccion.executeWithoutResult(estado -> repositorio.bloquear()));

        assertThat(esperaMasDe(segunda, 500)).isTrue();
        liberar.complete(null);
        primera.get(5, TimeUnit.SECONDS);
        segunda.get(5, TimeUnit.SECONDS);
    }

    private static boolean esperaMasDe(CompletableFuture<Void> tarea, long milisegundos)
            throws Exception {
        try {
            tarea.get(milisegundos, TimeUnit.MILLISECONDS);
            return false;
        } catch (TimeoutException esperada) {
            return true;
        }
    }
}
