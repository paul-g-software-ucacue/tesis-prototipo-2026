package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.adaptadores.salida.RepositorioBitacoraJdbc;
import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.EntradaConsultada;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La consulta de la bitácora contra PostgreSQL real (F17-H02): cada filtro y su combinación, el
 * orden, el corte, el identificador anidado y numérico, y 10 000 entradas en menos de 2 s (FR-001 a
 * FR-004, SC-003, research §2 a §5).
 *
 * <p>Las entradas se insertan con el superusuario para fijar {@code recibida_en}; el proceso solo
 * lee. Cada prueba acota por su periodo o por su usuario, porque la tabla no se puede vaciar.
 */
class RepositorioBitacoraConsultaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String ROL = "identidad_prueba";
    private static final String CLAVE = "clave-de-prueba";
    private static final Instant AHORA = Instant.parse("2026-11-09T12:00:00Z");
    private static final int MASIVAS = 10_000;

    private static JdbcClient comoSuperusuario;
    private static RepositorioBitacoraJdbc repositorio;

    @BeforeAll
    static void migrarYCargar() {
        POSTGRES.start();
        DataSource superusuario =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        comoSuperusuario = JdbcClient.create(superusuario);
        comoSuperusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        comoSuperusuario.sql("create schema usuarios authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new UsuariosFlywayConfig().flywayUsuarios(proceso).migrate();
        repositorio = new RepositorioBitacoraJdbc(JdbcClient.create(proceso));
        cargarMasivas();
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoUsuarioYFecha_cuandoSeConsulta_entoncesSoloTraeLasDeEseUsuarioEseDia() {
        String periodo = "P-USUARIO";
        UUID suya = insertar("coordinacion.demo", "2026-11-07T15:00:00Z", periodo);
        insertar("coordinacion.demo2", "2026-11-07T15:00:00Z", periodo);
        insertar("coordinacion.demo", "2026-11-06T15:00:00Z", periodo);

        PaginaDeEntradas pagina =
                consultar(
                        filtro(
                                LocalDate.parse("2026-11-07"),
                                LocalDate.parse("2026-11-07"),
                                "coordinacion.demo",
                                null,
                                null,
                                null,
                                periodo));

        assertThat(pagina.entradas()).extracting(EntradaConsultada::id).containsExactly(suya);
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    void dadoElBordeDelDia_cuandoSeFiltraPorFecha_entoncesUsaLaHoraDeEcuador() {
        String periodo = "P-BORDE";
        UUID ultimoMinuto = insertar("sistema", "2026-11-08T04:59:00Z", periodo);
        insertar("sistema", "2026-11-08T05:00:00Z", periodo);
        UUID primerMinuto = insertar("sistema", "2026-11-07T05:00:00Z", periodo);
        insertar("sistema", "2026-11-07T04:59:59Z", periodo);

        LocalDate dia = LocalDate.parse("2026-11-07");
        PaginaDeEntradas pagina = consultar(filtro(dia, dia, null, null, null, null, periodo));

        assertThat(pagina.entradas())
                .extracting(EntradaConsultada::id)
                .containsExactly(ultimoMinuto, primerMinuto);
    }

    @Test
    void dadoServicioYAccion_cuandoSeConsulta_entoncesSoloTraeEseTipoDeCambio() {
        String periodo = "P-TIPO";
        UUID buscada =
                insertar(
                        UUID.randomUUID(),
                        "generacion",
                        "ACTIVIDAD_GUARDADA",
                        "sistema",
                        "2026-11-07T15:00:00Z",
                        periodo,
                        "{\"actividad\": \"140-b\"}",
                        AHORA.minusSeconds(60));
        insertar(
                UUID.randomUUID(),
                "generacion",
                "ACTIVIDAD_QUITADA",
                "sistema",
                "2026-11-07T15:00:00Z",
                periodo,
                "{\"actividad\": \"140-b\"}",
                AHORA.minusSeconds(60));
        insertar(
                UUID.randomUUID(),
                "academico",
                "ACTIVIDAD_GUARDADA",
                "sistema",
                "2026-11-07T15:00:00Z",
                periodo,
                "{\"codigo\": \"140-b\"}",
                AHORA.minusSeconds(60));

        PaginaDeEntradas soloServicio =
                consultar(filtro(null, null, null, "generacion", null, null, periodo));
        PaginaDeEntradas servicioYAccion =
                consultar(
                        filtro(
                                null,
                                null,
                                null,
                                "generacion",
                                "ACTIVIDAD_GUARDADA",
                                null,
                                periodo));

        assertThat(soloServicio.total()).isEqualTo(2);
        assertThat(servicioYAccion.entradas())
                .extracting(EntradaConsultada::id)
                .containsExactly(buscada);
    }

    @Test
    void dadoUnIdentificadorAnidadoEnUnaLista_cuandoSeConsulta_entoncesLoEncuentra() {
        String periodo = "P-ANIDADO";
        UUID anidada =
                insertarObjeto(
                        periodo,
                        "{\"identificacion\": \"0102030405\", \"cambios\": [{\"campo\": \"dedicacion\","
                                + " \"cargo\": \"DOC/0102030405\"}]}");
        insertarObjeto(periodo, "{\"identificacion\": \"0102030406\"}");

        PaginaDeEntradas pagina =
                consultar(filtro(null, null, null, null, null, "DOC/0102030405", periodo));

        assertThat(pagina.entradas()).extracting(EntradaConsultada::id).containsExactly(anidada);
    }

    @Test
    void dadoUnIdentificadorNumerico_cuandoSeConsulta_entoncesCoincideConElNumeroDelObjeto() {
        String periodo = "P-NUMERO";
        UUID version = insertarObjeto(periodo, "{\"numero\": 3, \"periodo\": \"P-NUMERO\"}");
        insertarObjeto(periodo, "{\"numero\": 33}");

        PaginaDeEntradas pagina = consultar(filtro(null, null, null, null, null, "3", periodo));

        assertThat(pagina.entradas()).extracting(EntradaConsultada::id).containsExactly(version);
    }

    @Test
    void dadoUnIdentificadorSoloParecido_cuandoSeConsulta_entoncesNoLoTrae() {
        String periodo = "P-PARECIDO";
        insertarObjeto(periodo, "{\"actividad\": \"140-bb\", \"proyecto\": \"x140-b\"}");

        PaginaDeEntradas pagina = consultar(filtro(null, null, null, null, null, "140-b", periodo));

        assertThat(pagina.total()).isZero();
        assertThat(pagina.entradas()).isEmpty();
    }

    @Test
    void dadoValoresConComillasYComodines_cuandoSeConsulta_entoncesNoAmplianNiRompenLaConsulta() {
        String periodo = "P-INYECCION";
        insertarObjeto(periodo, "{\"actividad\": \"140-b\"}");

        PaginaDeEntradas comilla =
                consultar(filtro(null, null, "x' or '1'='1", null, null, null, periodo));
        PaginaDeEntradas comodin = consultar(filtro(null, null, "%", null, null, null, periodo));

        assertThat(comilla.total()).isZero();
        assertThat(comodin.total()).isZero();
    }

    @Test
    void dadoVariasEntradas_cuandoSeConsulta_entoncesLasOrdenaPorMomentoYDesempataPorId() {
        String periodo = "P-ORDEN";
        UUID menor = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID mayor = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID antigua = insertar("sistema", "2026-11-01T10:00:00Z", periodo);
        insertar(
                menor,
                "generacion",
                "ACTIVIDAD_GUARDADA",
                "sistema",
                "2026-11-05T10:00:00Z",
                periodo,
                "{}",
                AHORA.minusSeconds(60));
        insertar(
                mayor,
                "generacion",
                "ACTIVIDAD_GUARDADA",
                "sistema",
                "2026-11-05T10:00:00Z",
                periodo,
                "{}",
                AHORA.minusSeconds(60));

        PaginaDeEntradas pagina = consultar(filtro(null, null, null, null, null, null, periodo));

        assertThat(pagina.entradas())
                .extracting(EntradaConsultada::id)
                .containsExactly(mayor, menor, antigua);
    }

    @Test
    void dadoUnaEntradaRecibidaDespuesDelCorte_cuandoSeConsulta_entoncesNoCuentaAunqueSeaAntigua() {
        String periodo = "P-CORTE";
        Instant corte = AHORA.minusSeconds(30);
        UUID antes =
                insertar(
                        UUID.randomUUID(),
                        "generacion",
                        "ACTIVIDAD_GUARDADA",
                        "sistema",
                        "2026-11-07T10:00:00Z",
                        periodo,
                        "{}",
                        corte.minusSeconds(1));
        insertar(
                UUID.randomUUID(),
                "generacion",
                "ACTIVIDAD_GUARDADA",
                "sistema",
                "2026-11-01T10:00:00Z",
                periodo,
                "{}",
                corte.plusSeconds(1));

        PaginaDeEntradas pagina =
                repositorio.consultar(
                        filtro(null, null, null, null, null, null, periodo),
                        PaginaPedida.de(1, 50, corte, AHORA));

        assertThat(pagina.entradas()).extracting(EntradaConsultada::id).containsExactly(antes);
        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.corte()).isEqualTo(corte);
    }

    @Test
    void dadoVariasPaginas_cuandoSePideUnaMasAllaDeLaUltima_entoncesVieneVaciaConElTotal() {
        String periodo = "P-PAGINAS";
        for (int i = 0; i < 5; i++) {
            insertar("sistema", "2026-11-07T1" + i + ":00:00Z", periodo);
        }
        FiltroDeBitacora filtro = filtro(null, null, null, null, null, null, periodo);

        PaginaDeEntradas segunda =
                repositorio.consultar(filtro, PaginaPedida.de(2, 2, null, AHORA));
        PaginaDeEntradas cuarta = repositorio.consultar(filtro, PaginaPedida.de(4, 2, null, AHORA));

        assertThat(segunda.entradas()).hasSize(2);
        assertThat(segunda.entradas().getFirst().momento())
                .isEqualTo(Instant.parse("2026-11-07T12:00:00Z"));
        assertThat(segunda.total()).isEqualTo(5);
        assertThat(cuarta.entradas()).isEmpty();
        assertThat(cuarta.total()).isEqualTo(5);
    }

    @Test
    void dadoUnaEntradaQueCrea_cuandoSeConsulta_entoncesTraeSuObjetoSinAnteriorYConNuevo() {
        String periodo = "P-CREA";
        UUID id = UUID.randomUUID();
        comoSuperusuario
                .sql(
                        "insert into usuarios.bitacora (id_evento, servicio, accion, usuario,"
                                + " momento, periodo, objeto, nuevo, recibida_en) values (:id,"
                                + " 'docentes', 'DOCENTE_REGISTRADO', 'datos.demo', :momento,"
                                + " :periodo, cast(:objeto as jsonb), cast(:nuevo as jsonb),"
                                + " :recibida)")
                .param("id", id)
                .param("momento", Timestamp.from(Instant.parse("2026-11-07T15:00:00Z")))
                .param("periodo", periodo)
                .param(
                        "objeto",
                        "{\"identificacion\": \"0102030405\", \"camposPersonales\": [\"telefono\"]}")
                .param("nuevo", "{\"dedicacion\": \"TC\"}")
                .param("recibida", Timestamp.from(AHORA.minusSeconds(60)))
                .update();

        EntradaConsultada entrada =
                consultar(filtro(null, null, null, null, null, null, periodo))
                        .entradas()
                        .getFirst();

        assertThat(entrada.id()).isEqualTo(id);
        assertThat(entrada.servicio()).isEqualTo("docentes");
        assertThat(entrada.accion()).isEqualTo("DOCENTE_REGISTRADO");
        assertThat(entrada.usuario()).isEqualTo("datos.demo");
        assertThat(entrada.periodo()).contains(periodo);
        assertThat(entrada.objeto())
                .containsEntry("identificacion", "0102030405")
                .containsEntry("camposPersonales", List.of("telefono"));
        assertThat(entrada.anterior()).isEmpty();
        assertThat(entrada.nuevo()).contains(Map.of("dedicacion", "TC"));
    }

    @Test
    void dadoLaBitacora_cuandoSePidenLasAcciones_entoncesTraeLosParesDistintosOrdenados() {
        insertar(
                UUID.randomUUID(),
                "roles",
                "EDITAR_ROL",
                "admin.demo",
                "2026-11-07T15:00:00Z",
                null,
                "{}",
                AHORA.minusSeconds(60));
        insertar(
                UUID.randomUUID(),
                "roles",
                "EDITAR_ROL",
                "admin.demo",
                "2026-11-07T16:00:00Z",
                null,
                "{}",
                AHORA.minusSeconds(60));

        List<AccionConocida> acciones = repositorio.acciones();

        assertThat(acciones).containsOnlyOnce(new AccionConocida("roles", "EDITAR_ROL"));
        assertThat(acciones)
                .isSortedAccordingTo(
                        java.util.Comparator.comparing(AccionConocida::servicio)
                                .thenComparing(AccionConocida::accion));
    }

    @Test
    void dadoDiezMilEntradas_cuandoSeFiltra_entoncesLaPrimeraPaginaLlegaEnMenosDeDosSegundos() {
        LocalDate dia = LocalDate.parse("2025-06-01");
        FiltroDeBitacora porUsuarioYFecha =
                filtro(dia, dia.plusDays(30), "carga.demo", null, null, null, null);
        FiltroDeBitacora porIdentificador =
                filtro(null, null, null, null, null, "CODIGO-9999", null);
        repositorio.consultar(porUsuarioYFecha, PaginaPedida.de(1, 50, null, AHORA));

        long inicio = System.nanoTime();
        PaginaDeEntradas porUsuario =
                repositorio.consultar(porUsuarioYFecha, PaginaPedida.de(1, 50, null, AHORA));
        Duration primera = Duration.ofNanos(System.nanoTime() - inicio);
        inicio = System.nanoTime();
        PaginaDeEntradas porCodigo =
                repositorio.consultar(porIdentificador, PaginaPedida.de(1, 50, null, AHORA));
        Duration segunda = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(porUsuario.entradas()).hasSize(50);
        assertThat(porUsuario.total()).isPositive();
        assertThat(porCodigo.total()).isEqualTo(1);
        assertThat(primera).isLessThan(Duration.ofSeconds(2));
        assertThat(segunda).isLessThan(Duration.ofSeconds(2));
    }

    // 10 000 entradas de 2025, de un usuario propio, con un objeto como el de una actividad.
    private static void cargarMasivas() {
        comoSuperusuario
                .sql(
                        "insert into usuarios.bitacora (id_evento, servicio, accion, usuario,"
                                + " momento, periodo, objeto, anterior, nuevo, recibida_en)"
                                + " select gen_random_uuid(), 'generacion', 'ACTIVIDAD_GUARDADA',"
                                + " 'carga.demo', timestamptz '2025-06-01 10:00:00+00' + n *"
                                + " interval '5 minutes', '2025-I', jsonb_build_object('periodo',"
                                + " '2025-I', 'docente', lpad(n::text, 10, '0'), 'actividad',"
                                + " 'CODIGO-' || n), jsonb_build_object('horas', '4'),"
                                + " jsonb_build_object('horas', '6'), :recibida from"
                                + " generate_series(0, :ultima) as n")
                .param("recibida", Timestamp.from(AHORA.minusSeconds(3600)))
                .param("ultima", MASIVAS - 1)
                .update();
    }

    private static FiltroDeBitacora filtro(
            LocalDate desde,
            LocalDate hasta,
            String usuario,
            String servicio,
            String accion,
            String identificador,
            String periodo) {
        return FiltroDeBitacora.de(desde, hasta, usuario, servicio, accion, identificador, periodo);
    }

    private static PaginaDeEntradas consultar(FiltroDeBitacora filtro) {
        return repositorio.consultar(filtro, PaginaPedida.de(1, 50, null, AHORA));
    }

    private static UUID insertar(String usuario, String momento, String periodo) {
        return insertar(
                UUID.randomUUID(),
                "generacion",
                "ACTIVIDAD_GUARDADA",
                usuario,
                momento,
                periodo,
                "{\"actividad\": \"140-b\"}",
                AHORA.minusSeconds(60));
    }

    private static UUID insertarObjeto(String periodo, String objeto) {
        return insertar(
                UUID.randomUUID(),
                "generacion",
                "ACTIVIDAD_GUARDADA",
                "sistema",
                "2026-11-07T15:00:00Z",
                periodo,
                objeto,
                AHORA.minusSeconds(60));
    }

    private static UUID insertar(
            UUID id,
            String servicio,
            String accion,
            String usuario,
            String momento,
            String periodo,
            String objeto,
            Instant recibida) {
        comoSuperusuario
                .sql(
                        "insert into usuarios.bitacora (id_evento, servicio, accion, usuario,"
                                + " momento, periodo, objeto, anterior, nuevo, recibida_en) values"
                                + " (:id, :servicio, :accion, :usuario, :momento, :periodo,"
                                + " cast(:objeto as jsonb), '{\"horas\": \"4\"}',"
                                + " '{\"horas\": \"6\"}', :recibida)")
                .param("id", id)
                .param("servicio", servicio)
                .param("accion", accion)
                .param("usuario", usuario)
                .param("momento", Timestamp.from(Instant.parse(momento)))
                .param("periodo", periodo)
                .param("objeto", objeto)
                .param("recibida", Timestamp.from(recibida))
                .update();
        return id;
    }
}
