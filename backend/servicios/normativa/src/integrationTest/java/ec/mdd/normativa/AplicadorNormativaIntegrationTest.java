package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.normativa.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.adaptadores.salida.TransaccionSpring;
import ec.mdd.normativa.aplicacion.AsociarVersionAPeriodoServicio;
import ec.mdd.normativa.aplicacion.CrearVersionServicio;
import ec.mdd.normativa.aplicacion.EditarVersionServicio;
import ec.mdd.normativa.aplicacion.cargas.CargasDeNormativa;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.CambioDeCampo;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.RegistroDeLaVista;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La carga 07 con PostgreSQL real (F01-H04, research §7): la política entra como una versión de
 * normativa, creada copiando la última o editada entera, y una versión en uso no cambia. Cada
 * prueba migra una base propia, con la versión 1 de la política 2024.
 */
class AplicadorNormativaIntegrationTest {

    private static final String QUIEN = "vicerrectorado.demo";

    /** Lo que cada prueba recibe: el aplicador, el repositorio y la asociación a un periodo. */
    private record Normativa(
            AplicadorDeCargas aplicador,
            RepositorioVersionesJdbc repositorio,
            AsociarVersionAPeriodoServicio asociar) {}

    private static void conNormativa(Consumer<Normativa> prueba) {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            RepositorioVersionesJdbc repositorio = new RepositorioVersionesJdbc(jdbc);
            RegistroDeCambiosJdbc registro = new RegistroDeCambiosJdbc(jdbc);
            TransaccionSpring transaccion =
                    new TransaccionSpring(
                            new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
            Clock reloj = Clock.systemUTC();
            prueba.accept(
                    new Normativa(
                            CargasDeNormativa.aplicador(
                                    transaccion,
                                    repositorio,
                                    new CrearVersionServicio(
                                            repositorio, registro, transaccion, reloj),
                                    new EditarVersionServicio(
                                            repositorio, registro, transaccion, reloj)),
                            repositorio,
                            new AsociarVersionAPeriodoServicio(
                                    repositorio, registro, transaccion, reloj)));
        }
    }

    private static LoteDeCarga politica() {
        return LotesDePrueba.lote("politica_carga_horaria", "07_politica_carga_horaria.csv");
    }

    private static ResultadoDelLote confirmar(AplicadorDeCargas aplicador, LoteDeCarga lote) {
        return aplicador.aplicar(lote, aplicador.previsualizar(lote).huella(), QUIEN);
    }

    private static FilaCargaHoraria fila22(VersionNormativa version) {
        return version.tabla().filas().stream()
                .filter(fila -> fila.perfil().equals("DOCENTE_INVESTIGACION_VINCULACION"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void laCarga07DePruebaFrenteALaVersion1_tieneUnaFilaQueCambiaEnDosCampos() {
        conNormativa(
                normativa -> {
                    VistaPreviaDelLote vista = normativa.aplicador().previsualizar(politica());

                    assertThat(vista.cuantos(TipoDeRegistro.IGUAL)).isEqualTo(21);
                    RegistroDeLaVista cambia =
                            vista.registros().stream()
                                    .filter(r -> r.tipo() == TipoDeRegistro.CAMBIA)
                                    .findFirst()
                                    .orElseThrow();
                    assertThat(cambia.clave())
                            .isEqualTo(
                                    "SENESCYT-2024-03 · TODOS · DOCENTE_INVESTIGACION_VINCULACION");
                    assertThat(cambia.cambios())
                            .containsExactly(
                                    new CambioDeCampo("horas_otras_docencia", "", "0"),
                                    new CambioDeCampo("horas_gestion", "", "0"));
                });
    }

    @Test
    void laPoliticaPrecargada_cuentaComoDatosDeLaCarga07() {
        conNormativa(
                normativa ->
                        assertThat(normativa.aplicador().tieneDatos("politica_carga_horaria"))
                                .isTrue());
    }

    @Test
    void conLaVersion1SinPeriodos_confirmarLaEditaYSubidaOtraVezEsIgual() {
        conNormativa(
                normativa -> {
                    assertThat(confirmar(normativa.aplicador(), politica()))
                            .isEqualTo(new ResultadoDelLote(0, 1, 0));

                    VersionNormativa version = normativa.repositorio().buscar(1).orElseThrow();
                    assertThat(fila22(version).horasOtrasDocencia()).isZero();
                    assertThat(fila22(version).horasGestion()).isZero();
                    assertThat(
                                    normativa
                                            .aplicador()
                                            .previsualizar(politica())
                                            .cuantos(TipoDeRegistro.IGUAL))
                            .isEqualTo(22);
                });
    }

    @Test
    void conLaVersion1EnUso_laCargaSeRechazaConVersionEnUsoYNoCambiaNada() {
        conNormativa(
                normativa -> {
                    normativa.asociar().asociar("2026-I", 1, QUIEN);

                    assertThatThrownBy(() -> confirmar(normativa.aplicador(), politica()))
                            .isInstanceOfSatisfying(
                                    RechazoDelLote.class,
                                    rechazo ->
                                            assertThat(rechazo.getCause())
                                                    .isInstanceOf(VersionEnUso.class));
                    assertThat(
                                    fila22(normativa.repositorio().buscar(1).orElseThrow())
                                            .horasGestion())
                            .isNull();
                });
    }

    @Test
    void unCodigoDeVersionNuevo_creaLaVersionCopiandoLaUltimaConSusReglas() {
        conNormativa(
                normativa -> {
                    List<FilaDelLote> filas = new ArrayList<>();
                    for (FilaDelLote fila : politica().filas()) {
                        filas.add(
                                LotesDePrueba.cambiar(
                                        fila, "codigo_version_politica", "SENESCYT-2026-01"));
                    }
                    LoteDeCarga nueva = LotesDePrueba.con(politica(), filas);

                    assertThat(confirmar(normativa.aplicador(), nueva))
                            .isEqualTo(new ResultadoDelLote(22, 0, 0));

                    VersionNormativa version = normativa.repositorio().buscar(2).orElseThrow();
                    assertThat(version.codigo()).isEqualTo("SENESCYT-2026-01");
                    assertThat(version.copiadaDe()).isOne();
                    assertThat(version.reglas())
                            .isEqualTo(normativa.repositorio().buscar(1).orElseThrow().reglas());
                    assertThat(fila22(version).horasGestion()).isZero();
                });
    }

    @Test
    void unaFilaDeMenos_esUnaBajaQueNormativaRechazaAlConfirmar() {
        conNormativa(
                normativa -> {
                    List<FilaDelLote> filas = new ArrayList<>(politica().filas());
                    filas.removeLast();
                    LoteDeCarga incompleta = LotesDePrueba.con(politica(), filas);

                    assertThat(
                                    normativa
                                            .aplicador()
                                            .previsualizar(incompleta)
                                            .cuantos(TipoDeRegistro.BAJA))
                            .isOne();
                    assertThatThrownBy(() -> confirmar(normativa.aplicador(), incompleta))
                            .isInstanceOf(RechazoDelLote.class);
                });
    }

    @Test
    void laReferenciaYLaObservacionNoCuentanComoCambio() {
        conNormativa(
                normativa -> {
                    confirmar(normativa.aplicador(), politica());
                    List<FilaDelLote> filas = new ArrayList<>();
                    for (FilaDelLote fila : politica().filas()) {
                        filas.add(
                                LotesDePrueba.cambiar(
                                        LotesDePrueba.cambiar(fila, "observacion", "Otra nota"),
                                        "referencia_normativa",
                                        "POL 7"));
                    }

                    assertThat(
                                    normativa
                                            .aplicador()
                                            .previsualizar(LotesDePrueba.con(politica(), filas))
                                            .cuantos(TipoDeRegistro.IGUAL))
                            .isEqualTo(22);
                });
    }
}
