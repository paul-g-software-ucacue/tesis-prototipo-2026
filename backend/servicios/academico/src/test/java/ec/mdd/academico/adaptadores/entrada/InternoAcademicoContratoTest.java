package ec.mdd.academico.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.puertos.entrada.AsignaturaConCampos;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.ConsultarParaleloDeLaOferta;
import ec.mdd.academico.puertos.entrada.ParaleloConCampos;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

/** Las consultas internas de la oferta cumplen su contrato y dan lo mínimo (ADR-023). */
@WebMvcTest(InternoAcademicoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoAcademicoContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoAcademicoContratoTest {

    private static final String RUTA = "/interno/academico/periodos/%s/asignaturas-ofertadas";
    private static final String RUTA_PARALELO = "/interno/academico/periodos/%s/oferta/%s/%s";

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

    @Test
    void dadoUnParalelo_cuandoConsulta_entonces200SoloConLoQueNecesitaLaSugerencia()
            throws Exception {
        MvcResult resultado = pedirParalelo("2026-I", "POO-204", "A_NOCTURNA", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"asignatura\":\"POO-204\",\"nombre\":\"Programación\","
                                + "\"carrera\":\"TDS\",\"paralelo\":\"A_NOCTURNA\","
                                + "\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\","
                                + "\"troncoComun\":true,\"horasSemanales\":2.5,"
                                + "\"estado\":\"OFERTADA\"}")
                .doesNotContain("palabrasClave", "estudiantes", "sede");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnParaleloRetirado_cuandoConsulta_entonces200ConSuEstado() throws Exception {
        MvcResult resultado = pedirParalelo("2026-I", "POO-204", "B", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"estado\":\"RETIRADA\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnParaleloQueNoEstaEnLaOferta_cuandoConsulta_entonces404() throws Exception {
        MvcResult resultado = pedirParalelo("2026-I", "POO-204", "Z", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoConsultaUnParalelo_entonces404() throws Exception {
        MvcResult resultado = pedirParalelo("NADA", "POO-204", "A", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnParaleloMalEscrito_cuandoConsulta_entonces400() throws Exception {
        MvcResult resultado = pedirParalelo("2026-I", "POO-204", "a b", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaUnParalelo_entonces403() throws Exception {
        MvcResult resultado = pedirParalelo("2026-I", "POO-204", "B", jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    private MvcResult pedir(String periodo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo)).with(token)).andReturn();
    }

    private MvcResult pedirParalelo(
            String periodo, String asignatura, String paralelo, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(get(RUTA_PARALELO.formatted(periodo, asignatura, paralelo)).with(token))
                .andReturn();
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

        @Bean
        ConsultarParaleloDeLaOferta consultarParaleloDeLaOferta() {
            return (periodo, asignatura, paralelo) -> {
                if (!"2026-I".equals(periodo)) {
                    throw new PeriodoNoEncontrado(periodo);
                }
                return switch (paralelo) {
                    case "A_NOCTURNA" -> Optional.of(uno(EstadoDeAsignaturaParalelo.OFERTADA));
                    case "B" -> Optional.of(uno(EstadoDeAsignaturaParalelo.RETIRADA));
                    default -> Optional.empty();
                };
            };
        }

        private static ParaleloConCampos uno(EstadoDeAsignaturaParalelo estado) {
            return new ParaleloConCampos(
                    "POO-204",
                    "Programación",
                    "TDS",
                    "A_NOCTURNA",
                    "06",
                    "061",
                    true,
                    new BigDecimal("2.5"),
                    estado);
        }
    }
}
