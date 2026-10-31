package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentesActivos;
import ec.mdd.docentes.puertos.entrada.ConsultarFormacion;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
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

/**
 * Las consultas internas de Docentes cumplen su contrato y dan lo mínimo (ADR-023, principio IX).
 */
@WebMvcTest(InternoDocentesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoDocentesContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoDocentesContratoTest {

    private static final String RUTA = "/interno/docentes/%s/formacion";
    private static final String RUTA_ACTIVOS = "/interno/docentes/activos";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("docentes");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnProceso_cuandoConsulta_entonces200SoloConLaFormacion() throws Exception {
        MvcResult resultado = pedir(DocentesDePrueba.CEDULA, deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"tercerNivel\":{\"nombre\":\"INGENIERO DE SISTEMAS\","
                                + "\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\"},"
                                + "\"cuartoNivel\":[{\"nombre\":\"MÁSTER EN TELEMÁTICA\","
                                + "\"campoEspecifico\":\"061\"}],"
                                + "\"experiencia\":[{\"nombre\":\"DESARROLLADOR\","
                                + "\"campoEspecifico\":\"061\"}],"
                                + "\"cursos\":[{\"nombre\":\"PROGRAMACIÓN EN JAVA\","
                                + "\"campoEspecifico\":\"061\"},"
                                + "{\"nombre\":\"DOCENCIA UNIVERSITARIA\","
                                + "\"campoEspecifico\":\"011\"}]}")
                .doesNotContain(
                        "correo",
                        "telefono",
                        "dedicacion",
                        "EMPRESA XYZ",
                        "registroSenescyt",
                        "asignaturasAfines");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoConsulta_entonces404() throws Exception {
        MvcResult resultado = pedir("0000000000", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaIdentificacionMalEscrita_cuandoConsulta_entonces400() throws Exception {
        MvcResult resultado = pedir("abc", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado = pedir(DocentesDePrueba.CEDULA, jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnProceso_cuandoConsultaLosActivos_entonces200SoloConLoNecesario() throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA_ACTIVOS).with(deProceso())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .startsWith(
                        "[{\"identificacion\":\"0109990010\","
                                + "\"nombre\":\"PERALTA MALDONADO JUAN ANDRES\","
                                + "\"dedicacion\":\"TC\",\"horasContratadas\":40,"
                                + "\"formacion\":{\"tercerNivel\":")
                .doesNotContain(
                        "correo",
                        "telefono",
                        "tipoPersonal",
                        "fechaIngreso",
                        "EMPRESA XYZ",
                        "registroSenescyt",
                        "asignaturasAfines",
                        "carreraBase");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaLosActivos_entonces403() throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA_ACTIVOS).with(jwt())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    private MvcResult pedir(String identificacion, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(RUTA.formatted(identificacion)).with(token)).andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:distributivo"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarFormacion consultarFormacion() {
            return identificacion -> {
                if (!DocentesDePrueba.CEDULA.equals(identificacion)) {
                    throw new DocenteNoEncontrado();
                }
                return DocentesDePrueba.ficha();
            };
        }

        @Bean
        ConsultarDocentesActivos consultarDocentesActivos() {
            return () -> List.of(DocentesDePrueba.docente());
        }
    }
}
