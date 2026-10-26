package ec.mdd.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.docentes.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioCargosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioDocentesJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioProyectosJdbc;
import ec.mdd.docentes.adaptadores.salida.TransaccionSpring;
import ec.mdd.docentes.aplicacion.MantenerCargosServicio;
import ec.mdd.docentes.aplicacion.MantenerDocentesServicio;
import ec.mdd.docentes.aplicacion.MantenerProyectosServicio;
import ec.mdd.docentes.aplicacion.cargas.CargasDeDocentes;
import ec.mdd.docentes.dominio.DocenteNoHabilitado;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.infraestructura.DocentesFlywayConfig;
import ec.mdd.docentes.puertos.entrada.CoordinadorVigente;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Las cargas de Docentes con PostgreSQL real (F01-H04, MD-13): la 06, la 08 y la 09 se aplican
 * completas o nada en una transacción de Docentes, con sus casos de uso y sus reglas, sobre las
 * referencias de Académico de las cargas 01 a 04 de prueba.
 */
class AplicadorDocentesIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    // Un docente de prueba de medio tiempo (los de cargo son a tiempo completo).
    private static final String MEDIO_TIEMPO = "0109990424";

    private static JdbcClient jdbc;
    private static AplicadorDeCargas aplicador;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        TransaccionSpring transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        RepositorioDocentesJdbc docentes = new RepositorioDocentesJdbc(jdbc);
        RepositorioCargosJdbc cargos = new RepositorioCargosJdbc(jdbc);
        RepositorioProyectosJdbc proyectos = new RepositorioProyectosJdbc(jdbc);
        RegistroDeCambiosJdbc registro = new RegistroDeCambiosJdbc(jdbc);
        Referencias referencias = referencias();
        aplicador =
                CargasDeDocentes.aplicador(
                        transaccion,
                        docentes,
                        new MantenerDocentesServicio(
                                docentes,
                                () -> referencias,
                                registro,
                                transaccion,
                                Clock.systemUTC()),
                        cargos,
                        new MantenerCargosServicio(
                                cargos,
                                docentes,
                                () -> referencias,
                                registro,
                                transaccion,
                                Clock.fixed(
                                        LocalDate.of(2026, 6, 1)
                                                .atStartOfDay(ZoneId.of("America/Guayaquil"))
                                                .toInstant(),
                                        ZoneId.of("America/Guayaquil"))),
                        proyectos,
                        new MantenerProyectosServicio(
                                proyectos,
                                docentes,
                                () -> referencias,
                                registro,
                                transaccion,
                                Clock.systemUTC()));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql(
                        "TRUNCATE docentes.proyecto, docentes.cargo, docentes.docente,"
                                + " docentes.registro_cambio")
                .update();
    }

    /** Las referencias de Académico que existirían con las cargas 01 a 04 de prueba aplicadas. */
    private static Referencias referencias() {
        Map<String, Referencias.Carrera> carreras = new LinkedHashMap<>();
        for (FilaDelLote fila : LotesDePrueba.lote("carreras", "02_carreras.csv").filas()) {
            carreras.put(
                    fila.valor("codigo_carrera"),
                    new Referencias.Carrera(fila.valor("nombre"), fila.valor("siglas")));
        }
        Map<String, Referencias.Asignatura> asignaturas = new LinkedHashMap<>();
        for (FilaDelLote fila :
                LotesDePrueba.lote("asignaturas_malla", "04_asignaturas_malla.csv").filas()) {
            asignaturas.put(
                    fila.valor("codigo_asignatura"),
                    new Referencias.Asignatura(fila.valor("nombre"), fila.valor("codigo_carrera")));
        }
        Map<String, Referencias.Unidad> unidades = new LinkedHashMap<>();
        for (FilaDelLote fila :
                LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv").filas()) {
            unidades.put(
                    fila.valor("codigo_unidad"),
                    new Referencias.Unidad(
                            fila.valor("nombre"),
                            fila.valor("tipo_unidad"),
                            fila.valor("codigo_gestion_autorizada").isEmpty()
                                    ? null
                                    : fila.valor("codigo_gestion_autorizada"),
                            "ACTIVO".equals(fila.valor("estado"))));
        }
        Map<String, Referencias.Periodo> periodos = new LinkedHashMap<>();
        for (FilaDelLote fila :
                LotesDePrueba.lote("ciclos_academicos", "03_ciclos_academicos.csv").filas()) {
            periodos.put(
                    fila.valor("codigo_periodo"),
                    new Referencias.Periodo(
                            fila.valor("nombre"),
                            LocalDate.parse(fila.valor("fecha_inicio")),
                            LocalDate.parse(fila.valor("fecha_fin"))));
        }
        Map<String, String> campos = new LinkedHashMap<>();
        for (int amplio = 0; amplio <= 10; amplio++) {
            campos.put(String.format("%02d", amplio), "Campo " + amplio);
        }
        return new Referencias(carreras, asignaturas, campos, unidades, periodos);
    }

    private static ResultadoDelLote confirmar(LoteDeCarga lote) {
        return aplicador.aplicar(lote, aplicador.previsualizar(lote).huella(), "datos.demo");
    }

    private static long filas(String tabla) {
        return jdbc.sql("SELECT count(*) FROM docentes." + tabla).query(Long.class).single();
    }

    @Test
    void laCarga06_08Y09EnSuOrden_quedanAplicadasYSubidasOtraVezSonIguales() {
        List<LoteDeCarga> lotes =
                List.of(
                        LotesDePrueba.lote("docentes", "06_docentes.csv"),
                        LotesDePrueba.lote("cargos_autoridades", "08_cargos_autoridades.csv"),
                        LotesDePrueba.lote("proyectos_practicas", "09_proyectos_practicas.csv"));

        assertThat(lotes.stream().map(lote -> confirmar(lote).nuevos()).toList())
                .containsExactly(47, 21, 11);
        assertThat(filas("docente")).isEqualTo(47);
        for (LoteDeCarga lote : lotes) {
            assertThat(aplicador.previsualizar(lote).cuantos(TipoDeRegistro.IGUAL))
                    .as(lote.carga())
                    .isEqualTo(lote.filas().size());
        }
    }

    @Test
    void unSegundoCoordinadorVigenteDeLaMismaCarrera_rechazaSuFilaSinAplicarNada() {
        confirmar(LotesDePrueba.lote("docentes", "06_docentes.csv"));
        LoteDeCarga cargos = LotesDePrueba.lote("cargos_autoridades", "08_cargos_autoridades.csv");
        List<FilaDelLote> filas = new ArrayList<>(cargos.filas());
        FilaDelLote coordinador =
                filas.stream()
                        .filter(fila -> fila.valor("codigo_cargo").equals("COORDINADOR_CARRERA"))
                        .findFirst()
                        .orElseThrow();
        FilaDelLote otroCoordinador =
                filas.stream()
                        .filter(fila -> fila.valor("codigo_cargo").equals("COORDINADOR_CARRERA"))
                        .skip(1)
                        .findFirst()
                        .orElseThrow();
        filas.add(
                new FilaDelLote(
                        23,
                        new java.util.HashMap<>(
                                LotesDePrueba.cambiar(
                                                LotesDePrueba.cambiar(
                                                        coordinador,
                                                        "numero_identificacion",
                                                        otroCoordinador.valor(
                                                                "numero_identificacion")),
                                                "fecha_designacion",
                                                "2026-05-01")
                                        .valores())));

        assertThatThrownBy(() -> confirmar(LotesDePrueba.con(cargos, filas)))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo -> {
                            assertThat(rechazo.fila()).isEqualTo(23);
                            assertThat(rechazo.getCause()).isInstanceOf(CoordinadorVigente.class);
                        });
        assertThat(filas("cargo")).as("ninguno de los 21 cargos anteriores").isZero();
    }

    @Test
    void unCargoParaUnDocenteDeMedioTiempo_rechazaLaFilaConDocenteNoHabilitado() {
        confirmar(LotesDePrueba.lote("docentes", "06_docentes.csv"));
        LoteDeCarga cargos = LotesDePrueba.lote("cargos_autoridades", "08_cargos_autoridades.csv");
        FilaDelLote medioTiempo =
                LotesDePrueba.cambiar(
                        cargos.filas().getFirst(), "numero_identificacion", MEDIO_TIEMPO);

        assertThatThrownBy(() -> confirmar(LotesDePrueba.con(cargos, List.of(medioTiempo))))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo ->
                                assertThat(rechazo.getCause())
                                        .isInstanceOf(DocenteNoHabilitado.class));
        assertThat(filas("cargo")).isZero();
    }
}
