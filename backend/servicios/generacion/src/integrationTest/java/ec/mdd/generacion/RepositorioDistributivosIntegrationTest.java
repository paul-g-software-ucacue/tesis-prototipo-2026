package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** El distributivo en PostgreSQL, con las restricciones de la migración V3 (MD-01, D-19). */
class RepositorioDistributivosIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-10-31T15:00:00Z");
    private static final String QUIEN = "coordinacion.demo";
    private static final String DOCENTE = "0109990010";

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static RepositorioDistributivosJdbc repositorio;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioDistributivosJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnDistributivoNuevo_cuandoSeGuarda_entoncesSeLeeConSusParciales() {
        String periodo = periodo();

        VersionDistributivo guardada = crear(periodo, "AGRO", "TDS");

        VersionDistributivo leida = repositorio.vigenteDe(periodo).orElseThrow();
        assertThat(leida.id()).isEqualTo(guardada.id());
        assertThat(leida.numero()).isEqualTo(1);
        assertThat(leida.estado()).isEqualTo(guardada.estado());
        assertThat(leida.parciales()).containsExactlyElementsOf(guardada.parciales());
        assertThat(leida.propuestas()).isEmpty();
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeLee_entoncesVacio() {
        assertThat(repositorio.vigenteDe(periodo())).isEmpty();
    }

    @Test
    void dadoUnPeriodoConDistributivo_cuandoSeCreaOtro_entoncesDistributivoRepetido() {
        String periodo = periodo();
        crear(periodo, "TDS");

        assertThatThrownBy(() -> crear(periodo, "TDS")).isInstanceOf(DistributivoRepetido.class);
    }

    @Test
    void dadaUnaPropuesta_cuandoSeGuarda_entoncesSeLeeEntera() {
        String periodo = periodo();
        VersionDistributivo version = crear(periodo, "TDS");
        Propuesta propuesta = propuesta("A", DOCENTE, excepcion());

        repositorio.guardarPropuesta(version.id(), propuesta);

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas())
                .containsExactly(propuesta);
    }

    @Test
    void dadaUnaPropuestaDelParalelo_cuandoSeGuardaOtra_entoncesLaReemplaza() {
        String periodo = periodo();
        VersionDistributivo version = crear(periodo, "TDS");
        repositorio.guardarPropuesta(version.id(), propuesta("A", DOCENTE, excepcion()));
        Propuesta otra = propuesta("A", "0109990020", afinidad());

        repositorio.guardarPropuesta(version.id(), otra);

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas()).containsExactly(otra);
    }

    @Test
    void dadosDosParalelos_cuandoSeGuardan_entoncesTienenCadaUnoSuPropuesta() {
        String periodo = periodo();
        VersionDistributivo version = crear(periodo, "TDS");

        repositorio.guardarPropuesta(version.id(), propuesta("A", DOCENTE, afinidad()));
        repositorio.guardarPropuesta(version.id(), propuesta("B", DOCENTE, afinidad()));

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas()).hasSize(2);
    }

    @Test
    void dadaUnaPropuesta_cuandoSeQuita_entoncesDesaparece() {
        String periodo = periodo();
        VersionDistributivo version = crear(periodo, "TDS");
        repositorio.guardarPropuesta(version.id(), propuesta("A", DOCENTE, afinidad()));

        assertThat(repositorio.quitarPropuesta(version.id(), "POO-204", "A")).isTrue();

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas()).isEmpty();
    }

    @Test
    void dadoQueNoHayPropuesta_cuandoSeQuita_entoncesDiceQueNoExistia() {
        VersionDistributivo version = crear(periodo(), "TDS");

        assertThat(repositorio.quitarPropuesta(version.id(), "POO-204", "A")).isFalse();
    }

    @Test
    void dadaUnaCarreraQueLlegaDespues_cuandoSeAgregaSuParcial_entoncesAdmitePropuestas() {
        String periodo = periodo();
        VersionDistributivo version = crear(periodo, "AGRO");

        repositorio.agregarParcial(version.id(), ParcialCarrera.nuevo("TDS"));
        repositorio.agregarParcial(version.id(), ParcialCarrera.nuevo("TDS"));
        repositorio.guardarPropuesta(version.id(), propuesta("A", DOCENTE, afinidad()));

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().parciales())
                .extracting(ParcialCarrera::carrera)
                .containsExactly("AGRO", "TDS");
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeGuardaUnaPropuesta_entoncesLaBaseLaRechaza() {
        VersionDistributivo version = crear(periodo(), "AGRO");
        Propuesta sinParcial = propuesta("A", DOCENTE, afinidad());

        assertThatThrownBy(() -> repositorio.guardarPropuesta(version.id(), sinParcial))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dadaUnaExcepcionSinTexto_cuandoSeGuarda_entoncesLaBaseLaRechaza() {
        VersionDistributivo version = crear(periodo(), "TDS");
        Propuesta sinTexto =
                propuesta("A", DOCENTE, new Justificacion(TipoDeJustificacion.EXCEPCION, null));

        assertThatThrownBy(() -> repositorio.guardarPropuesta(version.id(), sinTexto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dadaUnaAfinidadConTexto_cuandoSeGuarda_entoncesLaBaseLaRechaza() {
        VersionDistributivo version = crear(periodo(), "TDS");
        Propuesta conTexto =
                propuesta(
                        "A",
                        DOCENTE,
                        new Justificacion(TipoDeJustificacion.AFINIDAD, "Un texto cualquiera"));

        assertThatThrownBy(() -> repositorio.guardarPropuesta(version.id(), conTexto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void dadoUnCambio_cuandoSeRegistra_entoncesQuedaConSuDetalleEnJson() {
        String usuario = "registro-" + UUID.randomUUID().toString().substring(0, 8);

        new RegistroDeCambiosJdbc(jdbc)
                .registrar(
                        new CambioDeDistributivo(
                                MOMENTO,
                                usuario,
                                CambioDeDistributivo.Accion.PROPUESTA_QUITADA,
                                Map.of("paralelo", "A")));

        assertThat(
                        jdbc.sql(
                                        "SELECT detalle ->> 'paralelo' FROM"
                                                + " generacion.registro_cambio WHERE usuario = ?")
                                .param(usuario)
                                .query(String.class)
                                .single())
                .isEqualTo("A");
    }

    private static VersionDistributivo crear(String periodo, String... carreras) {
        VersionDistributivo version =
                VersionDistributivo.primera(UUID.randomUUID(), List.of(carreras));
        repositorio.crear(periodo, version, "vicerrectorado.demo", MOMENTO);
        return version;
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Justificacion afinidad() {
        return new Justificacion(TipoDeJustificacion.AFINIDAD, null);
    }

    private static Justificacion excepcion() {
        return new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó la materia tres años");
    }

    private static Propuesta propuesta(
            String paralelo, String docente, Justificacion justificacion) {
        return new Propuesta(
                "POO-204", paralelo, "TDS", docente, "PEREZ LUIS", justificacion, MOMENTO, QUIEN);
    }
}
