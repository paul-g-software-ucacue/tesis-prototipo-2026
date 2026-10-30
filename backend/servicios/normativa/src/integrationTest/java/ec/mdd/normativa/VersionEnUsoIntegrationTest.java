package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.normativa.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.adaptadores.salida.TransaccionSpring;
import ec.mdd.normativa.aplicacion.AsociarVersionAPeriodoServicio;
import ec.mdd.normativa.aplicacion.CrearVersionServicio;
import ec.mdd.normativa.aplicacion.EditarVersionServicio;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
 * Una versión en uso no cambia, también con concurrencia (FR-004, research §3): con los servicios
 * reales y PostgreSQL, una edición que llega mientras otra transacción asocia la versión espera y
 * la encuentra en uso; dos ediciones a la vez se guardan una detrás de otra.
 */
class VersionEnUsoIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String QUIEN = "vicerrectorado.demo";

    private static JdbcClient jdbc;
    private static RepositorioVersionesJdbc repositorio;
    private static TransaccionSpring transaccion;
    private static RegistroDeCambiosJdbc registro;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioVersionesJdbc(jdbc);
        registro = new RegistroDeCambiosJdbc(jdbc);
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoUnaAsociacionEnCurso_cuandoLlegaUnaEdicion_entoncesEsperaYLaRechazaPorEnUso() {
        VersionNormativa version = nueva();
        CountDownLatch bloqueada = new CountDownLatch(1);
        AsociarVersionAPeriodoServicio asociar =
                new AsociarVersionAPeriodoServicio(
                        new Lento(repositorio, bloqueada),
                        registro,
                        transaccion,
                        Clock.systemUTC());
        EditarVersionServicio editar = editar(repositorio);

        CompletableFuture<?> asociacion =
                CompletableFuture.runAsync(
                        () -> asociar.asociar("p-" + version.numero(), version.numero(), QUIEN));
        aguardar(bloqueada);

        assertThatThrownBy(
                        () ->
                                editar.editar(
                                        version.numero(),
                                        conClase(version, clase(version) - 1),
                                        QUIEN))
                .isInstanceOf(VersionEnUso.class);
        asociacion.join();
        VersionNormativa leida = repositorio.buscar(version.numero()).orElseThrow();
        assertThat(leida.enUso()).isTrue();
        assertThat(clase(leida)).isEqualTo(clase(version));
        assertThat(acciones(version.numero()))
                .containsExactly("VERSION_CREADA", "VERSION_ASOCIADA");
    }

    @Test
    void dadoDosEdicionesALaVez_cuandoSeGuardan_entoncesVanEnOrdenYQuedanLasDosEnElRegistro() {
        VersionNormativa version = nueva();
        int original = clase(version);
        CountDownLatch bloqueada = new CountDownLatch(1);
        EditarVersionServicio lenta = editar(new Lento(repositorio, bloqueada));
        EditarVersionServicio rapida = editar(repositorio);

        CompletableFuture<VersionNormativa> primera =
                CompletableFuture.supplyAsync(
                        () ->
                                lenta.editar(
                                        version.numero(), conClase(version, original - 1), QUIEN));
        aguardar(bloqueada);
        VersionNormativa segunda =
                rapida.editar(version.numero(), conClase(version, original - 2), QUIEN);
        primera.join();

        assertThat(clase(segunda)).isEqualTo(original - 2);
        assertThat(clase(repositorio.buscar(version.numero()).orElseThrow()))
                .isEqualTo(original - 2);
        assertThat(acciones(version.numero()))
                .containsExactly("VERSION_CREADA", "VERSION_EDITADA", "VERSION_EDITADA");
    }

    private static VersionNormativa nueva() {
        return new CrearVersionServicio(repositorio, registro, transaccion, Clock.systemUTC())
                .crear(
                        1,
                        new DatosDeVersion(
                                "PRUEBA-" + UUID.randomUUID().toString().substring(0, 8),
                                "Reforma de prueba",
                                LocalDate.of(2025, 1, 15)),
                        QUIEN);
    }

    private static EditarVersionServicio editar(RepositorioVersiones con) {
        return new EditarVersionServicio(con, registro, transaccion, Clock.systemUTC());
    }

    private static List<String> acciones(int numero) {
        return jdbc.sql(
                        "SELECT accion FROM normativa.registro_cambio"
                                + " WHERE (detalle ->> 'numero')::int = :numero ORDER BY momento")
                .param("numero", numero)
                .query(String.class)
                .list();
    }

    private static CambiosDeVersion conClase(VersionNormativa version, int clase) {
        List<HorasDeFila> filas =
                version.tabla().filas().stream()
                        .map(
                                fila ->
                                        fila.perfil().equals("DOCENTE_MT")
                                                ? HorasDeFila.de(fila).conHorasClase(clase)
                                                : HorasDeFila.de(fila))
                        .toList();
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas()
                .forEach(regla -> parametros.put(regla.identificador(), regla.parametros()));
        return new CambiosDeVersion(
                new DatosDeVersion(version.codigo(), version.origen(), version.vigenteDesde()),
                filas,
                parametros);
    }

    private static int clase(VersionNormativa version) {
        return version.tabla().filas().stream()
                .filter(fila -> fila.perfil().equals("DOCENTE_MT"))
                .findFirst()
                .orElseThrow()
                .horasClase();
    }

    private static void aguardar(CountDownLatch senal) {
        try {
            assertThat(senal.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    /**
     * El repositorio real, que después de bloquear la versión avisa y espera un momento, todavía
     * dentro de la transacción: así la otra operación llega con la versión bloqueada.
     */
    private static final class Lento implements RepositorioVersiones {

        private final RepositorioVersiones real;
        private final CountDownLatch bloqueada;

        Lento(RepositorioVersiones real, CountDownLatch bloqueada) {
            this.real = real;
            this.bloqueada = bloqueada;
        }

        @Override
        public boolean hayVersiones() {
            return real.hayVersiones();
        }

        @Override
        public Optional<VersionNormativa> bloquear(int numero) {
            Optional<VersionNormativa> version = real.bloquear(numero);
            bloqueada.countDown();
            try {
                Thread.sleep(500);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
            return version;
        }

        @Override
        public List<VersionNormativa> todas() {
            return real.todas();
        }

        @Override
        public Optional<VersionNormativa> buscar(int numero) {
            return real.buscar(numero);
        }

        @Override
        public int siguienteNumero() {
            return real.siguienteNumero();
        }

        @Override
        public boolean existeCodigo(String codigo, int salvo) {
            return real.existeCodigo(codigo, salvo);
        }

        @Override
        public void bloquearPeriodo(String periodo) {
            real.bloquearPeriodo(periodo);
        }

        @Override
        public Optional<VersionNormativa> porPeriodo(String periodo) {
            return real.porPeriodo(periodo);
        }

        @Override
        public void guardar(VersionNormativa version, String quien, Instant momento) {
            real.guardar(version, quien, momento);
        }
    }
}
