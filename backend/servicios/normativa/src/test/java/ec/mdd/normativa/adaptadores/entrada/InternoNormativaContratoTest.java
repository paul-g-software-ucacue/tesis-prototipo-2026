package ec.mdd.normativa.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Componente;
import ec.mdd.normativa.dominio.MinimoAfinidad;
import ec.mdd.normativa.dominio.PesosAfinidad;
import ec.mdd.normativa.puertos.entrada.ActividadesDeLaVersion;
import ec.mdd.normativa.puertos.entrada.ConsultarActividadesDelPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarPesosDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.PesosDeLaVersion;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
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

/** La consulta interna de los pesos de afinidad cumple su contrato (ADR-023). */
@WebMvcTest(InternoNormativaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoNormativaContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoNormativaContratoTest {

    private static final String RUTA = "/interno/normativa/periodos/%s/pesos-de-afinidad";
    private static final String RUTA_ACTIVIDADES = "/interno/normativa/periodos/%s/actividades";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("normativa");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnProceso_cuandoConsulta_entonces200ConLaVersionSusPesosYSuMinimo() throws Exception {
        MvcResult resultado = pedir("2026-I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"version\":2,\"pesos\":{"
                                + "\"cuartoNivelCampoEspecifico\":7,\"cuartoNivelCampoAmplio\":4,"
                                + "\"tercerNivelCampoEspecifico\":5,\"tercerNivelCampoAmplio\":3,"
                                + "\"experienciaRelacionada\":2.5,\"cursoRelacionado\":1.5,"
                                + "\"topeCursos\":4},\"minimoAfinidad\":2.5}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoConsulta_entonces404() throws Exception {
        MvcResult resultado = pedir("2026-II", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado = pedir("2026-I", jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnProceso_cuandoConsultaLasActividades_entonces200ConLoQueExigeCadaUna()
            throws Exception {
        MvcResult resultado = pedirActividades("2026-I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"version\":2,\"actividades\":["
                                + "{\"codigo\":\"140-b\",\"nombre\":\"Planificar\","
                                + "\"componente\":\"OTRAS_DOCENCIA\",\"tiposDeProyecto\":[],"
                                + "\"cargos\":[]},"
                                + "{\"codigo\":\"141-a\",\"nombre\":\"Investigar\","
                                + "\"componente\":\"INVESTIGACION\","
                                + "\"tiposDeProyecto\":[\"INVESTIGACION\"],\"cargos\":[]},"
                                + "{\"codigo\":\"143-d\",\"nombre\":\"Coordinar\","
                                + "\"componente\":\"GESTION\",\"tiposDeProyecto\":[],"
                                + "\"cargos\":[\"COORDINADOR_CARRERA\"]}]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoConsultaLasActividades_entonces404() throws Exception {
        MvcResult resultado = pedirActividades("2026-II", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaLasActividades_entonces403() throws Exception {
        MvcResult resultado = pedirActividades("2026-I", jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    private MvcResult pedirActividades(String periodo, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(get(RUTA_ACTIVIDADES.formatted(periodo)).with(token)).andReturn();
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
        ConsultarActividadesDelPeriodo consultarActividadesDelPeriodo() {
            return periodo -> {
                if (!"2026-I".equals(periodo)) {
                    throw new PeriodoSinVersion(periodo);
                }
                return new ActividadesDeLaVersion(
                        2,
                        List.of(
                                new ActividadDelCatalogo(
                                        "140-b",
                                        "Planificar",
                                        Componente.OTRAS_DOCENCIA,
                                        List.of(),
                                        List.of()),
                                new ActividadDelCatalogo(
                                        "141-a",
                                        "Investigar",
                                        Componente.INVESTIGACION,
                                        List.of("INVESTIGACION"),
                                        List.of()),
                                new ActividadDelCatalogo(
                                        "143-d",
                                        "Coordinar",
                                        Componente.GESTION,
                                        List.of(),
                                        List.of("COORDINADOR_CARRERA"))));
            };
        }

        @Bean
        ConsultarPesosDelPeriodo consultarPesosDelPeriodo() {
            return periodo -> {
                if (!"2026-I".equals(periodo)) {
                    throw new PeriodoSinVersion(periodo);
                }
                return new PesosDeLaVersion(
                        2,
                        new PesosAfinidad(
                                new BigDecimal("7"),
                                new BigDecimal("4"),
                                new BigDecimal("5"),
                                new BigDecimal("3"),
                                new BigDecimal("2.5"),
                                new BigDecimal("1.5"),
                                new BigDecimal("4")),
                        new MinimoAfinidad(new BigDecimal("2.5")));
            };
        }
    }
}
