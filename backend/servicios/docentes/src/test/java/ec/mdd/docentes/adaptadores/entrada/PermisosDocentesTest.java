package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.VerificadorDePermisos;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Cada operación de Docentes exige los permisos de su {@code x-mdd-permiso} (ADR-021, D-70): las de
 * los docentes (F03-H04) y las de los cargos y los proyectos (F03-H05). «Proyectos y horas de
 * participantes: lectura» sola no da acceso a ninguna (D-64, aclaración 8 de F03-H05).
 */
@WebMvcTest({DocentesControlador.class, CargosControlador.class, ProyectosControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    DocentesContratoTest.Dobles.class,
    PermisosDocentesTest.Dobles.class,
    ErroresDocentes.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosDocentesTest {

    private static final String LECTURA =
            "([DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA])";
    private static final String ESCRITURA =
            "([DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA])";
    private static final String LECTURA_F03H05 =
            "([DOCENTES:LECTURA, CATALOGOS:LECTURA, DISTRIBUTIVO:LECTURA])";

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("docentes");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeDocentes_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/docentes/docentes " + LECTURA,
                        "POST /api/docentes/docentes " + ESCRITURA,
                        "GET /api/docentes/docentes/0109990010 " + LECTURA,
                        "PUT /api/docentes/docentes/0109990010 " + ESCRITURA,
                        "PUT /api/docentes/docentes/0109990010/estado " + ESCRITURA,
                        "GET /api/docentes/referencias " + LECTURA,
                        "GET /api/docentes/cargos " + LECTURA_F03H05,
                        "POST /api/docentes/cargos " + ESCRITURA,
                        "PUT /api/docentes/cargos/0109990044/COORDINADOR_CARRERA/2025-04-01 "
                                + ESCRITURA,
                        "GET /api/docentes/proyectos " + LECTURA_F03H05,
                        "POST /api/docentes/proyectos " + ESCRITURA,
                        "GET /api/docentes/proyectos/INV-2025-01 " + LECTURA_F03H05,
                        "PUT /api/docentes/proyectos/INV-2025-01 " + ESCRITURA);

        verificador.verificar(mvc, conversor);
    }

    @Test
    void dadoUnTokenConSoloProyectosLectura_cuandoLlamaCadaOperacion_entoncesRecibe403()
            throws Exception {
        List<String> sin403 = new ArrayList<>();
        for (VerificadorDePermisos.OperacionProtegida operacion : verificador.operaciones()) {
            MockHttpServletRequestBuilder peticion =
                    request(HttpMethod.valueOf(operacion.metodo()), operacion.ruta())
                            .with(
                                    jwt().authorities(
                                                    new SimpleGrantedAuthority(
                                                            "PERMISO_PROYECTOS:LECTURA")));
            if (operacion.conCuerpo()) {
                peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
            }
            int estado = mvc.perform(peticion).andReturn().getResponse().getStatus();
            if (estado != 403) {
                sin403.add(operacion + " → " + estado);
            }
        }

        assertThat(sin403).isEmpty();
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        CargosYProyectosEnMemoria cargosYProyectosEnMemoria(DocentesEnMemoria docentes) {
            return new CargosYProyectosEnMemoria(docentes);
        }

        @Bean
        ConsultarCargos consultarCargos(CargosYProyectosEnMemoria datos) {
            return datos.consultarCargos();
        }

        @Bean
        MantenerCargos mantenerCargos(CargosYProyectosEnMemoria datos) {
            return datos.mantenerCargos();
        }

        @Bean
        ConsultarProyectos consultarProyectos(CargosYProyectosEnMemoria datos) {
            return datos.consultarProyectos();
        }

        @Bean
        MantenerProyectos mantenerProyectos(CargosYProyectosEnMemoria datos) {
            return datos.mantenerProyectos();
        }
    }
}
