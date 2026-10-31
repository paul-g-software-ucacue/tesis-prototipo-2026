package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El evento de prueba cumple {@code contratos/generacion/openapi.yaml} (D-48): encendido responde
 * 202 con el identificador; apagado, la ruta no existe.
 */
class EventosPruebaContratoTest {

    private static final UUID ID = UUID.fromString("3f0e6c3a-8f2b-4c1d-9e5a-7b6c5d4e3f21");

    private static final ValidadorDeContrato CONTRATO = ValidadorDeContrato.para("generacion");

    @Nested
    @WebMvcTest(EventosPruebaControlador.class)
    @ImportAutoConfiguration({
        SeguridadProcesoAutoConfiguracion.class,
        ErroresAutoConfiguracion.class
    })
    @Import(Dobles.class)
    @TestPropertySource(
            properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks",
                "mdd.generacion.eventos-prueba.habilitado=true"
            })
    class Encendido {

        @Autowired private MockMvc mvc;

        @Test
        void dadoUnUsuario_cuandoPublicaElEventoDePrueba_entonces202ConSuIdentificador()
                throws Exception {
            MvcResult resultado =
                    mvc.perform(post("/api/generacion/eventos-prueba").with(jwt())).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(202);
            assertThat(resultado.getResponse().getContentAsString())
                    .isEqualTo("{\"idEvento\":\"" + ID + "\"}");
            CONTRATO.validar(resultado);
        }

        @Test
        void dadoUnaPeticionSinToken_cuandoPublica_entonces401() throws Exception {
            MvcResult resultado = mvc.perform(post("/api/generacion/eventos-prueba")).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            CONTRATO.validar(resultado);
        }
    }

    @Nested
    @WebMvcTest(EventosPruebaControlador.class)
    @ImportAutoConfiguration({
        SeguridadProcesoAutoConfiguracion.class,
        ErroresAutoConfiguracion.class
    })
    @Import(Dobles.class)
    @TestPropertySource(
            properties =
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
    class Apagado {

        @Autowired private MockMvc mvc;

        @Test
        void dadoElEndpointApagado_cuandoSePublica_entonces404ComoUnaRutaInexistente()
                throws Exception {
            MvcResult resultado =
                    mvc.perform(post("/api/generacion/eventos-prueba").with(jwt())).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"COMUN_RECURSO_NO_ENCONTRADO\"");
            CONTRATO.validar(resultado);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        PublicarEventoDePrueba publicarEventoDePrueba() {
            return () -> ID;
        }
    }
}
