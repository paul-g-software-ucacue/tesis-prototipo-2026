package ec.mdd.academico.adaptadores.entrada;

import static ec.mdd.academico.adaptadores.entrada.AcademicoContratoTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las operaciones de la estructura orgánica cumplen {@code contratos/academico/openapi.yaml}
 * (principio IV, F03-H01), con un doble de los casos de uso escrito a mano (principio VIII).
 */
@WebMvcTest(EstructuraControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({EstructuraContratoTest.Dobles.class, ErroresAcademico.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EstructuraContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private EstructuraEnMemoria estructura;

    @BeforeEach
    void reiniciar() {
        estructura.reiniciar();
    }

    @Test
    void dadaLaEstructura_cuandoSeLista_entonces200PorCodigoConSuPadre() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/unidades"), lectura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(cuerpo, "$[*].codigo"))
                .containsExactly("COM-EVAL", "G-CAL", "MATRIZ", "RECT");
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$[2]"))
                .doesNotContainKeys("padre", "gestionAutorizada", "comisionPermanente");
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$[0]"))
                .containsEntry("tipo", "COMISION")
                .containsEntry("padre", "G-CAL")
                .containsEntry("comisionPermanente", true)
                .containsEntry("activa", true);
        contrato.validar(resultado);
    }

    @Test
    void dadoElCatalogo_cuandoSeLista_entoncesTraeLas21() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/gestiones-autorizadas"), catalogos());

        assertThat(
                        JsonPath.<List<String>>read(
                                resultado.getResponse().getContentAsString(), "$[*].codigo"))
                .hasSize(21)
                .startsWith("G01")
                .endsWith("G21");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaUnidad_cuandoSeConsulta_entonces200ConSuGestion() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/unidades/RECT"), lectura());

        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("gestionAutorizada", "G01")
                .containsEntry("tipo", "GOBIERNO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoQueNoExiste_cuandoSeConsultaOSeCambia_entonces404Comun() throws Exception {
        for (MvcResult resultado :
                List.of(
                        pedir(get("/api/academico/unidades/NADA"), lectura()),
                        pedir(
                                json(put("/api/academico/unidades/NADA"), "{\"nombre\":\"Nada\"}"),
                                escritura()),
                        pedir(
                                json(
                                        put("/api/academico/unidades/NADA/estado"),
                                        "{\"activa\":false}"),
                                escritura()))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("COMUN_RECURSO_NO_ENCONTRADO");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaUnaGestionNueva_cuandoSeRegistra_entonces201Activa() throws Exception {
        MvcResult resultado =
                registrar(
                        "{\"codigo\":\"G-INV\",\"nombre\":\"Gestión de Investigación\","
                                + "\"tipo\":\"GESTION\",\"padre\":\"RECT\","
                                + "\"gestionAutorizada\":\"G07\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("codigo", "G-INV")
                .containsEntry("gestionAutorizada", "G07")
                .containsEntry("activa", true);
        assertThat(estructura.ultimoUsuario()).isEqualTo("admin.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entonces409UnidadRepetida() throws Exception {
        MvcResult resultado =
                registrar(
                        "{\"codigo\":\"RECT\",\"nombre\":\"Otro\",\"tipo\":\"GOBIERNO\","
                                + "\"padre\":\"MATRIZ\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_UNIDAD_REPETIDA");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaSegundaSedeMatriz_cuandoSeRegistra_entonces409SedeMatrizExistente()
            throws Exception {
        MvcResult resultado =
                registrar("{\"codigo\":\"MATRIZ2\",\"nombre\":\"Otra\",\"tipo\":\"SEDE_MATRIZ\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_SEDE_MATRIZ_EXISTENTE");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPadreQueNoExiste_cuandoSeRegistra_entonces400ConElCampoPadre() throws Exception {
        MvcResult resultado =
                registrar(
                        "{\"codigo\":\"OCS\",\"nombre\":\"Consejo\",\"tipo\":\"GOBIERNO\","
                                + "\"padre\":\"NADA\"}");

        invalido(resultado, "padre", "La unidad NADA no existe");
    }

    @Test
    void dadoUnPadreQueFormaUnCiclo_cuandoSeCorrige_entonces400ConElCampoPadre() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                put("/api/academico/unidades/G-CAL"),
                                "{\"nombre\":\"Calidad\",\"padre\":\"COM-EVAL\","
                                        + "\"gestionAutorizada\":\"G10\"}"),
                        escritura());

        invalido(
                resultado,
                "padre",
                "La unidad no puede depender de COM-EVAL, porque COM-EVAL depende de ella");
    }

    @Test
    void dadaUnaUnidad_cuandoSeCorrige_entonces200ConLoNuevo() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                put("/api/academico/unidades/G-CAL"),
                                "{\"nombre\":\"Gestión de la Calidad\",\"padre\":\"MATRIZ\","
                                        + "\"gestionAutorizada\":\"G10\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("nombre", "Gestión de la Calidad")
                .containsEntry("padre", "MATRIZ")
                .containsEntry("tipo", "GESTION");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaUnidadConDependientes_cuandoSeInactiva_entonces409YSinEllosSi() throws Exception {
        MvcResult conDependientes = estado("G-CAL", false);

        assertThat(conDependientes.getResponse().getStatus()).isEqualTo(409);
        assertThat(conDependientes.getResponse().getContentAsString())
                .contains("ACADEMICO_UNIDAD_CON_DEPENDIENTES");
        contrato.validar(conDependientes);

        MvcResult hoja = estado("COM-EVAL", false);
        assertThat(hoja.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(hoja.getResponse().getContentAsString(), "$.activa"))
                .isFalse();
        contrato.validar(hoja);
        assertThat(estado("G-CAL", false).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoElPadreInactivo_cuandoSeReactiva_entonces400ConElCampoPadre() throws Exception {
        estado("COM-EVAL", false);
        estado("G-CAL", false);

        invalido(
                estado("COM-EVAL", true),
                "padre",
                "La unidad G-CAL de la que depende está inactiva: reactívala antes");
    }

    @Test
    void dadoUnTipoQueNoExiste_cuandoSeRegistra_entonces400() throws Exception {
        MvcResult resultado =
                registrar(
                        "{\"codigo\":\"OFI\",\"nombre\":\"Oficina\",\"tipo\":\"OFICINA\","
                                + "\"padre\":\"RECT\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(resultado);
    }

    private MvcResult registrar(String cuerpo) throws Exception {
        return pedir(json(post("/api/academico/unidades"), cuerpo), escritura());
    }

    private MvcResult estado(String codigo, boolean activa) throws Exception {
        return pedir(
                json(
                        put("/api/academico/unidades/" + codigo + "/estado"),
                        "{\"activa\":" + activa + "}"),
                escritura());
    }

    void invalido(MvcResult resultado, String campo, String descripcion) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("COMUN_SOLICITUD_INVALIDA");
        assertThat(JsonPath.<List<Map<String, Object>>>read(cuerpo, "$.detalles"))
                .containsExactly(Map.of("campo", campo, "descripcion", descripcion));
        contrato.validar(resultado);
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    /** El Rector: lee el distributivo del periodo y, con él, la estructura (D-70). */
    static JwtRequestPostProcessor lectura() {
        return jwt().jwt(token -> token.subject("rectorado.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    /** Solo «Catálogos y datos maestros: lectura»: también lee la estructura (D-70). */
    static JwtRequestPostProcessor catalogos() {
        return jwt().jwt(token -> token.subject("catalogos.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_CATALOGOS:LECTURA"));
    }

    /** El Administrador del sistema: lee y mantiene la estructura (D-16, aclaración 1). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("admin.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_CATALOGOS:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_CATALOGOS:ESCRITURA"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        EstructuraEnMemoria estructuraEnMemoria() {
            return new EstructuraEnMemoria();
        }
    }
}
