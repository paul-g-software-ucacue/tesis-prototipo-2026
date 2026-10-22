package ec.mdd.roles.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.entrada.ConsultarResponsables;
import ec.mdd.roles.puertos.entrada.DesignarResponsable;
import ec.mdd.roles.puertos.entrada.PeriodoDesconocido;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las operaciones del Responsable de consolidación cumplen {@code contratos/roles/openapi.yaml}
 * (F05-H03, principio IV), con un doble de los casos de uso escrito a mano que usa las reglas
 * reales del dominio.
 */
@WebMvcTest(ResponsablesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ResponsablesContratoTest.Dobles.class, ErroresRoles.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ResponsablesContratoTest {

    static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("roles");

    @Autowired private MockMvc mvc;

    @Autowired private ResponsablesEnMemoria responsables;

    @BeforeEach
    void reiniciar() {
        responsables.reiniciar();
    }

    @Test
    void dadoUnPeriodoAbierto_cuandoSeDesignaUnDocente_entonces200ConElResponsable()
            throws Exception {
        MvcResult resultado = designar("2026-2", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(
                        JsonPath.<Map<String, Object>>read(
                                resultado.getResponse().getContentAsString(), "$"))
                .containsEntry("periodo", "2026-2")
                .containsEntry("usuario", "docente.demo")
                .containsEntry("designadoEn", "2026-10-21T15:00:00Z");
        assertThat(responsables.ultimoQuien()).isEqualTo("vicerrectorado.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnUsuarioSinElRolDocente_cuandoSeDesigna_entonces400ConElCampoUsuario()
            throws Exception {
        MvcResult resultado = designar("2026-2", "rectorado.demo");

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].campo")).isEqualTo("usuario");
        assertThat(JsonPath.<String>read(cuerpo, "$.detalles[0].descripcion"))
                .isEqualTo("Solo se puede designar a un usuario con el rol Docente");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueRolesNoConoce_cuandoSeDesigna_entonces404Comun() throws Exception {
        MvcResult resultado = designar("2030-1", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeDesigna_entonces409PeriodoCerrado() throws Exception {
        MvcResult resultado = designar("2026-1", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.codigo"))
                .isEqualTo("ROLES_PERIODO_CERRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaDesignacion_cuandoSeListanLosResponsables_entoncesAparece() throws Exception {
        designar("2026-2", "docente.demo");

        MvcResult resultado = pedir(get("/api/roles/responsables"), lectura());

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"periodo\":\"2026-2\",\"usuario\":\"docente.demo\","
                                + "\"designadoEn\":\"2026-10-21T15:00:00Z\"}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoLosUsuarios_cuandoSePidenLosDocentes_entoncesSusNombres() throws Exception {
        MvcResult resultado = pedir(get("/api/roles/docentes"), escritura());

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("[\"docente.coordinador.demo\",\"docente.demo\"]");
        contrato.validar(resultado);
    }

    @Test
    void dadoElAdministradorSinDistributivo_cuandoDesigna_entonces403SinDesignar()
            throws Exception {
        MvcResult resultado =
                pedir(
                        put("/api/roles/responsables/2026-2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"usuario\":\"docente.demo\"}"),
                        jwt().jwt(token -> token.subject("admin.demo"))
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "PERMISO_USUARIOS_ROLES:LECTURA"),
                                        new SimpleGrantedAuthority(
                                                "PERMISO_USUARIOS_ROLES:ESCRITURA")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
        assertThat(responsables.responsables()).isEmpty();
    }

    private MvcResult designar(String periodo, String usuario) throws Exception {
        return pedir(
                put("/api/roles/responsables/" + periodo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\"}"),
                escritura());
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:ESCRITURA"));
    }

    static JwtRequestPostProcessor lectura() {
        return jwt().jwt(token -> token.subject("rectorado.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    /**
     * Doble escrito a mano: un periodo 2026-2 en Planificación y un 2026-1 cerrado; docente.demo y
     * docente.coordinador.demo tienen el rol Docente.
     */
    static final class ResponsablesEnMemoria implements DesignarResponsable, ConsultarResponsables {

        private static final Map<String, List<String>> ROLES =
                Map.of(
                        "docente.demo", List.of("DOCENTE"),
                        "docente.coordinador.demo", List.of("COORDINADOR_CARRERA", "DOCENTE"),
                        "rectorado.demo", List.of("RECTOR"));
        private static final Map<String, PeriodoConocido> PERIODOS =
                Map.of(
                        "2026-2", new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION),
                        "2026-1", new PeriodoConocido("2026-1", EstadoConocido.CERRADO));

        private final Map<String, Designacion> designaciones = new TreeMap<>();
        private String ultimoQuien;

        void reiniciar() {
            designaciones.clear();
            ultimoQuien = null;
        }

        String ultimoQuien() {
            return ultimoQuien;
        }

        @Override
        public Designacion designar(String periodo, String usuario, String quien) {
            PeriodoConocido conocido = PERIODOS.get(periodo);
            if (conocido == null) {
                throw new PeriodoDesconocido(periodo);
            }
            Designacion nueva =
                    Designacion.de(
                            conocido,
                            usuario,
                            quien,
                            ROLES.getOrDefault(usuario, List.of()),
                            Optional.ofNullable(designaciones.get(periodo)),
                            AHORA);
            designaciones.put(periodo, nueva);
            ultimoQuien = quien;
            return nueva;
        }

        @Override
        public List<Designacion> responsables() {
            return new ArrayList<>(designaciones.values());
        }

        @Override
        public List<String> docentes() {
            return List.of("docente.coordinador.demo", "docente.demo");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ResponsablesEnMemoria responsablesEnMemoria() {
            return new ResponsablesEnMemoria();
        }
    }
}
