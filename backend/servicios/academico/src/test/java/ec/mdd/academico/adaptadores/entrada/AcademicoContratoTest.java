package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
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
