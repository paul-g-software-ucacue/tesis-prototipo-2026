package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentesActivos;
import ec.mdd.docentes.puertos.entrada.ConsultarFormacion;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectosYCargos;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.ProyectosYCargos;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void dadoUnProceso_cuandoConsultaProyectosYCargos_entonces200SoloConLoNecesario()
            throws Exception {
        MvcResult resultado = pedirProyectosYCargos(DocentesDePrueba.CEDULA, "2026-I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"proyectos\":[{\"codigo\":\"INV-2025-01\",\"nombre\":\"Monitoreo\","
                                + "\"tipo\":\"INVESTIGACION\"}],\"cargos\":[\"RECTOR\"]}")
                .doesNotContain("correo", "telefono", "horas", "resolucion");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoConsultaProyectosYCargos_entonces404() throws Exception {
        MvcResult resultado = pedirProyectosYCargos("0109990099", "2026-I", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoConsultaProyectosYCargos_entonces404() throws Exception {
        MvcResult resultado = pedirProyectosYCargos(DocentesDePrueba.CEDULA, "NADA", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoConsultaProyectosYCargos_entonces403() throws Exception {
        MvcResult resultado = pedirProyectosYCargos(DocentesDePrueba.CEDULA, "2026-I", jwt());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    private MvcResult pedirProyectosYCargos(
            String docente, String periodo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(
                        get("/interno/docentes/%s/proyectos-y-cargos".formatted(docente))
                                .param("periodo", periodo)
                                .with(token))
                .andReturn();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarProyectosYCargos consultarProyectosYCargos() {
            return (identificacion, periodo) -> {
                if (!DocentesDePrueba.CEDULA.equals(identificacion)) {
                    throw new DocenteNoEncontrado();
                }
                if ("NADA".equals(periodo)) {
                    throw new DatoInvalido("periodo", "El periodo no existe");
                }
                return new ProyectosYCargos(
                        List.of(
                                new Proyecto(
                                        "INV-2025-01",
                                        "Monitoreo",
                                        TipoDeProyecto.INVESTIGACION,
                                        List.of("550613A01-P-0301"),
                                        "RES-OCS-1",
                                        LocalDate.of(2025, 3, 20),
                                        EstadoDeProyecto.EN_EJECUCION,
                                        LocalDate.of(2025, 4, 1),
                                        LocalDate.of(2026, 12, 31),
                                        DocentesDePrueba.CEDULA,
                                        List.of(
                                                new Participante(
                                                        DocentesDePrueba.CEDULA,
                                                        RolDeParticipante.DIRECTOR,
                                                        new BigDecimal("10"))),
                                        new BigDecimal("20"))),
                        List.of(TipoDeCargo.RECTOR));
            };
        }

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
