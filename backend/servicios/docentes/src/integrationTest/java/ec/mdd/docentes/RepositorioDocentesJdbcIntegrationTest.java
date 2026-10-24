package ec.mdd.docentes;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.docentes.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.docentes.adaptadores.salida.RepositorioDocentesJdbc;
import ec.mdd.docentes.dominio.CampoConocimiento;
import ec.mdd.docentes.dominio.CategoriaDeEscalafon;
import ec.mdd.docentes.dominio.CondicionEspecial;
import ec.mdd.docentes.dominio.Curso;
import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Dedicacion;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.ExperienciaLaboral;
import ec.mdd.docentes.dominio.FichaAcademica;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.dominio.TipoDeIdentificacion;
import ec.mdd.docentes.dominio.TipoDeTituloDeCuartoNivel;
import ec.mdd.docentes.dominio.TipoPersonal;
import ec.mdd.docentes.dominio.TituloCuartoNivel;
import ec.mdd.docentes.dominio.TituloTercerNivel;
import ec.mdd.docentes.infraestructura.DocentesFlywayConfig;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
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

/** Los docentes se guardan y se leen igual, campo por campo, con su ficha (F03-H04, T009). */
class RepositorioDocentesJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final Year ANIO = Year.of(2026);

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioDocentesJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioDocentesJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM docentes.docente").update();
        jdbc.sql("DELETE FROM docentes.registro_cambio").update();
    }

    @Test
    void dadoUnDocenteConLaFichaCompleta_cuandoSeGuardaYSeLee_entoncesEsIgual() {
        Docente completo = docente("0109990010", "PERALTA", "juan@prueba.edu.ec", fichaCompleta());

        repositorio.registrar(completo, "datos.demo", AHORA);

        assertThat(repositorio.buscar("0109990010")).contains(completo);
        assertThat(repositorio.buscar("0109990028")).isEmpty();
    }

    @Test
    void dadoUnDocenteConLaFichaMinima_cuandoSeGuardaYSeLee_entoncesEsIgual() {
        Docente minimo =
                docente(
                        "AB12345",
                        "ZAMBRANO",
                        "ana@prueba.edu.ec",
                        new FichaAcademica(tercerNivel(), null, null, null, null));

        repositorio.registrar(minimo, "datos.demo", AHORA);

        assertThat(repositorio.buscar("AB12345")).contains(minimo);
    }

    @Test
    void dadosVariosDocentes_cuandoSeListan_entoncesVanPorApellidosYNombres() {
        repositorio.registrar(
                docente("0109990028", "ZAMBRANO", "z@prueba.edu.ec", fichaCompleta()),
                "datos.demo",
                AHORA);
        repositorio.registrar(
                docente("0109990010", "ABAD", "a@prueba.edu.ec", fichaCompleta()),
                "datos.demo",
                AHORA);

        assertThat(repositorio.todos())
                .extracting(docente -> docente.datos().primerApellido())
                .containsExactly("ABAD", "ZAMBRANO");
    }

    @Test
    void dadoUnDocente_cuandoSeGuardaCorregidoYEnOtroEstado_entoncesCambiaCadaDato() {
        Docente original = docente("0109990010", "PERALTA", "juan@prueba.edu.ec", fichaCompleta());
        repositorio.registrar(original, "datos.demo", AHORA);
        DatosDelDocente nuevos =
                new DatosDelDocente(
                        "PERALTA",
                        null,
                        "JUAN",
                        null,
                        "otro@prueba.edu.ec",
                        null,
                        TipoPersonal.TITULAR,
                        CategoriaDeEscalafon.AGREGADO_2,
                        Dedicacion.MT,
                        20,
                        LocalDate.of(2018, 1, 2),
                        LocalDate.of(2018, 1, 2),
                        null,
                        CondicionEspecial.CUIDADOR_SUSTITUTO,
                        "550411C02-P-0301",
                        new FichaAcademica(tercerNivel(), null, null, null, List.of("B1")));
        Docente corregido =
                original.corregido(nuevos, ANIO).docente().conEstado(EstadoLaboral.LICENCIA);

        repositorio.guardar(corregido);

        assertThat(repositorio.buscar("0109990010")).contains(corregido);
    }

    @Test
    void dadoUnCorreo_cuandoSeBusca_entoncesNoDistingueMayusculasYExcluyeAlIndicado() {
        repositorio.registrar(
                docente("0109990010", "PERALTA", "juan@prueba.edu.ec", fichaCompleta()),
                "datos.demo",
                AHORA);

        assertThat(repositorio.existeCorreo("JUAN@Prueba.edu.ec", null)).isTrue();
        assertThat(repositorio.existeCorreo("juan@prueba.edu.ec", "0109990010")).isFalse();
        assertThat(repositorio.existeCorreo("otro@prueba.edu.ec", null)).isFalse();
    }

    @Test
    void dadaUnaTransaccionConElCandado_cuandoOtraIntentaBloquear_entoncesEspera()
            throws Exception {
        TransactionTemplate transacciones =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CompletableFuture<Void> soltar = new CompletableFuture<>();
        CompletableFuture<Void> primera =
                CompletableFuture.runAsync(
                        () ->
                                transacciones.executeWithoutResult(
                                        estado -> {
                                            repositorio.bloquear();
                                            soltar.join();
                                        }));
        Thread.sleep(500);

        CompletableFuture<Void> segunda =
                CompletableFuture.runAsync(
                        () -> transacciones.executeWithoutResult(estado -> repositorio.bloquear()));

        try {
            segunda.get(1, TimeUnit.SECONDS);
            throw new AssertionError("La segunda transacción no esperó el candado");
        } catch (TimeoutException esperado) {
            soltar.complete(null);
        }
        primera.get(5, TimeUnit.SECONDS);
        segunda.get(5, TimeUnit.SECONDS);
    }

    @Test
    void dadoUnCambio_cuandoSeRegistra_entoncesQuedaConSuAccionYSuDetalle() {
        new RegistroDeCambiosJdbc(jdbc)
                .registrar(
                        new CambioRegistrado(
                                AHORA,
                                "datos.demo",
                                CambioRegistrado.Accion.DOCENTE_CORREGIDO,
                                Map.of("identificacion", "0109990010", "cambios", List.of())));

        assertThat(
                        jdbc.sql(
                                        "SELECT accion || ' ' || (detalle ->> 'identificacion')"
                                                + " FROM docentes.registro_cambio")
                                .query(String.class)
                                .single())
                .isEqualTo("DOCENTE_CORREGIDO 0109990010");
    }

    private static Docente docente(
            String identificacion, String apellido, String correo, FichaAcademica ficha) {
        return Docente.nuevo(
                new Identificacion(
                        identificacion.startsWith("AB")
                                ? TipoDeIdentificacion.PASAPORTE
                                : TipoDeIdentificacion.CEDULA,
                        identificacion),
                new DatosDelDocente(
                        apellido,
                        "MALDONADO",
                        "JUAN",
                        "ANDRES",
                        correo,
                        "0990000001",
                        TipoPersonal.OCASIONAL,
                        CategoriaDeEscalafon.NO_APLICA,
                        Dedicacion.TC,
                        40,
                        LocalDate.of(2019, 10, 1),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 8, 31),
                        CondicionEspecial.NINGUNA,
                        "550613A01-P-0301",
                        ficha),
                ANIO);
    }

    private static TituloTercerNivel tercerNivel() {
        return new TituloTercerNivel(
                "INGENIERO DE SISTEMAS", "1031-2020-0000001", new CampoConocimiento("06", "061"));
    }

    private static FichaAcademica fichaCompleta() {
        return new FichaAcademica(
                tercerNivel(),
                List.of(
                        new TituloCuartoNivel(
                                "MÁSTER EN TELEMÁTICA",
                                TipoDeTituloDeCuartoNivel.MAESTRIA,
                                "061",
                                2015),
                        new TituloCuartoNivel(
                                "DOCTOR EN EDUCACIÓN",
                                TipoDeTituloDeCuartoNivel.DOCTORADO,
                                "011",
                                2022)),
                List.of(
                        new Curso("PROGRAMACIÓN EN JAVA", "061", 40, 2023),
                        new Curso("DOCENCIA UNIVERSITARIA", "011", 60, 2021)),
                List.of(
                        new ExperienciaLaboral(
                                "DESARROLLADOR", "EMPRESA XYZ", "061", new BigDecimal("3.5"))),
                List.of("A1", "A2"));
    }
}
