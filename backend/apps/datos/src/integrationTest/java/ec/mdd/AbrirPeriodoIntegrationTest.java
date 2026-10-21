package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.aplicacion.AbrirPeriodoServicio;
import ec.mdd.academico.puertos.entrada.AbrirPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.HorasDeFila;
import ec.mdd.normativa.dominio.VersionEnUso;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Abrir un periodo con la Normativa real del proceso {@code datos} (F02-H01, T016): la versión
 * queda en Normativa y ya no se edita, cada servicio confirma su propia transacción (research §3) y
 * dos aperturas simultáneas dejan una sola versión.
 *
 * <p>La base es un PostgreSQL de Testcontainers que se descarta al terminar, así que la prueba no
 * borra lo que crea; cada caso usa códigos de periodo propios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class AbrirPeriodoIntegrationTest {

    private static final String QUIEN = "vicerrectorado.demo";

    @Autowired private RegistrarPeriodo registrar;
    @Autowired private AbrirPeriodo abrir;
    @Autowired private ConsultarPeriodos consultar;
    @Autowired private CrearVersion crearVersion;
    @Autowired private EditarVersion editarVersion;
    @Autowired private ConsultarNormativa normativa;
    @Autowired private RepositorioPeriodos repositorio;
    @Autowired private NormativaDelPeriodo normativaDelPeriodo;
    @Autowired private Transaccion transaccionAcademico;
    @Autowired private JdbcTemplate jdbc;

    private static String codigo() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String periodoNuevo() {
        String codigo = codigo();
        registrar.registrar(
                codigo,
                "Periodo " + codigo,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 2, 28),
                QUIEN);
        return codigo;
    }

    private int versionNueva() {
        return crearVersion
                .crear(
                        1,
                        new DatosDeVersion(
                                "IT-" + UUID.randomUUID().toString().substring(0, 8),
                                "Prueba de F02-H01",
                                LocalDate.of(2026, 1, 1)),
                        QUIEN)
                .numero();
    }

    // La misma versión, sin cambios: basta para saber si todavía se puede editar.
    private static CambiosDeVersion sinCambios(VersionNormativa version) {
        Map<String, Map<String, BigDecimal>> parametros = new LinkedHashMap<>();
        version.reglas()
                .forEach(regla -> parametros.put(regla.identificador(), regla.parametros()));
        return new CambiosDeVersion(
                new DatosDeVersion(version.codigo(), version.origen(), version.vigenteDesde()),
                version.tabla().filas().stream().map(HorasDeFila::de).toList(),
                parametros);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeAbre_entoncesNormativaLaGuardaYYaNoSeEdita() {
        String codigo = periodoNuevo();
        int version = versionNueva();
        VersionNormativa antes = normativa.version(version);
        editarVersion.editar(version, sinCambios(antes), QUIEN);

        abrir.abrir(codigo, version, QUIEN);

        assertThat(normativa.periodo(codigo).version()).isEqualTo(version);
        assertThat(consultar.porCodigo(codigo).version()).hasValue(version);
        assertThatThrownBy(() -> editarVersion.editar(version, sinCambios(antes), QUIEN))
                .isInstanceOf(VersionEnUso.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT usuario FROM normativa.registro_cambio WHERE accion ="
                                        + " 'VERSION_ASOCIADA' AND detalle->>'periodo' = ?",
                                String.class,
                                codigo))
                .isEqualTo(QUIEN);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT usuario || ' ' || (detalle->>'version') FROM"
                                        + " academico.registro_cambio WHERE accion ="
                                        + " 'PERIODO_ABIERTO' AND detalle->>'codigo' = ?",
                                String.class,
                                codigo))
                .isEqualTo(QUIEN + " " + version);
    }

    @Test
    void dadoDosPeriodos_cuandoSeAbrenConLaMismaVersion_entoncesLosDosQuedanConElla() {
        String primero = periodoNuevo();
        String segundo = periodoNuevo();
        int version = versionNueva();

        abrir.abrir(primero, version, QUIEN);
        abrir.abrir(segundo, version, QUIEN);

        assertThat(consultar.porCodigo(primero).version()).hasValue(version);
        assertThat(consultar.porCodigo(segundo).version()).hasValue(version);
        assertThat(normativa.version(version).periodos()).contains(primero, segundo);
    }

    @Test
    void dadoQueFallaElRegistroDeAcademico_cuandoSeAbre_entoncesLaAsociacionDeNormativaQueda() {
        String codigo = periodoNuevo();
        int version = versionNueva();
        AbrirPeriodoServicio conRegistroQueFalla =
                new AbrirPeriodoServicio(
                        repositorio,
                        normativaDelPeriodo,
                        cambio -> {
                            throw new IllegalStateException("el registro de Académico falla");
                        },
                        transaccionAcademico,
                        Clock.systemUTC());

        assertThatThrownBy(() -> conRegistroQueFalla.abrir(codigo, version, QUIEN))
                .isInstanceOf(IllegalStateException.class);

        // Normativa confirmó en su propia transacción: la de Académico no la deshace.
        assertThat(normativa.periodo(codigo).version()).isEqualTo(version);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM academico.registro_cambio WHERE accion ="
                                        + " 'PERIODO_ABIERTO' AND detalle->>'codigo' = ?",
                                Integer.class,
                                codigo))
                .isZero();
        // Abrirlo otra vez con la misma versión no cambia nada.
        assertThat(abrir.abrir(codigo, version, QUIEN).version()).hasValue(version);
    }

    @Test
    void dadoDosAperturasSimultaneas_cuandoPidenVersionesDistintas_entoncesQuedaUnaSola()
            throws Exception {
        String codigo = periodoNuevo();
        int una = versionNueva();
        int otra = versionNueva();
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();

        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            for (int version : new int[] {una, otra}) {
                resultados.add(
                        hilos.submit(
                                () -> {
                                    salida.await();
                                    abrir.abrir(codigo, version, QUIEN);
                                    return version;
                                }));
            }
            salida.countDown();
        }

        List<Integer> ganadoras = new ArrayList<>();
        int rechazadas = 0;
        for (Future<Integer> resultado : resultados) {
            try {
                ganadoras.add(resultado.get());
            } catch (java.util.concurrent.ExecutionException error) {
                assertThat(error.getCause()).isInstanceOf(PeriodoConOtraVersion.class);
                rechazadas++;
            }
        }
        assertThat(ganadoras).hasSize(1);
        assertThat(rechazadas).isEqualTo(1);
        assertThat(normativa.periodo(codigo).version()).isEqualTo(ganadoras.getFirst());
    }
}
