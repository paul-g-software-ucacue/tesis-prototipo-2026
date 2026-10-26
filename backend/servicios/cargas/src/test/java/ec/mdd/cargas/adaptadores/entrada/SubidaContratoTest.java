package ec.mdd.cargas.adaptadores.entrada;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.cargas.adaptadores.salida.DatosDePrueba;
import ec.mdd.cargas.adaptadores.salida.LibroDePrueba;
import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Subir el archivo de una carga cumple {@code contratos/cargas/openapi.yaml} (principio IV,
 * F01-H02), con los lectores, el diccionario y los datos de prueba de la ERS reales.
 */
@WebMvcTest(SubidaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class, ReferenciasDePrueba.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
@ExtendWith(OutputCaptureExtension.class)
class SubidaContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElCsvDeDocentes_cuandoSeSube_entonces200ConSus47Registros() throws Exception {
        MvcResult resultado =
                subir("docentes", "06_docentes.csv", DatosDePrueba.contenido("06_docentes.csv"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("carga", "docentes")
                .containsEntry("archivo", "06_docentes.csv")
                .containsEntry("formato", "CSV")
                .containsEntry("registros", 47)
                .containsEntry("valido", true)
                .containsEntry("errores", 0)
                .containsEntry("advertencias", 0)
                .containsEntry("problemas", List.of());
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaCedulaSinElCero_cuandoSeSube_entonces200ConElInformeYLaSugerencia(
            CapturedOutput salida) throws Exception {
        MvcResult resultado =
                subir("docentes", "06_docentes.csv", docentesCon("0109990010", "109990010"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("valido", false)
                .containsEntry("errores", 1)
                .containsEntry("advertencias", 0);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.problemas[0]"))
                .containsEntry("fila", 2)
                .containsEntry("campo", "numero_identificacion")
                .containsEntry("valor", "109990010")
                .containsEntry(
                        "motivo", "Cédula inválida o sin cero inicial: le falta el 0 inicial.")
                .containsEntry("tipo", "ERROR")
                .containsEntry("sugerencia", "0109990010");
        contrato.validar(resultado);
        assertThat(salida.getAll()).doesNotContain("109990010");
    }

    @Test
    void dadoUnValorQueEmpiezaComoFormula_cuandoSeDescargaElInforme_entoncesCsvNeutralizado()
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion =
                multipart("/api/cargas/plantillas/docentes/archivo/informe");
        peticion.file(
                new MockMultipartFile(
                        "archivo", "06_docentes.csv", "text/csv", docentesCon(",TC,", ",=1+1,")));

        MvcResult resultado = mvc.perform(peticion.with(escritura())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).startsWith("text/csv");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .contains("informe-docentes.csv");
        String csv = cuerpo(resultado);
        assertThat(csv)
                .startsWith("\uFEFFfila,campo,valor,motivo,tipo,sugerencia\r\n")
                .contains("2,dedicacion,'=1+1,Tiene 4 caracteres; el máximo es 2.,ERROR,")
                .doesNotContain(",=1+1");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnArchivoRechazado_cuandoSeSubeElCorregido_entoncesSeValidaDesdeCero()
            throws Exception {
        MvcResult rechazado =
                subir("docentes", "06_docentes.csv", docentesCon("0109990010", "109990010"));
        MvcResult corregido =
                subir("docentes", "06_docentes.csv", DatosDePrueba.contenido("06_docentes.csv"));

        assertThat(JsonPath.<Boolean>read(cuerpo(rechazado), "$.valido")).isFalse();
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(corregido), "$"))
                .containsEntry("valido", true)
                .containsEntry("errores", 0)
                .containsEntry("problemas", List.of());
    }

    @Test
    void dadoElMismoArchivoEnXlsx_cuandoSeSube_entonces200ConLosMismosRegistros() throws Exception {
        // Los matriculados no tienen comillas: cada línea se parte por las comas.
        String csv =
                new String(DatosDePrueba.contenido("10_matriculados_carrera.csv"), UTF_8).strip();
        List<String> lineas = List.of(csv.split("\r\n"));
        byte[] xlsx =
                LibroDePrueba.deTexto(
                        List.of(lineas.getFirst().split(",", -1)),
                        lineas.subList(1, lineas.size()).stream()
                                .map(linea -> List.of(linea.split(",", -1)))
                                .toList());

        MvcResult resultado = subir("matriculados_carrera", "10_matriculados_carrera.xlsx", xlsx);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("formato", "XLSX")
                .containsEntry("registros", 7);
        contrato.validar(resultado);
    }

    @Test
    void dadoElArchivoMasGrandeDePrueba_cuandoSeSube_entoncesSeLeeEnMenosDeUnSegundo()
            throws Exception {
        long inicio = System.nanoTime();
        MvcResult resultado =
                subir(
                        "asignaturas_malla",
                        "04_asignaturas_malla.csv",
                        DatosDePrueba.contenido("04_asignaturas_malla.csv"));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.registros")).isEqualTo(300);
        assertThat(milisegundos).isLessThan(1000);
    }

    @Test
    void dadaUnaPeticionSinLaParteArchivo_cuandoSeSube_entonces400Comun() throws Exception {
        MvcResult resultado =
                mvc.perform(multipart("/api/cargas/plantillas/docentes/archivo").with(escritura()))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(resultado)).contains("COMUN_SOLICITUD_INVALIDA");
    }

    @Test
    void dadaUnaCargaQueNoExiste_cuandoSeSube_entonces404() throws Exception {
        MvcResult resultado = subir("comisiones", "c.csv", "a\n1\n".getBytes(UTF_8));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado)).contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoCadaArchivoQueNoSePuedeLeer_cuandoSeSube_entoncesSuCodigoSegunElContrato()
            throws Exception {
        String carreras =
                "codigo_carrera,nombre,siglas,codigo_unidad_sede,nivel_formacion,"
                        + "modalidad,jornadas,campo_amplio,campo_especifico,campo_detallado,numero_ciclos,"
                        + "estado";
        String fila = "C1,Uno,U,MATRIZ,TECNOLOGICO_SUPERIOR,PRESENCIAL,MATUTINA,04,041,,4,VIGENTE";
        byte[] conDosHojas;
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook libro =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            libro.createSheet("Uno");
            libro.createSheet("Dos");
            conDosHojas = LibroDePrueba.bytes(libro);
        }
        Object[][] casos = {
            {"carreras.pdf", "%PDF-1.7".getBytes(UTF_8), 400, "CARGAS_FORMATO_NO_ADMITIDO"},
            {
                "carreras.csv",
                (carreras + "\nC1,Peñafiel\n").getBytes(Charset.forName("windows-1252")),
                400,
                "CARGAS_CODIFICACION_NO_ADMITIDA"
            },
            {"carreras.xlsx", conDosHojas, 400, "CARGAS_VARIAS_HOJAS"},
            {
                "carreras.csv",
                (carreras + "\n" + fila + "\n\"sin cerrar\n").getBytes(UTF_8),
                400,
                "CARGAS_ARCHIVO_ILEGIBLE"
            },
            {
                "carreras.csv",
                (carreras + "\n").getBytes(UTF_8),
                400,
                "CARGAS_ARCHIVO_SIN_REGISTROS"
            },
            {
                "carreras.csv",
                new byte[ec.mdd.cargas.aplicacion.ValidarArchivoServicio.MAXIMO + 1],
                413,
                "CARGAS_ARCHIVO_DEMASIADO_GRANDE"
            },
        };
        for (Object[] caso : casos) {
            MvcResult resultado = subir("carreras", (String) caso[0], (byte[]) caso[1]);

            assertThat(resultado.getResponse().getStatus()).as((String) caso[3]).isEqualTo(caso[2]);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo(caso[3]);
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoUnArchivoIlegible_cuandoSeSube_entoncesElDetalleDiceLaFila() throws Exception {
        MvcResult resultado =
                subir(
                        "matriculados_carrera",
                        "m.csv",
                        "codigo_periodo,codigo_carrera,total_matriculados,fecha_corte,fuente\n1,2\n"
                                .getBytes(UTF_8));

        assertThat(JsonPath.<List<Map<String, String>>>read(cuerpo(resultado), "$.detalles"))
                .containsExactly(
                        Map.of(
                                "campo",
                                "archivo",
                                "descripcion",
                                "La fila 2 tiene 2 valores y los encabezados, 5."));
    }

    @Test
    void dadoUnArchivoDeCarrerasSinCodigoYConUnaDeMas_cuandoSeSube_entoncesNombraLasColumnas()
            throws Exception {
        String sinCodigo =
                "nombre,siglas,codigo_unidad_sede,nivel_formacion,modalidad,jornadas,campo_amplio,"
                        + "campo_especifico,campo_detallado,numero_ciclos,estado,extra\n"
                        + "Uno,U,MATRIZ,TECNOLOGICO_SUPERIOR,PRESENCIAL,MATUTINA,04,041,,4,VIGENTE,x\n";

        MvcResult resultado = subir("carreras", "carreras.csv", sinCodigo.getBytes(UTF_8));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_ENCABEZADOS_DISTINTOS");
        assertThat(JsonPath.<List<Map<String, String>>>read(cuerpo(resultado), "$.detalles"))
                .containsExactly(
                        Map.of("campo", "codigo_carrera", "descripcion", "Falta en el archivo."),
                        Map.of("campo", "extra", "descripcion", "No está en la plantilla."));
        contrato.validar(resultado);
    }

    @Test
    void dadoUnArchivoConUnaCedulaYUnError_cuandoSeSube_entoncesLaCedulaNoApareceEnNingunLado(
            CapturedOutput salida) throws Exception {
        String csv = new String(DatosDePrueba.contenido("06_docentes.csv"), UTF_8);
        String[] lineas = csv.split("\r\n");
        String conCedula =
                lineas[0] + ",extra\r\n" + lineas[1].replace("0109990010", "0999999999") + ",x\r\n";

        MvcResult resultado = subir("docentes", "06_docentes.csv", conCedula.getBytes(UTF_8));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(resultado)).doesNotContain("0999999999");
        assertThat(salida.getAll()).doesNotContain("0999999999");
    }

    MvcResult subir(String carga, String nombre, byte[] contenido) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion =
                multipart("/api/cargas/plantillas/" + carga + "/archivo");
        peticion.file(
                new MockMultipartFile("archivo", nombre, "application/octet-stream", contenido));
        return mvc.perform(peticion.with(escritura())).andReturn();
    }

    /** El archivo de docentes de prueba con el primer texto cambiado. */
    private static byte[] docentesCon(String antes, String despues) {
        String csv = new String(DatosDePrueba.contenido("06_docentes.csv"), UTF_8);
        return csv.replaceFirst(java.util.regex.Pattern.quote(antes), despues).getBytes(UTF_8);
    }

    static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(UTF_8);
    }

    /** El Responsable de consolidación, con «Cargas: escritura» desde D-71. */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_CARGAS:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_CARGAS:ESCRITURA"),
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }
}
