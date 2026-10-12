package ec.mdd.cargas;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.infraestructura.CargasFlywayConfig;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Comprueba, sin base de datos, que el Flyway del servicio solo apunta a su esquema y a su carpeta
 * de migraciones (ADR-013, contrato de persistencia).
 */
class CargasFlywayConfigTest {

    // Nunca se conecta: configurar Flyway no abre conexiones.
    private final Flyway flyway =
            new CargasFlywayConfig()
                    .flywayCargas(new DriverManagerDataSource("jdbc:postgresql://nunca:5432/mdd"));

    @Test
    void dadoElFlywayDelServicio_cuandoSeConfigura_entoncesSoloUsaSuEsquema() {
        assertThat(flyway.getConfiguration().getSchemas()).containsExactly("cargas");
    }

    @Test
    void dadoElFlywayDelServicio_cuandoSeConfigura_entoncesSoloLeeSuCarpeta() {
        assertThat(Arrays.stream(flyway.getConfiguration().getLocations()).map(Location::toString))
                .containsExactly("classpath:db/migration/cargas");
    }
}
