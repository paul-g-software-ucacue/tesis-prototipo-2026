package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.Confirmacion;
import ec.mdd.cargas.puertos.entrada.ConfirmarCarga;
import ec.mdd.cargas.puertos.entrada.ConsultarHistorial;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Las 10 cargas de prueba de la ERS, validadas y confirmadas en su orden con el proceso {@code
 * datos} real (F01-H04, SC-001, SC-002): cada una la aplica su servicio dueño y la siguiente
 * encuentra sus referencias. Subirlas otra vez no cambia nada; confirmar el mismo archivo da {@code
 * REPETIDA}; subir sin confirmar no cambia nada (US3). Su propia propiedad le da un contexto y un
 * PostgreSQL propios, vacíos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
@TestPropertySource(properties = "mdd.prueba=las-diez-cargas")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfirmarLasDiezCargasIntegrationTest {

    private static final String QUIEN = "datos.demo";

    /** Las 10 cargas en su orden, con el número de registros de cada archivo de prueba. */
    private static final Map<String, Integer> CARGAS = new LinkedHashMap<>();

    static {
        CARGAS.put("estructura_organica", 24);
        CARGAS.put("carreras", 7);
        CARGAS.put("ciclos_academicos", 2);
        CARGAS.put("asignaturas_malla", 300);
        CARGAS.put("oferta_periodo", 165);
        CARGAS.put("docentes", 47);
        CARGAS.put("politica_carga_horaria", 22);
        CARGAS.put("cargos_autoridades", 21);
        CARGAS.put("proyectos_practicas", 11);
        CARGAS.put("matriculados_carrera", 7);
    }

    @Autowired private ValidarArchivo validar;
    @Autowired private ConfirmarCarga confirmar;
    @Autowired private ConsultarHistorial historial;
    @Autowired private JdbcTemplate jdbc;

    private static ArchivoSubido archivo(String carga) {
        int numero = List.copyOf(CARGAS.keySet()).indexOf(carga) + 1;
        String nombre = String.format("%02d_%s.csv", numero, carga);
        try (InputStream entrada =
                ConfirmarLasDiezCargasIntegrationTest.class.getResourceAsStream(
                        "/datos-de-prueba/" + nombre)) {
            return new ArchivoSubido(nombre, entrada.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long filas(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private String conteo() {
        return filas("academico.unidad_organica")
                + "/"
                + filas("academico.carrera")
                + "/"
                + filas("docentes.docente")
                + "/"
                + filas("cargas.historial_carga");
    }

    @Test
    @Order(1)
    void subirSinConfirmar_noCambiaNingunDato() {
        String antes = conteo();

        ArchivoValidado validado =
                validar.validar("estructura_organica", archivo("estructura_organica"));

        assertThat(validado.vistaPrevia().delDueno().orElseThrow().cuantos(TipoDeRegistro.NUEVO))
                .isEqualTo(24);
        assertThat(conteo()).isEqualTo(antes);
    }

    @Test
    @Order(2)
    void lasDiezCargasEnSuOrden_quedanAplicadasSinErrores() {
        for (Map.Entry<String, Integer> carga : CARGAS.entrySet()) {
            ArchivoSubido archivo = archivo(carga.getKey());
            ArchivoValidado validado = validar.validar(carga.getKey(), archivo);
            assertThat(validado.informe().errores())
                    .as(carga.getKey() + ": " + validado.informe().primeros(3))
                    .isZero();
            VistaPreviaDelLote vista = validado.vistaPrevia().delDueno().orElseThrow();

            Confirmacion confirmacion =
                    confirmar.confirmar(carga.getKey(), archivo, vista.huella(), QUIEN);

            assertThat(confirmacion.resultado()).isEqualTo(ResultadoDeHistorial.APLICADA);
            assertThat(confirmacion.nuevos() + confirmacion.cambiados())
                    .as(carga.getKey())
                    .isPositive();
        }
        assertThat(filas("academico.unidad_organica")).isEqualTo(24);
        assertThat(filas("academico.asignatura")).isEqualTo(300);
        assertThat(filas("academico.asignatura_paralelo")).isEqualTo(165);
        assertThat(filas("academico.matricula")).isEqualTo(7);
        assertThat(filas("docentes.docente")).isEqualTo(47);
        assertThat(filas("docentes.cargo")).isEqualTo(21);
        assertThat(filas("docentes.proyecto")).isEqualTo(11);
    }

    @Test
    @Order(3)
    void subidasOtraVez_ningunaTieneNuevosCambiosNiBajas() {
        for (String carga : CARGAS.keySet()) {
            VistaPreviaDelLote vista =
                    validar.validar(carga, archivo(carga)).vistaPrevia().delDueno().orElseThrow();

            assertThat(vista.cuantos(TipoDeRegistro.IGUAL)).as(carga).isEqualTo(CARGAS.get(carga));
            assertThat(vista.registros()).as(carga).hasSize(CARGAS.get(carga));
        }
    }

    @Test
    @Order(5)
    void unArchivoDeDocentesDeCasiCincoMegas_seRevisaEn30SYSeConfirmaEn60S() {
        ArchivoSubido grande = docentesFicticios();
        assertThat(grande.contenido().length).isLessThanOrEqualTo(5 * 1024 * 1024);

        long inicio = System.nanoTime();
        ArchivoValidado validado = validar.validar("docentes", grande);
        long vista = (System.nanoTime() - inicio) / 1_000_000;
        VistaPreviaDelLote delDueno = validado.vistaPrevia().delDueno().orElseThrow();
        inicio = System.nanoTime();
        Confirmacion confirmacion =
                confirmar.confirmar("docentes", grande, delDueno.huella(), QUIEN);
        long aplicar = (System.nanoTime() - inicio) / 1_000_000;

        // La medición de SC-004, para la evidencia.
        System.out.printf(
                "SC-004: %d bytes y %d docentes nuevos; vista previa en %d ms y confirmación en %d"
                        + " ms%n",
                grande.contenido().length, confirmacion.nuevos(), vista, aplicar);
        assertThat(confirmacion.nuevos()).isGreaterThan(10_000);
        assertThat(vista).isLessThan(30_000);
        assertThat(aplicar).isLessThan(60_000);
    }

    /**
     * Unos 11 600 docentes ficticios con cédulas válidas y correos distintos (RNF-SEG-06), copiados
     * del primero de prueba, sin pasar de 5 MB.
     */
    private static ArchivoSubido docentesFicticios() {
        String[] lineas =
                new String(archivo("docentes").contenido(), java.nio.charset.StandardCharsets.UTF_8)
                        .replace("\uFEFF", "")
                        .split("\r?\n");
        String encabezado = lineas[0];
        List<String> campos = List.of(encabezado.split(",", -1));
        String[] modelo = lineas[1].split(",", -1);
        StringBuilder csv = new StringBuilder(encabezado).append("\r\n");
        long bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        int maximo = 5 * 1024 * 1024 - 1024;
        for (int numero = 0; ; numero++) {
            String[] fila = modelo.clone();
            fila[campos.indexOf("numero_identificacion")] = cedula(numero);
            fila[campos.indexOf("correo_institucional")] =
                    "d" + numero + "@instituto-prueba.edu.ec";
            String linea = String.join(",", fila) + "\r\n";
            int largo = linea.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (bytes + largo > maximo) {
                break;
            }
            csv.append(linea);
            bytes += largo;
        }
        return new ArchivoSubido(
                "06_docentes.csv",
                csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Una cédula ficticia de 10 dígitos que empieza con 09, con su dígito verificador. */
    private static String cedula(int numero) {
        String base = String.format("09%07d", 2_000_000 + numero);
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int producto = (base.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += producto > 9 ? producto - 9 : producto;
        }
        return base + (10 - suma % 10) % 10;
    }

    @Test
    @Order(4)
    void confirmarOtraVezElMismoArchivo_esRepetidaYNoAgregaNada() {
        ArchivoSubido carreras = archivo("carreras");
        String huella =
                validar.validar("carreras", carreras)
                        .vistaPrevia()
                        .delDueno()
                        .orElseThrow()
                        .huella();
        long antes = filas("academico.registro_cambio");

        Confirmacion repetida = confirmar.confirmar("carreras", carreras, huella, QUIEN);

        assertThat(repetida.resultado()).isEqualTo(ResultadoDeHistorial.REPETIDA);
        assertThat(filas("academico.registro_cambio")).isEqualTo(antes);
        List<EntradaDeHistorial> deCarreras = historial.historial("carreras");
        assertThat(deCarreras)
                .extracting(EntradaDeHistorial::resultado)
                .containsExactly(ResultadoDeHistorial.REPETIDA, ResultadoDeHistorial.APLICADA);
        assertThat(filas("cargas.historial_carga")).isEqualTo(11);
    }
}
