package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarPublicaciones;
import ec.mdd.validacionocs.puertos.entrada.ConsultarPublicaciones.PublicacionVigente;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

/** La consulta interna de las publicaciones vigentes cumple su contrato (F20-H01, ADR-023). */
@WebMvcTest(InternoValidacionocsControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoValidacionocsContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoValidacionocsContratoTest {

    private static final String RUTA = "/interno/validacionocs/publicaciones";
    private static final List<PublicacionVigente> PUBLICACIONES = new ArrayList<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        PUBLICACIONES.clear();
    }

    @Test
    void sinPublicaciones_cuandoUnProcesoConsulta_entonces200ConLaListaVacia() throws Exception {
        MvcResult resultado = pedir(deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("[]");
        contrato.validar(resultado);
    }

    @Test
    void conUnaPublicacionEnReforma_cuandoUnProcesoConsulta_entonces200ConLaVigente()
            throws Exception {
        PUBLICACIONES.add(
                new PublicacionVigente(
                        "2026-I",
                        1,
                        "d".repeat(64),
                        "secretaria.demo",
                        Instant.parse("2026-11-18T15:00:00Z"),
                        true));

        MvcResult resultado = pedir(deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"periodo\":\"2026-I\",\"version\":1,\"hash\":\""
                                + "d".repeat(64)
                                + "\",\"publicadaPor\":\"secretaria.demo\","
                                + "\"publicadaEn\":\"2026-11-18T15:00:00Z\",\"enReforma\":true}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado = pedir(jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    private MvcResult pedir(JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(RUTA).with(token)).andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:salida"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarPublicaciones consultarPublicaciones() {
            return () -> List.copyOf(PUBLICACIONES);
        }
    }
}
