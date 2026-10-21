package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
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
 * Las operaciones de {@code /api/academico} cumplen {@code contratos/academico/openapi.yaml}
 * (principio IV), con un doble de los casos de uso escrito a mano (principio VIII).
 */
@WebMvcTest(AcademicoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({AcademicoContratoTest.Dobles.class, ErroresAcademico.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class AcademicoContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private AcademicoEnMemoria academico;

    @BeforeEach
    void reiniciar() {
        academico.reiniciar();
    }

    @Test
    void dadoDosPeriodos_cuandoSeListan_entonces200ConSuEstadoYSuVersion() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/periodos"), lectura());

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"codigo\":\"2026-2\",\"nombre\":\"Periodo 2026-2\","
                                + "\"inicio\":\"2026-09-01\",\"fin\":\"2027-02-28\","
                                + "\"estado\":\"PLANIFICACION\"},"
                                + "{\"codigo\":\"2026-1\",\"nombre\":\"Periodo 2026-1\","
                                + "\"inicio\":\"2026-03-01\",\"fin\":\"2026-08-15\","
                                + "\"estado\":\"VIGENTE\",\"version\":1}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoConVersion_cuandoSeConsulta_entonces200ConSuVersion() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/periodos/2026-1"), lectura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("estado", "VIGENTE")
                .containsEntry("version", 1);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesSinElCampoVersion() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/periodos/2026-2"), lectura());

        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .doesNotContainKey("version");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoQueNoExiste_cuandoSeConsulta_entonces404Comun() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/periodos/2030-1"), lectura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoNuevo_cuandoSeRegistra_entonces201EnPlanificacionSinVersion()
            throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                post("/api/academico/periodos"),
                                "{\"codigo\":\"2027-1\",\"nombre\":\"Periodo 2027-1\","
                                        + "\"inicio\":\"2027-03-01\",\"fin\":\"2027-08-15\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"codigo\":\"2027-1\",\"nombre\":\"Periodo 2027-1\","
                                + "\"inicio\":\"2027-03-01\",\"fin\":\"2027-08-15\","
                                + "\"estado\":\"PLANIFICACION\"}");
        assertThat(academico.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entonces409CodigoRepetido() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                post("/api/academico/periodos"),
                                "{\"codigo\":\"2026-2\",\"nombre\":\"Otro\","
                                        + "\"inicio\":\"2027-03-01\",\"fin\":\"2027-08-15\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_CODIGO_REPETIDO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnFinAntesDelInicio_cuandoSeRegistra_entonces400ConElCampoFin() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                post("/api/academico/periodos"),
                                "{\"codigo\":\"2027-1\",\"nombre\":\"Periodo 2027-1\","
                                        + "\"inicio\":\"2027-08-15\",\"fin\":\"2027-03-01\"}"),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].campo")).isEqualTo("fin");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].descripcion"))
                .isEqualTo(
                        "La fecha de fin (2027-03-01) debe ser posterior a la de inicio"
                                + " (2027-08-15)");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnNombreEnBlanco_cuandoSeRegistra_entonces400ConElCampoNombre() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                post("/api/academico/periodos"),
                                "{\"codigo\":\"2027-1\",\"nombre\":\"   \","
                                        + "\"inicio\":\"2027-03-01\",\"fin\":\"2027-08-15\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(
                        JsonPath.<String>read(
                                resultado.getResponse().getContentAsString(),
                                "$.detalles[0].campo"))
                .isEqualTo("nombre");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeAbre_entonces200ConLaVersion() throws Exception {
        MvcResult resultado =
                pedir(
                        json(put("/api/academico/periodos/2026-2/normativa"), "{\"version\":2}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Integer>read(
                                resultado.getResponse().getContentAsString(), "$.version"))
                .isEqualTo(2);
        assertThat(academico.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoConOtraVersion_cuandoSeAbre_entonces409PeriodoConVersion() throws Exception {
        MvcResult resultado =
                pedir(
                        json(put("/api/academico/periodos/2026-1/normativa"), "{\"version\":2}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_PERIODO_CON_VERSION");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeAbre_entonces400ConElCampoVersion() throws Exception {
        MvcResult resultado =
                pedir(
                        json(put("/api/academico/periodos/2026-2/normativa"), "{\"version\":9}"),
                        escritura());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].campo")).isEqualTo("version");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].descripcion"))
                .isEqualTo("La versión de normativa 9 no existe");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeAbre_entonces404Comun() throws Exception {
        MvcResult resultado =
                pedir(
                        json(put("/api/academico/periodos/2030-1/normativa"), "{\"version\":1}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoAbierto_cuandoPasaAEnAprobacion_entonces200EnSuEstadoNuevo()
            throws Exception {
        academico.conEstado("2026-2", EstadoPeriodo.PLANIFICACION, 2);

        MvcResult resultado = estado("2026-2", "EN_APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.estado"))
                .isEqualTo("EN_APROBACION");
        assertThat(academico.ultimoUsuario()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoPasaAEnAprobacion_entonces409PeriodoSinVersion()
            throws Exception {
        MvcResult resultado = estado("2026-2", "EN_APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_PERIODO_SIN_VERSION");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoEnPlanificacion_cuandoSaltaAVigente_entonces409TransicionNoPermitida()
            throws Exception {
        academico.conEstado("2026-2", EstadoPeriodo.PLANIFICACION, 2);

        MvcResult resultado = estado("2026-2", "VIGENTE");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.codigo"))
                .isEqualTo("ACADEMICO_TRANSICION_NO_PERMITIDA");
        assertThat(
                        JsonPath.<String>read(
                                resultado.getResponse().getContentAsString(), "$.descripcion"))
                .isEqualTo("El periodo solo pasa al estado siguiente, sin saltos ni retrocesos.");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoVigente_cuandoVuelveAPlanificacion_entonces409TransicionNoPermitida()
            throws Exception {
        MvcResult resultado = estado("2026-1", "PLANIFICACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("ACADEMICO_TRANSICION_NO_PERMITIDA");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnEstadoQueNoEsDelContrato_cuandoSeCambia_entonces400() throws Exception {
        MvcResult resultado = estado("2026-2", "ABIERTO");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoVigente_cuandoSeCorrige_entonces200ConSusDatosNuevos() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                put("/api/academico/periodos/2026-1"),
                                "{\"nombre\":\"Periodo académico 2026-1\","
                                        + "\"inicio\":\"2026-03-01\",\"fin\":\"2026-08-31\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("nombre", "Periodo académico 2026-1")
                .containsEntry("fin", "2026-08-31")
                .containsEntry("estado", "VIGENTE")
                .containsEntry("version", 1);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeIntentaCambiarlo_entonces409PeriodoCerrado()
            throws Exception {
        academico.conEstado("2026-1", EstadoPeriodo.CERRADO, null);

        for (MockHttpServletRequestBuilder peticion :
                new MockHttpServletRequestBuilder[] {
                    json(
                            put("/api/academico/periodos/2026-1"),
                            "{\"nombre\":\"Otro\",\"inicio\":\"2026-03-01\","
                                    + "\"fin\":\"2026-08-15\"}"),
                    json(put("/api/academico/periodos/2026-1/estado"), "{\"estado\":\"VIGENTE\"}"),
                    json(put("/api/academico/periodos/2026-1/normativa"), "{\"version\":1}")
                }) {
            MvcResult resultado = pedir(peticion, escritura());

            assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("ACADEMICO_PERIODO_CERRADO");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entonces200ConTodosSusDatos() throws Exception {
        academico.conEstado("2026-1", EstadoPeriodo.CERRADO, null);

        MvcResult resultado = pedir(get("/api/academico/periodos/2026-1"), lectura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("estado", "CERRADO")
                .containsEntry("version", 1)
                .containsEntry("nombre", "Periodo 2026-1");
        contrato.validar(resultado);
    }

    private MvcResult estado(String codigo, String destino) throws Exception {
        return pedir(
                json(
                        put("/api/academico/periodos/" + codigo + "/estado"),
                        "{\"estado\":\"" + destino + "\"}"),
                escritura());
    }

    static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder peticion, String cuerpo) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    /** El Rector: lee el distributivo del periodo, pero no lo escribe (D-65). */
    static JwtRequestPostProcessor lectura() {
        return jwt().jwt(token -> token.subject("rectorado.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    /** El Vicerrector académico: lee y escribe los periodos (D-69). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:ESCRITURA"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        AcademicoEnMemoria academicoEnMemoria() {
            return new AcademicoEnMemoria();
        }
    }
}
