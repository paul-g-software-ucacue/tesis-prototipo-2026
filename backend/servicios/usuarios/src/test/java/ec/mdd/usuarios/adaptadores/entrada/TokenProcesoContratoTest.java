package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
import ec.mdd.usuarios.puertos.entrada.EmitirTokenDeProceso;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code POST /interno/usuarios/token-proceso} cumple el contrato de Usuarios (D-49, ADR-018), con
 * un doble del caso de uso escrito a mano.
 */
@WebMvcTest(TokenProcesoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({TokenProcesoContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks",
            "mdd.seguridad.rutas-publicas[0]=POST /interno/usuarios/token-proceso"
        })
@ExtendWith(OutputCaptureExtension.class)
class TokenProcesoContratoTest {

    private static final String CLAVE = "clave-del-proceso";
    private static final String TOKEN = "cabecera.cuerpo.firma";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @Test
    void dadoLaClaveCorrecta_cuandoPideElToken_entonces200SegunElContrato(CapturedOutput salida)
            throws Exception {
        MvcResult resultado = pedir("{\"proceso\":\"distributivo\",\"clave\":\"" + CLAVE + "\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"token\":\"" + TOKEN + "\"")
                .contains("\"tipo\":\"Bearer\"");
        contrato.validar(resultado);
        assertThat(salida.getAll()).doesNotContain(CLAVE).doesNotContain(TOKEN);
    }

    @Test
    void dadoUnaClaveIncorrectaOUnProcesoDesconocido_cuandoPideElToken_entoncesElMismo401()
            throws Exception {
        MvcResult incorrecta = pedir("{\"proceso\":\"distributivo\",\"clave\":\"otra\"}");
        MvcResult desconocido = pedir("{\"proceso\":\"salida\",\"clave\":\"" + CLAVE + "\"}");

        assertThat(incorrecta.getResponse().getStatus()).isEqualTo(401);
        assertThat(incorrecta.getResponse().getContentAsString())
                .contains("USUARIOS_CREDENCIALES_INVALIDAS")
                .isEqualTo(desconocido.getResponse().getContentAsString());
        assertThat(desconocido.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(incorrecta);
        contrato.validar(desconocido);
    }

    @Test
    void dadoDatosVacios_cuandoPideElToken_entonces400ConDetalles() throws Exception {
        MvcResult resultado = pedir("{\"proceso\":\"\",\"clave\":\"\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"")
                .contains("\"campo\":\"proceso\"")
                .contains("\"campo\":\"clave\"");
        contrato.validar(resultado);
    }

    private MvcResult pedir(String cuerpo) throws Exception {
        return mvc.perform(
                        post("/interno/usuarios/token-proceso")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        EmitirTokenDeProceso emitirTokenDeProceso() {
            return (proceso, clave) -> {
                if ("distributivo".equals(proceso) && CLAVE.equals(clave)) {
                    return new TokenEmitido(TOKEN, Instant.parse("2026-10-14T12:05:00Z"));
                }
                throw new CredencialesInvalidas();
            };
        }
    }
}
