package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El repositorio reconstruye la versión 1 con el agregado, así que la política de la migración
 * cumple todas sus invariantes (research §2).
 */
class RepositorioVersionesJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static RepositorioVersionesJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        repositorio = new RepositorioVersionesJdbc(JdbcClient.create(dataSource));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLaMigracion_cuandoSeListan_entoncesHayUnaVersionReconstruidaConSusInvariantes() {
        assertThat(repositorio.todas())
                .singleElement()
                .satisfies(
                        version -> {
                            assertThat(version.numero()).isEqualTo(1);
                            assertThat(version.vigenteDesde()).isEqualTo(LocalDate.of(2024, 3, 11));
                            assertThat(version.tipoIst()).isEqualTo(TipoIst.T2);
                            assertThat(version.tabla().filas()).hasSize(22);
                            assertThat(version.reglas()).hasSize(7);
                        });
    }

    @Test
    void dadoLaVersionUno_cuandoSeBusca_entoncesSusFilasVienenEnElOrdenDeLaPolitica() {
        VersionNormativa version = repositorio.buscar(1).orElseThrow();

        assertThat(version.tabla().filas().getFirst())
                .isEqualTo(
                        new FilaCargaHoraria(
                                null,
                                "DOCENTE_TC",
                                40,
                                22,
                                18,
                                24,
                                18,
                                0,
                                null,
                                Severidad.DURA,
                                "POL 6; RCE arts. 146 y 149",
                                "Por defecto 22 h clase + 18 otras de docencia (C-01)"));
        assertThat(version.tabla().filas().getLast().perfil()).isEqualTo("CUIDADOR_SUSTITUTO");
        assertThat(version.tabla().filaPara("GESTOR", TipoIst.T2).orElseThrow().horasGestion())
                .isEqualTo(10);
        assertThat(version.regla("RN-01").orElseThrow().severidad()).isNull();
        assertThat(version.regla("RN-02").orElseThrow().parametro("horas_maximas_docente"))
                .hasValueSatisfying(
                        valor -> assertThat(valor).isEqualByComparingTo(BigDecimal.valueOf(40)));
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeBusca_entoncesVacio() {
        assertThat(repositorio.buscar(2)).isEmpty();
    }
}
