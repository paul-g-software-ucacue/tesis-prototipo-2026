package ec.mdd.roles;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.roles.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioAsignacionesDeRolJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioRolesJdbc;
import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.Funcionalidad;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.infraestructura.RolesFlywayConfig;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Los repositorios de Roles contra PostgreSQL real, con las migraciones V1 a V3. */
class RepositoriosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static JdbcClient jdbc;
    private static RepositorioRolesJdbc roles;
    private static RepositorioAsignacionesDeRolJdbc asignaciones;
    private static RegistroDeCambiosJdbc registro;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new RolesFlywayConfig().flywayRoles(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        TransactionTemplate transacciones =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        roles = new RepositorioRolesJdbc(jdbc, transacciones);
        asignaciones = new RepositorioAsignacionesDeRolJdbc(jdbc, transacciones);
        registro = new RegistroDeCambiosJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLasFuncionalidades_entoncesVienenEnSuOrdenConSusTipos() {
        List<Funcionalidad> funcionalidades = roles.funcionalidades().funcionalidades();

        assertThat(funcionalidades).hasSize(16);
        assertThat(funcionalidades.getFirst().codigo()).isEqualTo("USUARIOS_ROLES");
        assertThat(funcionalidades.getLast().codigo()).isEqualTo("MI_DISTRIBUTIVO");
        assertThat(funcionalidades.get(10).tipos())
                .containsExactlyInAnyOrder(
                        TipoDePermiso.LECTURA,
                        TipoDePermiso.ESCRITURA,
                        TipoDePermiso.APROBACION,
                        TipoDePermiso.FIRMA);
    }

    @Test
    void dadoUnRolNuevo_cuandoSeGuardaSeEditaYSeBorra_entoncesSeLeeIgualCadaVez() {
        String nombre = "Auditor " + sufijo();
        Rol rol =
                Rol.nuevo(
                        nombre,
                        "Revisa cargas.",
                        List.of(
                                new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO),
                                new Permiso("OFERTA", TipoDePermiso.LECTURA, Alcance.PROPIO)));

        roles.guardar(rol, AHORA);
        assertThat(roles.buscar(rol.identificador())).contains(rol);

        Rol editado =
                rol.editado(
                        nombre + " editado",
                        "Revisa docentes.",
                        List.of(new Permiso("DOCENTES", TipoDePermiso.LECTURA, Alcance.TODO)));
        roles.guardar(editado, AHORA.plusSeconds(1));
        assertThat(roles.buscar(rol.identificador())).contains(editado);
        assertThat(roles.todos()).contains(editado).doesNotContain(rol);

        roles.borrar(rol.identificador());
        assertThat(roles.buscar(rol.identificador())).isEmpty();
    }

    @Test
    void dadoLosRolesDeD16_cuandoSeLeen_entoncesSonPredefinidosYEstanOrdenadosPorNombre() {
        List<Rol> todos = roles.todos();

        assertThat(roles.buscar("DOCENTE"))
                .hasValueSatisfying(rol -> assertThat(rol.predefinido()).isTrue());
        assertThat(todos).extracting(Rol::nombre).isSortedAccordingTo(String::compareTo);
    }

    @Test
    void dadoUnUsuario_cuandoSeLeAsignanYQuitanRoles_entoncesSeLeenYSeCuentan() {
        String usuario = "usuario." + sufijo();
        int docentesAntes = asignaciones.usuariosPorRol().getOrDefault("DOCENTE", 0);

        asignaciones.reemplazar(usuario, List.of("DOCENTE", "RECTOR"), "admin.demo", AHORA);
        assertThat(asignaciones.rolesDe(usuario)).containsExactly("DOCENTE", "RECTOR");
        assertThat(asignaciones.todas()).containsEntry(usuario, List.of("DOCENTE", "RECTOR"));
        assertThat(asignaciones.usuariosPorRol()).containsEntry("DOCENTE", docentesAntes + 1);

        asignaciones.reemplazar(usuario, List.of("RECTOR"), "admin.demo", AHORA);
        assertThat(asignaciones.rolesDe(usuario)).containsExactly("RECTOR");

        asignaciones.reemplazar(usuario, List.of(), "admin.demo", AHORA);
        assertThat(asignaciones.rolesDe(usuario)).isEmpty();
        assertThat(asignaciones.todas()).doesNotContainKey(usuario);
    }

    @Test
    void dadoUnUsuario_cuandoSeMarcaInicializadoDosVeces_entoncesSoloLaPrimeraCuenta() {
        String usuario = "usuario." + sufijo();

        assertThat(asignaciones.marcarInicializado(usuario, AHORA)).isTrue();
        assertThat(asignaciones.marcarInicializado(usuario, AHORA)).isFalse();
    }

    @Test
    void dadoUnCambio_cuandoSeRegistra_entoncesQuedaConSuDetalleEnJson() {
        String usuario = "admin." + sufijo();

        registro.registrar(
                new CambioDeRoles(
                        AHORA,
                        usuario,
                        CambioDeRoles.Accion.ASIGNAR_ROL,
                        Map.of("usuario", "docente.demo", "roles", List.of("DOCENTE", "RECTOR"))));

        Map<String, Object> fila =
                jdbc.sql(
                                "select accion, detalle ->> 'usuario' as afectado,"
                                        + " jsonb_array_length(detalle -> 'roles') as roles"
                                        + " from roles.registro_cambio where usuario = :usuario")
                        .param("usuario", usuario)
                        .query()
                        .singleRow();
        assertThat(fila)
                .containsEntry("accion", "ASIGNAR_ROL")
                .containsEntry("afectado", "docente.demo")
                .containsEntry("roles", 2);
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
