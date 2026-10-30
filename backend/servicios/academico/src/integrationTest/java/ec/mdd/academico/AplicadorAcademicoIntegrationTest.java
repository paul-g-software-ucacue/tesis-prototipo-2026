package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioCarrerasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioEstructuraJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioMatriculasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioOfertaJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioPeriodosJdbc;
import ec.mdd.academico.adaptadores.salida.TransaccionSpring;
import ec.mdd.academico.aplicacion.CorregirPeriodoServicio;
import ec.mdd.academico.aplicacion.MantenerCarrerasServicio;
import ec.mdd.academico.aplicacion.MantenerEstructuraServicio;
import ec.mdd.academico.aplicacion.MantenerMatriculasServicio;
import ec.mdd.academico.aplicacion.MantenerOfertaServicio;
import ec.mdd.academico.aplicacion.RegistrarPeriodoServicio;
import ec.mdd.academico.aplicacion.cargas.CargasDeAcademico;
import ec.mdd.academico.dominio.SedeMatrizExistente;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
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
 * Las cargas de Académico con PostgreSQL real (F01-H04, MD-13): cada lote se aplica completo o nada
 * en una transacción de Académico, con sus casos de uso y su registro de cambios.
 */
class AplicadorAcademicoIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static final List<String> avisos = new ArrayList<>();

    private static JdbcClient jdbc;
    private static AplicadorDeCargas aplicador;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        TransaccionSpring transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        RepositorioEstructuraJdbc estructura = new RepositorioEstructuraJdbc(jdbc);
        RepositorioCarrerasJdbc carreras = new RepositorioCarrerasJdbc(jdbc);
        RegistroDeCambiosJdbc registro = new RegistroDeCambiosJdbc(jdbc);
        RepositorioPeriodosJdbc periodos = new RepositorioPeriodosJdbc(jdbc);
        RepositorioOfertaJdbc oferta = new RepositorioOfertaJdbc(jdbc);
        RepositorioMatriculasJdbc matriculas = new RepositorioMatriculasJdbc(jdbc);
        NormativaDelPeriodo sinNormativa =
                new NormativaDelPeriodo() {
                    @Override
                    public OptionalInt versionDe(String codigo) {
                        return OptionalInt.empty();
                    }

                    @Override
                    public void asociar(String codigo, int version, String quien) {}
                };
        Clock reloj = Clock.systemUTC();
        aplicador =
                CargasDeAcademico.aplicador(
                        new CargasDeAcademico.Dependencias(
                                transaccion,
                                estructura,
                                new MantenerEstructuraServicio(
                                        estructura, registro, transaccion, reloj),
                                carreras,
                                new MantenerCarrerasServicio(
                                        carreras, estructura, registro, transaccion, reloj),
                                periodos,
                                new RegistrarPeriodoServicio(
                                        periodos,
                                        sinNormativa,
                                        registro,
                                        periodo -> avisos.add(periodo.codigo()),
                                        transaccion,
                                        reloj),
                                new CorregirPeriodoServicio(
                                        periodos, sinNormativa, registro, transaccion, reloj),
                                oferta,
                                new MantenerOfertaServicio(
                                        periodos,
                                        oferta,
                                        carreras,
                                        estructura,
                                        registro,
                                        transaccion,
                                        reloj),
                                matriculas,
                                new MantenerMatriculasServicio(
                                        matriculas, registro, transaccion, reloj)));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql(
                        "TRUNCATE academico.matricula, academico.asignatura_paralelo,"
                                + " academico.asignatura, academico.malla, academico.carrera,"
                                + " academico.unidad_organica, academico.periodo_academico,"
                                + " academico.registro_cambio")
                .update();
        avisos.clear();
    }

    private static ResultadoDelLote confirmar(LoteDeCarga lote) {
        VistaPreviaDelLote vista = aplicador.previsualizar(lote);
        return aplicador.aplicar(lote, vista.huella(), "datos.demo");
    }

    private static long filas(String tabla) {
        return jdbc.sql("SELECT count(*) FROM academico." + tabla).query(Long.class).single();
    }

    @Test
    void confirmarLaEstructuraYDespuesLasCarreras_lasDejaAplicadas() {
        LoteDeCarga estructura =
                LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv");
        LoteDeCarga carreras = LotesDePrueba.lote("carreras", "02_carreras.csv");

        assertThat(aplicador.previsualizar(estructura).cuantos(TipoDeRegistro.NUEVO)).isEqualTo(24);
        assertThat(confirmar(estructura)).isEqualTo(new ResultadoDelLote(24, 0, 0));
        assertThat(confirmar(carreras)).isEqualTo(new ResultadoDelLote(7, 0, 0));

        assertThat(filas("unidad_organica")).isEqualTo(24);
        assertThat(filas("carrera")).isEqualTo(7);
        assertThat(
                        jdbc.sql("SELECT DISTINCT usuario FROM academico.registro_cambio")
                                .query(String.class)
                                .list())
                .containsExactly("datos.demo");
        assertThat(aplicador.previsualizar(estructura).cuantos(TipoDeRegistro.IGUAL))
                .as("subirla otra vez: todo igual")
                .isEqualTo(24);
        assertThat(aplicador.previsualizar(carreras).cuantos(TipoDeRegistro.IGUAL)).isEqualTo(7);
    }

    @Test
    void lasSeisCargasDeAcademicoEnSuOrden_quedanAplicadasYSubidasOtraVezSonIguales() {
        List<LoteDeCarga> lotes =
                List.of(
                        LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv"),
                        LotesDePrueba.lote("carreras", "02_carreras.csv"),
                        LotesDePrueba.lote("ciclos_academicos", "03_ciclos_academicos.csv"),
                        LotesDePrueba.lote("asignaturas_malla", "04_asignaturas_malla.csv"),
                        LotesDePrueba.lote("oferta_periodo", "05_oferta_periodo.csv"),
                        LotesDePrueba.lote("matriculados_carrera", "10_matriculados_carrera.csv"));

        List<Integer> nuevos = lotes.stream().map(lote -> confirmar(lote).nuevos()).toList();

        assertThat(nuevos).containsExactly(24, 7, 2, 300, 165, 7);
        assertThat(filas("malla")).isEqualTo(12);
        assertThat(avisos)
                .as("Roles recibe cada periodo nuevo")
                .containsExactly("2026-I", "2026-II");
        for (LoteDeCarga lote : lotes) {
            VistaPreviaDelLote otraVez = aplicador.previsualizar(lote);
            assertThat(
                            otraVez.cuantos(TipoDeRegistro.NUEVO)
                                    + otraVez.cuantos(TipoDeRegistro.CAMBIA)
                                    + otraVez.cuantos(TipoDeRegistro.BAJA))
                    .as(lote.carga())
                    .isZero();
        }
    }

    @Test
    void tieneDatos_soloConRegistrosVigentes_entradosPorArchivoOPorPantalla() {
        List<String> seis =
                List.of(
                        "estructura_organica",
                        "carreras",
                        "ciclos_academicos",
                        "asignaturas_malla",
                        "oferta_periodo",
                        "matriculados_carrera");
        assertThat(seis).noneMatch(aplicador::tieneDatos);

        confirmar(LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv"));
        confirmar(LotesDePrueba.lote("carreras", "02_carreras.csv"));
        confirmar(LotesDePrueba.lote("ciclos_academicos", "03_ciclos_academicos.csv"));
        confirmar(LotesDePrueba.lote("asignaturas_malla", "04_asignaturas_malla.csv"));
        confirmar(LotesDePrueba.lote("oferta_periodo", "05_oferta_periodo.csv"));
        confirmar(LotesDePrueba.lote("matriculados_carrera", "10_matriculados_carrera.csv"));
        assertThat(seis).allMatch(aplicador::tieneDatos);

        jdbc.sql("UPDATE academico.unidad_organica SET activa = false").update();
        jdbc.sql("UPDATE academico.carrera SET estado = 'NO_VIGENTE'").update();
        jdbc.sql("UPDATE academico.asignatura SET activa = false").update();
        jdbc.sql("UPDATE academico.asignatura_paralelo SET estado = 'RETIRADA'").update();
        assertThat(aplicador.tieneDatos("estructura_organica")).as("solo inactivas").isFalse();
        assertThat(aplicador.tieneDatos("carreras")).as("solo no vigentes").isFalse();
        assertThat(aplicador.tieneDatos("asignaturas_malla")).as("solo inactivas").isFalse();
        assertThat(aplicador.tieneDatos("oferta_periodo")).as("solo retiradas").isFalse();
        assertThat(aplicador.tieneDatos("ciclos_academicos")).as("en cualquier estado").isTrue();
        assertThat(aplicador.tieneDatos("matriculados_carrera")).isTrue();
    }

    @Test
    void unaSegundaSedeMatriz_rechazaSuFilaYNoAplicaNada() {
        LoteDeCarga estructura =
                LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv");
        List<FilaDelLote> filas = new ArrayList<>(estructura.filas());
        FilaDelLote matriz = filas.getFirst();
        filas.add(
                new FilaDelLote(
                        26,
                        Map.of(
                                "codigo_unidad", "OTRA",
                                "nombre", "Otra sede matriz",
                                "tipo_unidad", "SEDE_MATRIZ",
                                "codigo_unidad_padre", "",
                                "codigo_gestion_autorizada", "",
                                "es_comision_permanente", "",
                                "estado", "ACTIVO")));
        LoteDeCarga conDosMatrices = LotesDePrueba.con(estructura, filas);
        assertThat(matriz.valor("tipo_unidad")).isEqualTo("SEDE_MATRIZ");

        assertThatThrownBy(() -> confirmar(conDosMatrices))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo -> {
                            assertThat(rechazo.fila()).isEqualTo(26);
                            assertThat(rechazo.getCause()).isInstanceOf(SedeMatrizExistente.class);
                        });
        assertThat(filas("unidad_organica")).as("ni las filas anteriores (SC-003)").isZero();
        assertThat(filas("registro_cambio")).isZero();
    }

    @Test
    void unaCarreraQueCambia_seCorrigeYLaQueSeDaDeBajaQuedaComoEstaba() {
        confirmar(LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv"));
        LoteDeCarga carreras = LotesDePrueba.lote("carreras", "02_carreras.csv");
        confirmar(carreras);
        List<FilaDelLote> filas = new ArrayList<>(carreras.filas());
        filas.set(0, LotesDePrueba.cambiar(filas.getFirst(), "numero_ciclos", "6"));
        filas.removeLast();
        LoteDeCarga cambiadas = LotesDePrueba.con(carreras, filas);

        assertThat(confirmar(cambiadas)).isEqualTo(new ResultadoDelLote(0, 1, 1));
        assertThat(filas("carrera")).isEqualTo(7);
        assertThat(
                        jdbc.sql(
                                        "SELECT numero_ciclos FROM academico.carrera WHERE codigo ="
                                                + " :codigo")
                                .param("codigo", filas.getFirst().valor("codigo_carrera"))
                                .query(Integer.class)
                                .single())
                .isEqualTo(6);
    }

    @Test
    void dosConfirmacionesConLaMismaHuella_aplicanUnaSolaVez() {
        LoteDeCarga estructura =
                LotesDePrueba.lote("estructura_organica", "01_estructura_organica.csv");
        String huella = aplicador.previsualizar(estructura).huella();

        aplicador.aplicar(estructura, huella, "datos.demo");

        assertThatThrownBy(() -> aplicador.aplicar(estructura, huella, "datos.demo"))
                .isInstanceOf(ec.mdd.plataforma.cargas.VistaPreviaDesactualizada.class);
        assertThat(filas("unidad_organica")).isEqualTo(24);
    }
}
