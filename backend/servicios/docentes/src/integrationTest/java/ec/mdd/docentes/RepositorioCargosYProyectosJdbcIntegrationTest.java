package ec.mdd.docentes;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.docentes.adaptadores.salida.RepositorioCargosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioProyectosJdbc;
import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.infraestructura.DocentesFlywayConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

/**
 * Los repositorios de los cargos y de los proyectos con PostgreSQL real (F03-H05): guardan y leen
 * cada campo igual, también las listas en {@code jsonb} y las horas con sus decimales, encuentran
 * al coordinador vigente de una carrera y toman su candado.
 */
class RepositorioCargosYProyectosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-25T15:00:00Z");
    private static final List<String> DOCENTES = List.of("0109990010", "0109990028", "0109990044");

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioCargosJdbc cargos;
    private static RepositorioProyectosJdbc proyectos;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        cargos = new RepositorioCargosJdbc(jdbc);
        proyectos = new RepositorioProyectosJdbc(jdbc);
        for (String docente : DOCENTES) {
            jdbc.sql(
                            "INSERT INTO docentes.docente (identificacion, tipo_identificacion,"
                                    + " primer_apellido, primer_nombre, correo_institucional,"
                                    + " tipo_personal, categoria_escalafon, dedicacion,"
                                    + " horas_contratadas, fecha_ingreso, inicio_contrato,"
                                    + " condicion_especial, carrera_base, titulo_tercer_nivel,"
                                    + " campo_amplio, campo_especifico, estado, registrado_en,"
                                    + " registrado_por) VALUES (:id, 'CEDULA', 'PERALTA', 'JUAN',"
                                    + " :correo, 'TITULAR', 'AUXILIAR_1', 'TC', 40, DATE"
                                    + " '2019-10-01', DATE '2026-04-01', 'NINGUNA', '550613A01-P-0301',"
                                    + " 'INGENIERO', '06', '061', 'ACTIVO', now(), 'datos.demo')")
                    .param("id", docente)
                    .param("correo", docente + "@prueba.edu.ec")
                    .update();
        }
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM docentes.cargo").update();
        jdbc.sql("DELETE FROM docentes.proyecto").update();
    }

    @Test
    void dadoUnCargo_cuandoSeGuardaYSeLee_entoncesEsIgualCampoPorCampo() {
        Cargo gestor =
                new Cargo(
                        DOCENTES.get(1),
                        TipoDeCargo.G07_GESTOR_INVESTIGACION,
                        LocalDate.of(2025, 4, 1),
                        "G-INV",
                        null,
                        LocalDate.of(2027, 3, 31),
                        "RES-PRUEBA-2025-012",
                        EstadoDeCargo.VIGENTE);
        Cargo coordinador = coordinador(DOCENTES.get(0), "C1", EstadoDeCargo.VIGENTE);

        cargos.registrar(gestor, "datos.demo", AHORA);
        cargos.registrar(coordinador, "datos.demo", AHORA);

        assertThat(cargos.todos()).containsExactly(coordinador, gestor);
        assertThat(cargos.buscar(gestor.docente(), gestor.tipo(), gestor.fechaDesignacion()))
                .contains(gestor);
        assertThat(cargos.buscar(gestor.docente(), gestor.tipo(), LocalDate.of(2025, 4, 2)))
                .isEmpty();
        assertThat(
                        jdbc.sql("SELECT registrado_por FROM docentes.cargo WHERE docente = :d")
                                .param("d", gestor.docente())
                                .query(String.class)
                                .single())
                .isEqualTo("datos.demo");
    }

    @Test
    void dadoUnCargo_cuandoSeGuarda_entoncesCambiaCadaDatoYSuClaveNo() {
        Cargo antes = coordinador(DOCENTES.get(0), "C1", EstadoDeCargo.VIGENTE);
        cargos.registrar(antes, "datos.demo", AHORA);
        Cargo finalizado =
                new Cargo(
                        antes.docente(),
                        antes.tipo(),
                        antes.fechaDesignacion(),
                        "CC-DS",
                        "C1",
                        LocalDate.of(2026, 3, 31),
                        "RES-PRUEBA-2026-099",
                        EstadoDeCargo.FINALIZADO);

        cargos.guardar(finalizado);

        assertThat(cargos.todos()).containsExactly(finalizado);
    }

    @Test
    void
            dadoUnCoordinadorVigente_cuandoSeConsultaSuCarrera_entoncesLoEncuentraSalvoElQueSeCorrige() {
        Cargo vigente = coordinador(DOCENTES.get(0), "C1", EstadoDeCargo.VIGENTE);
        cargos.registrar(vigente, "datos.demo", AHORA);
        cargos.registrar(
                coordinador(DOCENTES.get(1), "C2", EstadoDeCargo.FINALIZADO), "datos.demo", AHORA);

        assertThat(cargos.hayCoordinadorVigente("C1", null)).isTrue();
        assertThat(cargos.hayCoordinadorVigente("C1", vigente)).isFalse();
        assertThat(cargos.hayCoordinadorVigente("C2", null)).isFalse();
        assertThat(cargos.hayCoordinadorVigente("C3", null)).isFalse();
    }

    @Test
    void dadoUnProyecto_cuandoSeGuardaYSeLee_entoncesEsIgualConSusListasYSusHoras() {
        Proyecto proyecto = proyecto("INV-2025-01", "14.25", "9.75", "24");

        proyectos.registrar(proyecto, "datos.demo", AHORA);

        Proyecto leido = proyectos.buscar("INV-2025-01").orElseThrow();
        assertThat(leido).isEqualTo(proyecto);
        assertThat(leido.participantes().getFirst().horas()).isEqualByComparingTo("14.25");
        assertThat(leido.horasAprobadas()).isEqualByComparingTo("24");
        assertThat(proyectos.buscar("NO-EXISTE")).isEmpty();
    }

    @Test
    void dadosVariosProyectos_cuandoSeGuardaUno_entoncesCambiaSusDatosYTodosVanPorCodigo() {
        proyectos.registrar(proyecto("VIN-2026-01", "10", "8", "18"), "datos.demo", AHORA);
        Proyecto antes = proyecto("INV-2025-01", "14", "10", "24");
        proyectos.registrar(antes, "datos.demo", AHORA);
        Proyecto suspendido =
                new Proyecto(
                        antes.codigo(),
                        "Otro nombre",
                        TipoDeProyecto.VINCULACION,
                        List.of("C2", "C1"),
                        "RES-OCS-2026-001",
                        LocalDate.of(2026, 1, 10),
                        EstadoDeProyecto.SUSPENDIDO,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2027, 1, 31),
                        DOCENTES.get(1),
                        List.of(
                                new Participante(
                                        DOCENTES.get(1),
                                        RolDeParticipante.DIRECTOR,
                                        new BigDecimal("2.5"))),
                        new BigDecimal("2.5"));

        proyectos.guardar(suspendido);

        assertThat(proyectos.todos())
                .extracting(Proyecto::codigo)
                .containsExactly("INV-2025-01", "VIN-2026-01");
        assertThat(proyectos.buscar("INV-2025-01")).contains(suspendido);
    }

    @Test
    void dadaUnaTransaccionConElCandadoDeLosCargos_cuandoOtraLoPide_entoncesEspera()
            throws Exception {
        candado(cargos::bloquear);
    }

    @Test
    void dadaUnaTransaccionConElCandadoDeLosProyectos_cuandoOtraLoPide_entoncesEspera()
            throws Exception {
        candado(proyectos::bloquear);
    }

    private static void candado(Runnable bloquear) throws Exception {
        TransactionTemplate transacciones =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CompletableFuture<Void> soltar = new CompletableFuture<>();
        CompletableFuture<Void> primera =
                CompletableFuture.runAsync(
                        () ->
                                transacciones.executeWithoutResult(
                                        estado -> {
                                            bloquear.run();
                                            soltar.join();
                                        }));
        Thread.sleep(500);

        CompletableFuture<Void> segunda =
                CompletableFuture.runAsync(
                        () -> transacciones.executeWithoutResult(estado -> bloquear.run()));

        try {
            segunda.get(1, TimeUnit.SECONDS);
            throw new AssertionError("La segunda transacción no esperó el candado");
        } catch (TimeoutException esperado) {
            soltar.complete(null);
        }
        primera.get(5, TimeUnit.SECONDS);
        segunda.get(5, TimeUnit.SECONDS);
    }

    private static Cargo coordinador(String docente, String carrera, EstadoDeCargo estado) {
        return new Cargo(
                docente,
                TipoDeCargo.COORDINADOR_CARRERA,
                LocalDate.of(2025, 4, 1),
                null,
                carrera,
                null,
                "RES-PRUEBA-2025-004",
                estado);
    }

    private static Proyecto proyecto(
            String codigo, String director, String investigador, String aprobadas) {
        return new Proyecto(
                codigo,
                "Prototipo de monitoreo IoT",
                TipoDeProyecto.INVESTIGACION,
                List.of("550613A01-P-0301", "550811F01-P-0301"),
                "RES-OCS-PRUEBA-2025-040",
                LocalDate.of(2025, 3, 20),
                EstadoDeProyecto.EN_EJECUCION,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 12, 31),
                DOCENTES.get(0),
                List.of(
                        new Participante(
                                DOCENTES.get(0),
                                RolDeParticipante.DIRECTOR,
                                new BigDecimal(director)),
                        new Participante(
                                DOCENTES.get(2),
                                RolDeParticipante.INVESTIGADOR,
                                new BigDecimal(investigador))),
                new BigDecimal(aprobadas));
    }
}
