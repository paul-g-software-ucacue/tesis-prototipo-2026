package ec.mdd.roles.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.roles.puertos.entrada.Autorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarFirmantes;
import ec.mdd.roles.puertos.entrada.Firmantes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code GET /interno/roles/autorizaciones/{usuario}} cumple el contrato de Roles: solo con un
 * token de proceso (ADR-018). Un token de usuario recibe el 403 común (ADR-021).
 */
@WebMvcTest(AutorizacionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({AutorizacionContratoTest.Dobles.class, ErroresRoles.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class AutorizacionContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("roles");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnTokenDeProceso_cuandoConsultaLosFirmantes_entonces200SinUsuarios() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/interno/roles/firmantes?periodo=2026-I")
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_PROCESO"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"rolesConUsuario\":[\"RECTOR\",\"VICERRECTOR\"],"
                                + "\"responsableDesignado\":true}");
        contrato.validar(resultado);
    }

    // F13-H02: el Responsable del periodo y su nombre, para el bloque de firmas.
    @Test
    void dadoUnTokenDeProceso_cuandoConsultaElResponsable_entonces200ConUsuarioYNombre()
            throws Exception {
        MvcResult resultado =
                mvc.perform(get("/interno/roles/periodos/2026-I/responsable").with(proceso()))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("{\"usuario\":\"consolidacion.demo\",\"nombre\":\"PÉREZ ANA\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinResponsable_cuandoSeConsulta_entonces404DelCatalogo() throws Exception {
        MvcResult resultado =
                mvc.perform(get("/interno/roles/periodos/2025-II/responsable").with(proceso()))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"ROLES_SIN_RESPONSABLE\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaElResponsable_entonces403() throws Exception {
        assertThat(
                        mvc.perform(get("/interno/roles/periodos/2026-I/responsable").with(jwt()))
                                .andReturn()
                                .getResponse()
                                .getStatus())
                .isEqualTo(403);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor proceso() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaLosFirmantes_entonces403() throws Exception {
        MvcResult resultado =
                mvc.perform(get("/interno/roles/firmantes?periodo=2026-I").with(jwt())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void dadoUnTokenDeProceso_cuandoConsultaLaAutorizacion_entonces200SegunElContrato()
            throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/interno/roles/autorizaciones/docente.coordinador.demo")
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_PROCESO"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"usuario\":\"docente.coordinador.demo\","
                                + "\"roles\":[\"COORDINADOR_CARRERA\",\"DOCENTE\"],"
                                + "\"permisos\":[\"DOCENTES:LECTURA\","
                                + "\"MI_DISTRIBUTIVO:LECTURA:PROPIO\"],\"carrera\":\"DS-01\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaLaAutorizacion_entonces403Comun() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/interno/roles/autorizaciones/docente.demo")
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_ADMINISTRADOR"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("SEGURIDAD_ACCESO_NO_AUTORIZADO");
        contrato.validar(resultado);
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarFirmantes consultarFirmantes() {
            return periodo -> new Firmantes(List.of("RECTOR", "VICERRECTOR"), true);
        }

        @Bean
        ec.mdd.roles.puertos.entrada.ConsultarResponsable consultarResponsable() {
            return periodo -> {
                if ("2025-II".equals(periodo)) {
                    throw new ec.mdd.roles.puertos.entrada.SinResponsable();
                }
                return new ec.mdd.roles.puertos.entrada.ResponsableDelPeriodo(
                        "consolidacion.demo", "PÉREZ ANA");
            };
        }

        @Bean
        ConsultarAutorizacion consultarAutorizacion() {
            return usuario ->
                    new Autorizacion(
                            usuario,
                            List.of("COORDINADOR_CARRERA", "DOCENTE"),
                            List.of("DOCENTES:LECTURA", "MI_DISTRIBUTIVO:LECTURA:PROPIO"),
                            "DS-01");
        }
    }
}
