package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.adaptadores.salida.RepositorioConsolidacionJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.aplicacion.ParcialesDelDistributivoServicio;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Devolver parciales contra PostgreSQL real (F12-H05, contracts/generacion.md): todo o nada, el
 * parcial devuelto vuelve a editarse y devolver no deja el consolidado desactualizado.
 */
class DevolucionDeParcialesIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-01T15:00:00Z");

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc repositorio;
    private static RepositorioConsolidacionJdbc consolidacion;
    private static ParcialesDelDistributivoServicio servicio;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioDistributivosJdbc(jdbc);
        consolidacion = new RepositorioConsolidacionJdbc(jdbc);
        // Devolver no usa el consolidado.
        servicio =
                new ParcialesDelDistributivoServicio(
                        repositorio,
                        new TransaccionSpring(
                                new TransactionTemplate(
                                        new DataSourceTransactionManager(dataSource))),
                        null);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadosDosParcialesEnviados_cuandoSeDevuelven_entoncesQuedanDevueltosYEditables() {
        String periodo = enviado("DS", "TDS", "EL");

        int numero = servicio.marcarParcialesDevueltos(periodo, List.of("DS", "TDS"));

        assertThat(numero).isEqualTo(1);
        assertThat(estados(periodo))
                .containsEntry("DS", EstadoDelParcial.DEVUELTO)
                .containsEntry("TDS", EstadoDelParcial.DEVUELTO)
                .containsEntry("EL", EstadoDelParcial.ENVIADO);
        VersionDistributivo version = repositorio.vigenteDe(periodo).orElseThrow();
        assertThat(version.parcialDe("DS").orElseThrow().editable()).isTrue();
        assertThat(version.parcialDe("EL").orElseThrow().editable()).isFalse();
        // research §7: devolver no es un cambio de asignaciones; el consolidado sigue al día.
        assertThat(consolidacion.ultimoCambioDeAsignaciones(periodo)).isEmpty();
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeDevuelve_entoncesNingunaCambia() {
        String periodo = enviado("DS", "TDS");

        assertThatThrownBy(() -> servicio.marcarParcialesDevueltos(periodo, List.of("DS", "OTRA")))
                .isInstanceOf(ParcialNoEncontrado.class);

        assertThat(estados(periodo).values()).containsOnly(EstadoDelParcial.ENVIADO);
    }

    @Test
    void dadoUnParcialDevuelto_cuandoSeReenvia_entoncesVuelveAEnviado() {
        String periodo = enviado("DS");
        servicio.marcarParcialesDevueltos(periodo, List.of("DS"));

        servicio.marcarParcialEnviado(periodo, "DS");

        assertThat(estados(periodo)).containsEntry("DS", EstadoDelParcial.ENVIADO);
    }

    // Un distributivo propio con los parciales de esas carreras enviados.
    private static String enviado(String... carreras) {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        repositorio.crear(
                periodo,
                VersionDistributivo.primera(UUID.randomUUID(), List.of(carreras)),
                "sistema",
                MOMENTO);
        for (String carrera : carreras) {
            servicio.marcarParcialEnviado(periodo, carrera);
        }
        return periodo;
    }

    private static Map<String, EstadoDelParcial> estados(String periodo) {
        return repositorio.vigenteDe(periodo).orElseThrow().parciales().stream()
                .collect(Collectors.toMap(ParcialCarrera::carrera, ParcialCarrera::estado));
    }
}
