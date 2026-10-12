package ec.mdd.roles.infraestructura;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Migraciones del servicio con su propio Flyway, limitado a su esquema (ADR-013).
 *
 * <p>El servicio solo migra en el esquema {@value #ESQUEMA} y solo lee las migraciones de {@code
 * db/migration/roles}; su registro de migraciones ({@code flyway_schema_history}) también queda en
 * ese esquema. El proceso que ensambla el servicio ejecuta la migración al arrancar. Con este bean,
 * la autoconfiguración de Flyway de Spring Boot no se aplica.
 *
 * <p>El nombre de la clase y el del bean son únicos en el proyecto, porque las configuraciones de
 * todos los servicios de un proceso se cargan juntas.
 */
@Configuration(proxyBeanMethods = false)
public class RolesFlywayConfig {

    static final String ESQUEMA = "roles";

    @Bean(initMethod = "migrate")
    public Flyway flywayRoles(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(ESQUEMA)
                .locations("classpath:db/migration/" + ESQUEMA)
                .load();
    }
}
