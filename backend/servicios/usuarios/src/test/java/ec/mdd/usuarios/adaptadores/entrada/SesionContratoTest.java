package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.nimbusds.jose.jwk.JWKSet;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Las respuestas de Usuarios cumplen {@code contratos/usuarios/openapi.yaml} (principio IV), con la
 * seguridad de los procesos y un doble del caso de uso escrito a mano: el principio VIII solo
 * admite Mockito en la capa de aplicación.
 */
@WebMvcTest({SesionControlador.class, JwksControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
// @WebMvcTest no escanea los @Component comunes: el catálogo del servicio se importa.
@Import({SesionContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks",
            "mdd.seguridad.rutas-publicas[0]=POST /api/usuarios/sesion",
            "mdd.seguridad.rutas-publicas[1]=GET /api/usuarios/jwks"
        })
class SesionContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @Test
    void dadoCredencialesCorrectas_cuandoIniciaSesion_entonces200SegunElContrato()
            throws Exception {
        MvcResult resultado = iniciar("correcta");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"tipo\":\"Bearer\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoCredencialesInvalidas_cuandoIniciaSesion_entonces401SegunElContrato()
            throws Exception {
        MvcResult resultado = iniciar("incorrecta");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("USUARIOS_CREDENCIALES_INVALIDAS");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnFalloTecnico_cuandoIniciaSesion_entonces500ConIncidenteSegunElContrato()
            throws Exception {
        MvcResult resultado = iniciar("explota");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_ERROR_INTERNO\"")
                .contains("\"idIncidente\":")
                .doesNotContain("eyJ")
                .doesNotContain("IllegalStateException");
        contrato.validar(resultado);
    }

    @Test
    void dadoDatosVacios_cuandoIniciaSesion_entonces400ConUnDetallePorCampo() throws Exception {
        MvcResult resultado = iniciarCon("{\"usuario\":\"\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"")
                .contains("{\"campo\":\"usuario\",\"descripcion\":\"es obligatorio\"}")
                .contains("{\"campo\":\"clave\",\"descripcion\":\"es obligatorio\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaClaveVacia_cuandoIniciaSesion_entonces400ConUnSoloDetalle() throws Exception {
        MvcResult resultado = iniciarCon("{\"usuario\":\"autor.prueba\",\"clave\":\"\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("{\"campo\":\"clave\",\"descripcion\":\"es obligatorio\"}")
                .doesNotContain("\"campo\":\"usuario\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenConRoles_cuandoConsultaLaSesion_entoncesUsuarioYRolesOrdenados()
            throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/usuarios/sesion")
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
                                get("/api/usuarios/sesion")
                                        .with(jwt().jwt(token -> token.subject("sin.roles"))))
                        .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).contains("\"roles\":[]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaPeticionSinToken_cuandoConsultaLaSesion_entonces401SegunElContrato()
            throws Exception {
        MvcResult resultado = mvc.perform(get("/api/usuarios/sesion")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("SEGURIDAD_NO_AUTENTICADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoElJwks_cuandoSeConsulta_entoncesSoloLlevaLaLlavePublicaSegunElContrato()
            throws Exception {
        MvcResult resultado = mvc.perform(get("/api/usuarios/jwks")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"alg\":\"RS256\"")
                .doesNotContain("\"d\"");
        contrato.validar(resultado);
    }

    private MvcResult iniciar(String clave) throws Exception {
        return iniciarCon("{\"usuario\":\"autor.prueba\",\"clave\":\"" + clave + "\"}");
    }

    private MvcResult iniciarCon(String cuerpo) throws Exception {
        return mvc.perform(
                        post("/api/usuarios/sesion")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        IniciarSesion iniciarSesion() {
            return new IniciarSesionDePrueba();
        }

        @Bean
        JWKSet jwks() {
            TokensDePrueba tokens = new TokensDePrueba();
            return new JWKSet(
                    new com.nimbusds.jose.jwk.RSAKey.Builder(tokens.llave())
                            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                            .build());
        }
    }

    /** Doble del caso de uso: acepta solo la contraseña «correcta». */
    static final class IniciarSesionDePrueba implements IniciarSesion {

        @Override
        public TokenEmitido iniciar(String usuario, String clave) {
            if ("explota".equals(clave)) {
                throw new IllegalStateException(
                        "fallo técnico con eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.firma");
            }
            if ("correcta".equals(clave)) {
                return new TokenEmitido(
                        "cabecera.cuerpo.firma", Instant.parse("2026-10-13T12:00:00Z"));
            }
            throw new CredencialesInvalidas();
        }
    }
}
