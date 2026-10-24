package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** La sesión actual de cargas cumple {@code contratos/cargas/openapi.yaml} (principio IV, D-39). */
@WebMvcTest(SesionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class SesionContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnTokenConRoles_cuandoConsultaLaSesion_entoncesUsuarioYRolesOrdenados()
            throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/cargas/sesion")
                                        .with(
                                                jwt().jwt(
                                                                token ->
                                                                        token.subject(
                                                                                        "autor.prueba")
                                                                                .claim(
                                                                                        "roles",
                                                                                        List.of(
                                                                                                "DOCENTE",
                                                                                                "COORDINADOR_CARRERA")))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"usuario\":\"autor.prueba\",\"roles\":[\"COORDINADOR_CARRERA\",\"DOCENTE\"]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenSinRoles_cuandoConsultaLaSesion_entoncesRolesVacios() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/cargas/sesion")
                                        .with(jwt().jwt(token -> token.subject("sin.roles"))))
                        .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).contains("\"roles\":[]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaPeticionSinToken_cuandoConsultaLaSesion_entonces401SegunElContrato()
            throws Exception {
        MvcResult resultado = mvc.perform(get("/api/cargas/sesion")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("SEGURIDAD_NO_AUTENTICADO");
        contrato.validar(resultado);
    }
}
