package ec.mdd.normativa.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las operaciones de {@code /api/normativa} cumplen {@code contratos/normativa/openapi.yaml}
 * (principio IV), con un doble de los casos de uso escrito a mano (principio VIII).
 */
@WebMvcTest(NormativaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({NormativaContratoTest.Dobles.class, ErroresNormativa.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class NormativaContratoTest {

    static final VersionNormativa VERSION_1 = version();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("normativa");

    @Autowired private MockMvc mvc;

    @Autowired private NormativaEnMemoria normativa;

    @BeforeEach
    void reiniciar() {
        normativa.reiniciar();
    }

    @Test
    void dadoElVicerrector_cuandoListaLasVersiones_entonces200SegunElContrato() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones"));

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"numero\":1,\"codigo\":\"SENESCYT-2024-03\","
                                + "\"origen\":\"Política SENESCYT 2024\","
                                + "\"vigenteDesde\":\"2024-03-11\",\"tipoIst\":\"T2\","
                                + "\"periodos\":[]},"
                                + "{\"numero\":2,\"codigo\":\"SENESCYT-2025-01\","
                                + "\"origen\":\"Reforma de la política 2025\","
                                + "\"vigenteDesde\":\"2025-01-15\",\"tipoIst\":\"T2\","
                                + "\"periodos\":[],\"copiadaDe\":1}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionUno_cuandoSeConsulta_entonces200ConSusReglasYSuTabla() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones/1"));

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.reglas[*].identificador"))
                .containsExactly("RN-01", "RN-02");
        assertThat(JsonPath.<Integer>read(cuerpo, "$.reglas[0].parametros[0].valor")).isEqualTo(40);
        // RN-01 es solo parámetros: sin severidad, el campo se omite.
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$.reglas[0]"))
                .doesNotContainKey("severidad");
        assertThat(JsonPath.<String>read(cuerpo, "$.reglas[1].severidad")).isEqualTo("DURA");
        Map<String, Object> docente = JsonPath.read(cuerpo, "$.tablaCargaHoraria[0]");
        assertThat(docente)
                .containsEntry("aplicaA", "TODOS")
                .containsEntry("perfil", "DOCENTE_TC")
                .containsEntry("horasClase", 22)
                .containsEntry("horasClaseMinimo", 18)
                .containsEntry("horasClaseMaximo", 24)
                .containsEntry("horasOtrasDocencia", 18)
                .containsEntry("horasGestion", 0)
                .doesNotContainKey("horasInvestigacionVinculacionMaximo")
                .doesNotContainKey("observacion");
        assertThat(JsonPath.<String>read(cuerpo, "$.tablaCargaHoraria[1].aplicaA")).isEqualTo("T2");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeConsulta_entonces404Comun() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones/9"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionUno_cuandoSeCreaUnaNueva_entonces201ConLaCopiaYElUsuarioDelToken()
            throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/normativa/versiones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"copiarDe\":1,\"codigo\":\"SENESCYT-2025-02\","
                                                + "\"origen\":\"Reforma 2025\","
                                                + "\"vigenteDesde\":\"2025-02-01\"}"),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Integer>read(cuerpo, "$.numero")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(cuerpo, "$.copiadaDe")).isEqualTo(1);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.periodos")).isEmpty();
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.tablaCargaHoraria[*].perfil"))
                .containsExactly("DOCENTE_TC", "GESTOR");
        assertThat(normativa.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaVersionDeOrigenQueNoExiste_cuandoSeCrea_entonces404() throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/normativa/versiones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"copiarDe\":9,\"codigo\":\"X\",\"origen\":\"Y\","
                                                + "\"vigenteDesde\":\"2025-02-01\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoRepetido_cuandoSeCrea_entonces409DelCatalogo() throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/normativa/versiones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"copiarDe\":1,\"codigo\":\"SENESCYT-2024-03\","
                                                + "\"origen\":\"Y\","
                                                + "\"vigenteDesde\":\"2025-02-01\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.codigo"))
                .isEqualTo("NORMATIVA_CODIGO_REPETIDO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoEnBlanco_cuandoSeCrea_entonces400ConSuCampo() throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/normativa/versiones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"copiarDe\":1,\"codigo\":\" \",\"origen\":\"Y\","
                                                + "\"vigenteDesde\":\"2025-02-01\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(
                        JsonPath.<List<String>>read(
                                resultado.getResponse().getContentAsString(),
                                "$.detalles[*].campo"))
                .containsExactly("codigo");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionDos_cuandoSeEditanSusHoras_entonces200ConElCambio() throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/normativa/versiones/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(20, true)),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(cuerpo, "$.tablaCargaHoraria[0].horasClase"))
                .isEqualTo(20);
        assertThat(JsonPath.<Double>read(cuerpo, "$.reglas[1].parametros[0].valor"))
                .isEqualTo(38.5);
        assertThat(JsonPath.<String>read(cuerpo, "$.origen")).isEqualTo("Reforma editada");
        assertThat(normativa.version(1)).isEqualTo(VERSION_1);
        assertThat(normativa.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoHorasDeClaseFueraDeRango_cuandoSeEdita_entonces400ConLaFilaYElMotivo()
            throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/normativa/versiones/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(25, true)),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].campo"))
                .isEqualTo("tablaCargaHoraria[DOCENTE_TC/TODOS]");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].descripcion"))
                .isEqualTo(
                        "Las horas de clase (25) no están entre el mínimo y el máximo (18 a 24)");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaFilaDeMenos_cuandoSeEdita_entonces400PorqueNoSeQuitanFilas() throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/normativa/versiones/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(22, false)),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].campo"))
                .isEqualTo("tablaCargaHoraria[GESTOR/T2]");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].descripcion"))
                .contains("no se quitan filas");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeEdita_entonces404() throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/normativa/versiones/9")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(22, true)),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionDosEnUso_cuandoSeEdita_entonces409QuePideUnaVersionNueva() throws Exception {
        normativa.asociar(2, "2025-1");

        MvcResult resultado =
                pedir(
                        put("/api/normativa/versiones/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(20, true)),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("NORMATIVA_VERSION_EN_USO");
        assertThat(JsonPath.<String>read(cuerpo, "$.descripcion"))
                .contains("Crea una versión nueva a partir de ella");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeAsocia_entonces200YLaVersionLoMuestra() throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/normativa/periodos/2025-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":2}"),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo, "$.periodo")).isEqualTo("2025-1");
        assertThat(JsonPath.<Integer>read(cuerpo, "$.version")).isEqualTo(2);
        assertThat(JsonPath.<String>read(cuerpo, "$.asociadaEn")).startsWith("2026-10-18T15:00");
        assertThat(normativa.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);

        MvcResult version = pedir(get("/api/normativa/versiones/2"));
        assertThat(
                        JsonPath.<List<String>>read(
                                version.getResponse().getContentAsString(), "$.periodos"))
                .containsExactly("2025-1");
        MvcResult periodo = pedir(get("/api/normativa/periodos/2025-1"));
        assertThat(periodo.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(periodo);
    }

    @Test
    void dadoUnPeriodoConOtraVersion_cuandoSeAsocia_entonces409DelCatalogo() throws Exception {
        normativa.asociar(1, "2025-1");

        MvcResult resultado =
                pedir(
                        put("/api/normativa/periodos/2025-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":2}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.codigo"))
                .isEqualTo("NORMATIVA_PERIODO_CON_VERSION");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoMalFormadoOUnaVersionQueNoExiste_cuandoSeAsocia_entonces400O404()
            throws Exception {
        MvcResult malFormado =
                pedir(
                        put("/api/normativa/periodos/2025 1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":2}"),
                        escritura());
        MvcResult sinVersion =
                pedir(
                        put("/api/normativa/periodos/2025-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":9}"),
                        escritura());

        assertThat(malFormado.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinVersion.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(sinVersion);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entonces404() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/periodos/2030-1"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    // La versión 2 completa, con las horas de clase del docente de tiempo completo y RN-02 en 38,5.
    private static String edicion(int claseTiempoCompleto, boolean conGestor) {
        String gestor =
                ",{\"aplicaA\":\"T2\",\"perfil\":\"GESTOR\",\"dedicacionTotal\":40,"
                        + "\"horasClase\":14,\"horasClaseMinimo\":14,\"horasClaseMaximo\":14,"
                        + "\"horasOtrasDocencia\":16,\"horasGestion\":10}";
        return "{\"codigo\":\"SENESCYT-2025-01\",\"origen\":\"Reforma editada\","
                + "\"vigenteDesde\":\"2025-01-15\","
                + "\"reglas\":[{\"identificador\":\"RN-01\",\"parametros\":"
                + "[{\"nombre\":\"dedicacion_tiempo_completo\",\"valor\":40}]},"
                + "{\"identificador\":\"RN-02\",\"parametros\":"
                + "[{\"nombre\":\"horas_maximas_docente\",\"valor\":38.5}]}],"
                + "\"tablaCargaHoraria\":[{\"aplicaA\":\"TODOS\",\"perfil\":\"DOCENTE_TC\","
                + "\"dedicacionTotal\":40,\"horasClase\":"
                + claseTiempoCompleto
                + ",\"horasClaseMinimo\":18,\"horasClaseMaximo\":24,"
                + "\"horasOtrasDocencia\":18,\"horasGestion\":0}"
                + (conGestor ? gestor : "")
                + "]}";
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion) throws Exception {
        return pedir(peticion, vicerrector());
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    private static JwtRequestPostProcessor vicerrector() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_PARAMETROS:LECTURA"));
    }

    // D-67: el Vicerrector académico también escribe la normativa.
    private static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_PARAMETROS:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_PARAMETROS:ESCRITURA"));
    }

    private static VersionNormativa version() {
        Map<String, BigDecimal> dedicaciones = new LinkedHashMap<>();
        dedicaciones.put("dedicacion_tiempo_completo", BigDecimal.valueOf(40));
        return new VersionNormativa(
                1,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                new TablaCargaHoraria(
                        List.of(
                                new FilaCargaHoraria(
                                        null,
                                        "DOCENTE_TC",
                                        40,
                                        22,
                                        18,
                                        24,
                                        18,
                                        0,
                                        null,
                                        Severidad.DURA,
                                        "POL 6",
                                        null),
                                new FilaCargaHoraria(
                                        TipoIst.T2,
                                        "GESTOR",
                                        40,
                                        14,
                                        14,
                                        14,
                                        16,
                                        10,
                                        null,
                                        Severidad.DURA,
                                        "POL 6 (docentes gestores)",
                                        null))),
                List.of(
                        new Regla("RN-01", null, dedicaciones),
                        new Regla(
                                "RN-02",
                                Severidad.DURA,
                                Map.of("horas_maximas_docente", BigDecimal.valueOf(40)))));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        NormativaEnMemoria normativaEnMemoria() {
            return new NormativaEnMemoria();
        }
    }
}
