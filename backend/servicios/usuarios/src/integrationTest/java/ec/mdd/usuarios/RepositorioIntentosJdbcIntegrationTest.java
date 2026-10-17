package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.adaptadores.salida.RepositorioIntentosJdbc;
import ec.mdd.usuarios.dominio.EstadoDeIntentos;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Los intentos fallidos contra PostgreSQL real (D-60): cuenta atómica, bloqueo y limpieza. */
class RepositorioIntentosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static RepositorioIntentosJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new UsuariosFlywayConfig().flywayUsuarios(dataSource).migrate();
        repositorio = new RepositorioIntentosJdbc(JdbcClient.create(dataSource));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoUnNombreSinIntentos_cuandoSeLee_entoncesNoTieneIntentos() {
        assertThat(repositorio.leer(nombre())).isEqualTo(EstadoDeIntentos.SIN_INTENTOS);
    }

    @Test
    void dadoTresFallos_cuandoSeSuman_entoncesLaCuentaSube() {
        String nombre = nombre();

        List<Integer> cuentas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            cuentas.add(repositorio.sumarFallo(nombre, AHORA));
        }

        assertThat(cuentas).containsExactly(1, 2, 3);
        assertThat(repositorio.leer(nombre)).isEqualTo(new EstadoDeIntentos(3, null));
    }

    @Test
    void dadoDiezFallosSimultaneos_cuandoSeSuman_entoncesCuentanTodos() throws Exception {
        String nombre = nombre();
        CountDownLatch salida = new CountDownLatch(1);
        try (ExecutorService hilos = Executors.newFixedThreadPool(10)) {
            List<Future<Integer>> fallos = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                fallos.add(
                        hilos.submit(
                                () -> {
                                    salida.await();
                                    return repositorio.sumarFallo(nombre, AHORA);
                                }));
            }
            salida.countDown();
            List<Integer> cuentas = new ArrayList<>();
            for (Future<Integer> fallo : fallos) {
                cuentas.add(fallo.get());
            }
            assertThat(cuentas).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
        assertThat(repositorio.leer(nombre).fallidos()).isEqualTo(10);
    }

    @Test
    void dadoUnNombreBloqueado_cuandoSeLee_entoncesTieneSuBloqueoYAlBorrarloSeVa() {
        String nombre = nombre();
        Instant hasta = AHORA.plus(Duration.ofMinutes(15));
        for (int i = 0; i < 5; i++) {
            repositorio.sumarFallo(nombre, AHORA);
        }

        repositorio.bloquear(nombre, hasta, AHORA);

        assertThat(repositorio.leer(nombre)).isEqualTo(new EstadoDeIntentos(5, hasta));
        repositorio.borrar(nombre);
        assertThat(repositorio.leer(nombre)).isEqualTo(EstadoDeIntentos.SIN_INTENTOS);
    }

    @Test
    void dadoNombresViejos_cuandoSeLimpia_entoncesBorraSoloLosSinBloqueoVigente() {
        String viejo = nombre();
        String bloqueado = nombre();
        String reciente = nombre();
        Instant ayer = AHORA.minus(Duration.ofHours(25));
        repositorio.sumarFallo(viejo, ayer);
        repositorio.sumarFallo(bloqueado, ayer);
        repositorio.bloquear(bloqueado, AHORA.plusSeconds(60), ayer);
        repositorio.sumarFallo(reciente, AHORA);

        int borrados = repositorio.limpiar(AHORA.minus(Duration.ofHours(24)), AHORA);

        assertThat(borrados).isGreaterThanOrEqualTo(1);
        assertThat(repositorio.leer(viejo)).isEqualTo(EstadoDeIntentos.SIN_INTENTOS);
        assertThat(repositorio.leer(bloqueado).fallidos()).isEqualTo(1);
        assertThat(repositorio.leer(reciente).fallidos()).isEqualTo(1);
    }

    private static String nombre() {
        return "prueba-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
