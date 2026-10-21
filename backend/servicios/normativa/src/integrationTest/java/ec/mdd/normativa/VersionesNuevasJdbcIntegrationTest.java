package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.normativa.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.adaptadores.salida.TransaccionSpring;
import ec.mdd.normativa.aplicacion.EditarVersionServicio;
import ec.mdd.normativa.dominio.AsignacionNormativa;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Guardar versiones nuevas, editarlas y asociarlas a periodos contra PostgreSQL, con la numeración
 * y los bloqueos de research §3 (F04-H02).
 */
class VersionesNuevasJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    // La base guarda microsegundos.
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    private static RepositorioVersionesJdbc repositorio;
    private static TransaccionSpring transaccion;
    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioVersionesJdbc(jdbc);
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    private static DatosDeVersion datos() {
        return new DatosDeVersion(
                "PRUEBA-" + UUID.randomUUID().toString().substring(0, 8),
                "Reforma de prueba",
                LocalDate.of(2025, 1, 15));
    }

    // Copia la versión 1 con el siguiente número y la guarda, en una transacción.
    private static VersionNormativa copiaGuardada() {
        return transaccion.ejecutar(
                () -> {
                    VersionNormativa copia =
                            VersionNormativa.copiaDe(
                                            repositorio.buscar(1).orElseThrow(),
                                            repositorio.siguienteNumero(),
                                            datos(),
                                            AHORA)
                                    .version();
                    repositorio.guardar(copia, "vicerrectorado.demo", AHORA);
                    return copia;
                });
    }

    private static CambiosDeVersion conClaseDeMedioTiempo(VersionNormativa version, int clase) {
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

    @Test
    void dadoUnaCopiaDeLaUno_cuandoSeGuarda_entoncesSeReconstruyeIgualValorPorValor() {
        VersionNormativa uno = repositorio.buscar(1).orElseThrow();

        VersionNormativa copia = copiaGuardada();

        VersionNormativa leida = repositorio.buscar(copia.numero()).orElseThrow();
        assertThat(leida).isEqualTo(copia);
        assertThat(leida.copiadaDe()).isEqualTo(1);
        assertThat(leida.tabla()).isEqualTo(uno.tabla());
        assertThat(leida.reglas()).isEqualTo(uno.reglas());
        assertThat(repositorio.buscar(1)).contains(uno);
        assertThat(repositorio.existeCodigo(copia.codigo(), 0)).isTrue();
        assertThat(repositorio.existeCodigo(copia.codigo(), copia.numero())).isFalse();
        assertThat(repositorio.existeCodigo("NO-EXISTE", 0)).isFalse();
    }

    @Test
    void dadoUnaVersionEditada_cuandoSeGuarda_entoncesSeReemplazanSusFilasYQuedaIgual() {
        VersionNormativa copia = copiaGuardada();
        int nueva = clase(copia) - 1;
        VersionNormativa editada = copia.editada(conClaseDeMedioTiempo(copia, nueva)).version();

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(editada, "vicerrectorado.demo", AHORA);
                    return null;
                });

        VersionNormativa leida = repositorio.buscar(copia.numero()).orElseThrow();
        assertThat(leida).isEqualTo(editada);
        assertThat(clase(leida)).isEqualTo(nueva);
        assertThat(leida.tabla().filas())
                .extracting(fila -> fila.perfil())
                .containsExactlyElementsOf(
                        copia.tabla().filas().stream().map(fila -> fila.perfil()).toList());
        assertThat(clase(repositorio.buscar(1).orElseThrow())).isEqualTo(clase(copia));
    }

    @Test
    void dadoUnaEdicionQueFallaAMitad_cuandoSeGuarda_entoncesNoQuedaNadaCambiado() {
        VersionNormativa copia = copiaGuardada();
        VersionNormativa editada =
                copia.editada(conClaseDeMedioTiempo(copia, clase(copia) - 1)).version();

        assertThatThrownBy(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            repositorio.guardar(
                                                    editada, "vicerrectorado.demo", AHORA);
                                            throw new IllegalStateException("falla a mitad");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(repositorio.buscar(copia.numero())).contains(copia);
    }

    @Test
    void dadoDosCreacionesSimultaneas_cuandoPidenNumero_entoncesObtienenNumerosDistintos() {
        CountDownLatch numerado = new CountDownLatch(1);
        CompletableFuture<Integer> primera =
                CompletableFuture.supplyAsync(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            int numero = repositorio.siguienteNumero();
                                            numerado.countDown();
                                            esperar(400);
                                            guardarCopia(numero);
                                            return numero;
                                        }));
        CompletableFuture<Integer> segunda =
                CompletableFuture.supplyAsync(
                        () -> {
                            aguardar(numerado);
                            return transaccion.ejecutar(
                                    () -> {
                                        int numero = repositorio.siguienteNumero();
                                        guardarCopia(numero);
                                        return numero;
                                    });
                        });

        int a = primera.join();
        int b = segunda.join();

        assertThat(b).isEqualTo(a + 1);
        assertThat(repositorio.buscar(a)).isPresent();
        assertThat(repositorio.buscar(b)).isPresent();
    }

    @Test
    void dadoUnaVersionBloqueada_cuandoOtraTransaccionLaBloquea_entoncesEsperaAQueTermine() {
        VersionNormativa copia = copiaGuardada();
        CountDownLatch bloqueada = new CountDownLatch(1);
        AtomicLong liberada = new AtomicLong();
        CompletableFuture<Void> primera =
                CompletableFuture.runAsync(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            repositorio.bloquear(copia.numero()).orElseThrow();
                                            bloqueada.countDown();
                                            esperar(400);
                                            liberada.set(System.nanoTime());
                                            return null;
                                        }));
        aguardar(bloqueada);

        long obtenida =
                transaccion.ejecutar(
                        () -> {
                            repositorio.bloquear(copia.numero()).orElseThrow();
                            return System.nanoTime();
                        });
        primera.join();

        assertThat(obtenida).isGreaterThan(liberada.get());
    }

    @Test
    void dadoUnaVersionAsociada_cuandoSeGuarda_entoncesTraeSuPeriodoYElPeriodoEsUnico() {
        VersionNormativa copia = copiaGuardada();
        String periodo = "p-" + copia.numero();
        VersionNormativa asociada = copia.asociadaA(periodo, AHORA);

        transaccion.ejecutar(
                () -> {
                    repositorio.bloquearPeriodo(periodo);
                    repositorio.guardar(asociada, "vicerrectorado.demo", AHORA);
                    return null;
                });

        VersionNormativa leida = repositorio.buscar(copia.numero()).orElseThrow();
        assertThat(leida.asignaciones()).containsExactly(new AsignacionNormativa(periodo, AHORA));
        assertThat(leida.enUso()).isTrue();
        assertThat(repositorio.porPeriodo(periodo)).contains(leida);
        assertThat(repositorio.porPeriodo("sin-version")).isEmpty();
        assertThat(
                        jdbc.sql(
                                        "SELECT asociada_por FROM normativa.asignacion_normativa"
                                                + " WHERE periodo = :periodo")
                                .param("periodo", periodo)
                                .query(String.class)
                                .single())
                .isEqualTo("vicerrectorado.demo");

        VersionNormativa otra = copiaGuardada();
        assertThatThrownBy(
                        () ->
                                transaccion.ejecutar(
                                        () -> {
                                            repositorio.guardar(
                                                    otra.asociadaA(periodo, AHORA),
                                                    "vicerrectorado.demo",
                                                    AHORA);
                                            return null;
                                        }))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repositorio.buscar(otra.numero()).orElseThrow().enUso()).isFalse();
    }

    private static void guardarCopia(int numero) {
        repositorio.guardar(
                VersionNormativa.copiaDe(
                                repositorio.buscar(1).orElseThrow(), numero, datos(), AHORA)
                        .version(),
                "vicerrectorado.demo",
                AHORA);
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private static void aguardar(CountDownLatch senal) {
        try {
            assertThat(senal.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    // F04-H03: la severidad cambiada se guarda, se registra y no toca la versión de origen.
    @Test
    void dadoUnaCopia_cuandoRn05PasaADura_entoncesSeGuardaSeRegistraYLaUnoSigueBlanda() {
        VersionNormativa copia = copiaGuardada();
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        copia.reglas().forEach(regla -> parametros.put(regla.identificador(), regla.parametros()));
        CambiosDeVersion cambios =
                new CambiosDeVersion(
                        new DatosDeVersion(copia.codigo(), copia.origen(), copia.vigenteDesde()),
                        copia.tabla().filas().stream().map(HorasDeFila::de).toList(),
                        parametros,
                        Map.of("RN-05", Severidad.DURA));
        EditarVersionServicio editar =
                new EditarVersionServicio(
                        repositorio,
                        new RegistroDeCambiosJdbc(jdbc),
                        transaccion,
                        Clock.fixed(AHORA, ZoneOffset.UTC));

        editar.editar(copia.numero(), cambios, "vicerrectorado.demo");

        VersionNormativa leida = repositorio.buscar(copia.numero()).orElseThrow();
        VersionNormativa uno = repositorio.buscar(1).orElseThrow();
        assertThat(leida.regla("RN-05").orElseThrow().severidad()).isEqualTo(Severidad.DURA);
        assertThat(uno.regla("RN-05").orElseThrow().severidad()).isEqualTo(Severidad.BLANDA);
        // FR-007: la copia trae la afinidad y las reglas del motor sin parámetros de la 1.
        for (String identificador : List.of("RN-19", "RN-24", "RF-24")) {
            assertThat(leida.regla(identificador)).isEqualTo(uno.regla(identificador));
        }
        assertThat(
                        jdbc.sql(
                                        "select c ->> 'anterior' || '>' || (c ->> 'nuevo')"
                                                + " from normativa.registro_cambio,"
                                                + " jsonb_array_elements(detalle -> 'cambios') c"
                                                + " where accion = 'VERSION_EDITADA'"
                                                + " and (detalle ->> 'numero')::int = :numero"
                                                + " and c ->> 'campo' = 'reglas[RN-05].severidad'")
                                .param("numero", copia.numero())
                                .query(String.class)
                                .list())
                .containsExactly("BLANDA>DURA");
    }
}
