package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.academico.puertos.entrada.AsignaturaConCampos;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
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

/** La consulta interna de las asignaturas ofertadas cumple su contrato (ADR-023). */
@WebMvcTest(InternoAcademicoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoAcademicoContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoAcademicoContratoTest {

    private static final String RUTA = "/interno/academico/periodos/%s/asignaturas-ofertadas";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnProceso_cuandoConsulta_entonces200SoloConLoQueNecesitaLaAfinidad() throws Exception {
        MvcResult resultado = pedir("2026-I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"codigo\":\"POO-204\",\"nombre\":\"Programación\","
                                + "\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\"}]")
                .doesNotContain("palabrasClave", "troncoComun");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinOferta_cuandoConsulta_entoncesListaVacia() throws Exception {
        MvcResult resultado = pedir("2026-II", deProceso());

        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("[]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoConsulta_entonces404() throws Exception {
        MvcResult resultado = pedir("NADA", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCodigoMalEscrito_cuandoConsulta_entonces400() throws Exception {
        MvcResult resultado = pedir("2026 I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado = pedir("2026-I", jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoTieneToken_cuandoConsulta_entonces401() throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA.formatted("2026-I"))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
    }

    private MvcResult pedir(String periodo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo)).with(token)).andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:distributivo"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarAsignaturasOfertadas consultarAsignaturasOfertadas() {
            return periodo ->
                    switch (periodo) {
                        case "2026-I" ->
                                List.of(
                                        new AsignaturaConCampos(
                                                "POO-204", "Programación", "06", "061"));
                        case "2026-II" -> List.of();
                        default -> throw new PeriodoNoEncontrado(periodo);
                    };
        }
    }
}
