package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las plantillas y el diccionario de datos cumplen {@code contratos/cargas/openapi.yaml} (principio
 * IV, F01-H01), con el diccionario versionado real y los encabezados de los anexos de la ERS.
 */
@WebMvcTest(PlantillasControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ReferenciasDePrueba.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PlantillasContratoTest {

    static final String BOM = "\uFEFF";

    static final String ENCABEZADO_DEL_DICCIONARIO =
            "numero,campo,descripcion,tipo,longitud_maxima,obligatorio,formato_o_valores,clave,"
                    + "validacion,ejemplo";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElDiccionario_cuandoSeListan_entoncesLasDiezCargasConSus115Columnas()
            throws Exception {
        MvcResult resultado = pedir(get("/api/cargas/plantillas"), distributivo());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<Integer>>read(cuerpo, "$.cargas[*].numero"))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.cargas[*].columnas[*].campo"))
                .hasSize(115);
        assertThat(JsonPath.<List<Integer>>read(cuerpo, "$.cargas[4].dependeDe"))
                .containsExactly(3, 4);
        assertThat(JsonPath.<String>read(cuerpo, "$.cargas[0].nombre"))
                .isEqualTo("Estructura orgánica");
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$.cargas[0].columnas[0]"))
                .doesNotContainKeys("valores");
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.convenciones[*].tema")).hasSize(13);
        contrato.validar(resultado);
    }

    @Test
    void dadaCadaCarga_cuandoSeDescargaSuPlantilla_entoncesSoloSusEncabezadosComoLaErs()
            throws Exception {
        for (Map.Entry<String, String> anexo : anexo().entrySet()) {
            String archivo = anexo.getKey();
            String codigo = archivo.substring(3, archivo.length() - 4);
            MvcResult resultado =
                    pedir(get("/api/cargas/plantillas/" + codigo + "/plantilla"), cargas());

            assertThat(resultado.getResponse().getStatus()).as(archivo).isEqualTo(200);
            assertThat(resultado.getResponse().getContentType())
                    .isEqualTo("text/csv;charset=UTF-8");
            assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                    .isEqualTo("attachment; filename=\"" + archivo + "\"");
            assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .isEqualTo(BOM + anexo.getValue() + "\r\n");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaCadaCarga_cuandoSeDescargaSuDiccionario_entoncesUnaFilaPorColumnaYLasConvenciones()
            throws Exception {
        for (Map.Entry<String, String> anexo : anexo().entrySet()) {
            String archivo = anexo.getKey();
            String codigo = archivo.substring(3, archivo.length() - 4);
            List<String> campos = List.of(anexo.getValue().split(","));
            MvcResult resultado =
                    pedir(get("/api/cargas/plantillas/" + codigo + "/diccionario"), distributivo());

            assertThat(resultado.getResponse().getStatus()).as(archivo).isEqualTo(200);
            assertThat(resultado.getResponse().getContentType())
                    .isEqualTo("text/csv;charset=UTF-8");
            assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                    .isEqualTo(
                            "attachment; filename=\""
                                    + archivo.replace(".csv", "_diccionario.csv")
                                    + "\"");
            String csv = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(csv).startsWith(BOM + ENCABEZADO_DEL_DICCIONARIO + "\r\n");
            List<String> lineas = List.of(csv.substring(1).split("\r\n", -1));
            assertThat(lineas.subList(1, campos.size() + 1))
                    .as(archivo)
                    .allSatisfy(linea -> assertThat(linea.split(",")[0]).isNotEmpty());
            for (int i = 0; i < campos.size(); i++) {
                assertThat(lineas.get(i + 1)).startsWith((i + 1) + "," + campos.get(i) + ",");
            }
            assertThat(lineas.get(campos.size() + 1)).isEmpty();
            assertThat(lineas.get(campos.size() + 2)).isEqualTo("tema,regla");
            assertThat(lineas.subList(campos.size() + 3, lineas.size() - 1)).hasSize(13);
            assertThat(lineas.getLast()).isEmpty();
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaLaEstructuraOrganica_cuandoSeDescargaSuDiccionario_entoncesLasGestionesYLasTildes()
            throws Exception {
        String csv =
                pedir(get("/api/cargas/plantillas/estructura_organica/diccionario"), cargas())
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        String gestion =
                csv.lines()
                        .filter(linea -> linea.startsWith("5,codigo_gestion_autorizada,"))
                        .findFirst()
                        .orElseThrow();
        assertThat(gestion)
                .contains("Gestión autorizada por SENESCYT que representa")
                .contains("Sí si tipo_unidad = GESTION")
                .contains("G01 Rector; G02 Vicerrector; G03 Coordinador Académico Institucional;")
                .contains("G21 Gestor Unidad de Estadísticas");
        assertThat(csv).contains("1,codigo_unidad,Código único de la unidad,Texto,20,Sí,");

        String json =
                pedir(get("/api/cargas/plantillas"), cargas())
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(json, "$.cargas[0].columnas[4].valores[*].codigo"))
                .hasSize(21)
                .startsWith("G01");
    }

    @Test
    void dadoUnCodigoQueNoEsDeUnaCarga_cuandoSeDescargaSuDiccionario_entonces404()
            throws Exception {
        MvcResult resultado =
                pedir(
                        get("/api/cargas/plantillas/comisiones/diccionario").accept("text/csv"),
                        distributivo());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoQueNoEsDeUnaCarga_cuandoSeDescarga_entonces404Comun() throws Exception {
        for (MockHttpServletRequestBuilder peticion :
                List.of(
                        get("/api/cargas/plantillas/comisiones/plantilla"),
                        get("/api/cargas/plantillas/comisiones/plantilla").accept("text/csv"))) {
            MvcResult resultado = pedir(peticion, distributivo());

            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(resultado.getResponse().getContentType()).startsWith("application/json");
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("COMUN_RECURSO_NO_ENCONTRADO");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoUnCodigoConOtroFormato_cuandoSeDescarga_entonces400() throws Exception {
        MvcResult resultado =
                pedir(get("/api/cargas/plantillas/Estructura-Organica/plantilla"), cargas());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_SOLICITUD_INVALIDA");
    }

    @Test
    void dadoUnUsuarioSinPermiso_cuandoPideUnCsv_entonces403ComunYNo406() throws Exception {
        MvcResult resultado =
                pedir(
                        get("/api/cargas/plantillas/docentes/plantilla").accept("text/csv"),
                        jwt().jwt(token -> token.subject("gestion.demo"))
                                .authorities(
                                        new SimpleGrantedAuthority("PERMISO_PROYECTOS:LECTURA")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(resultado.getResponse().getContentType()).startsWith("application/json");
        assertThat(resultado.getResponse().getContentAsString())
                .contains("SEGURIDAD_ACCESO_NO_AUTORIZADO");
        contrato.validar(resultado);
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    /** El Responsable de consolidación: lee el distributivo del periodo (aclaración 1). */
    static JwtRequestPostProcessor distributivo() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    /** Talento Humano: lee y escribe las cargas (D-65). */
    static JwtRequestPostProcessor cargas() {
        return jwt().jwt(token -> token.subject("datos.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_CARGAS:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_CARGAS:ESCRITURA"));
    }

    /** La fila de encabezados de cada plantilla de los anexos de la ERS, por archivo. */
    static Map<String, String> anexo() throws IOException {
        Map<String, String> anexo = new LinkedHashMap<>();
        try (InputStream archivo =
                PlantillasContratoTest.class.getResourceAsStream(
                        "/cargas/anexo-ers-encabezados.csv")) {
            String texto = new String(archivo.readAllBytes(), StandardCharsets.UTF_8);
            for (String linea : texto.strip().split("\n")) {
                int coma = linea.indexOf(',');
                anexo.put(linea.substring(0, coma), linea.substring(coma + 1).strip());
            }
        }
        return anexo;
    }
}
