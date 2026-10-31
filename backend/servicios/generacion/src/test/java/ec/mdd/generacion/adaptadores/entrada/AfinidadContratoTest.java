package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.generacion.dominio.AfinidadDelDocente;
import ec.mdd.generacion.dominio.AporteDeAfinidad;
import ec.mdd.generacion.dominio.CoincidenciaDeCampo;
import ec.mdd.generacion.dominio.PuntajeAfinidad;
import ec.mdd.generacion.dominio.TipoDeAporte;
import ec.mdd.generacion.puertos.entrada.ConsultarAfinidad;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * La afinidad del docente cumple su contrato (RF-17, principio IV), con un doble escrito a mano.
 */
@WebMvcTest(AfinidadControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({AfinidadContratoTest.Dobles.class, ErroresGeneracion.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class AfinidadContratoTest {

    private static final String RUTA = "/api/generacion/docentes/%s/afinidad";
    private static final String DOCENTE = "0109990010";
    private static final AtomicReference<Optional<String>> CARRERA = new AtomicReference<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnCoordinador_cuandoConsulta_entonces200ConElPuntajeYSuDesglose() throws Exception {
        MvcResult resultado = pedir(DOCENTE, "2026-I");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"version\":2,\"puntajes\":[{"
                                + "\"asignatura\":\"POO-204\",\"nombre\":\"Programación\","
                                + "\"total\":7,\"topeAplicado\":true,\"desglose\":[{"
                                + "\"tipo\":\"CUARTO_NIVEL\",\"nombre\":\"Maestría\","
                                + "\"coincidencia\":\"CAMPO_ESPECIFICO\",\"aporte\":7}]}]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoConsulta_entonces404DelCatalogo() throws Exception {
        assertError(pedir(DOCENTE, "NADA"), 404, "GENERACION_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoConsulta_entonces404DelCatalogo() throws Exception {
        assertError(pedir("0000000000", "2026-I"), 404, "GENERACION_DOCENTE_NO_ENCONTRADO");
    }

    @Test
    void dadoUnPeriodoSinAbrir_cuandoConsulta_entonces409QueDiceQueLoAbras() throws Exception {
        MvcResult resultado = pedir(DOCENTE, "2026-II");

        assertError(resultado, 409, "GENERACION_PERIODO_SIN_ABRIR");
        assertThat(resultado.getResponse().getContentAsString()).contains("Ábrelo");
    }

    @Test
    void dadoOtroProcesoConError_cuandoConsulta_entonces502SinDetallesTecnicos() throws Exception {
        MvcResult resultado = pedir(DOCENTE, "FALLA");

        assertError(resultado, 502, "COMUN_PROCESO_CON_ERROR");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("motivo-interno");
    }

    @Test
    void dadoOtroProcesoSinRespuesta_cuandoConsulta_entonces504() throws Exception {
        assertError(pedir(DOCENTE, "LENTO"), 504, "COMUN_PROCESO_SIN_RESPUESTA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026 I", "2026/I", "periodo-demasiado-largo-para-el-campo"})
    void dadoUnPeriodoMalEscrito_cuandoConsulta_entonces400(String periodo) throws Exception {
        assertThat(pedir(DOCENTE, periodo).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoQueFaltaElPeriodo_cuandoConsulta_entonces400() throws Exception {
        MvcResult resultado =
                mvc.perform(get(RUTA.formatted(DOCENTE)).with(coordinador())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "01099900101234"})
    void dadaUnaIdentificacionMalEscrita_cuandoConsulta_entonces400(String identificacion)
            throws Exception {
        assertThat(pedir(identificacion, "2026-I").getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoQuienNoPuedeLeerDocentes_cuandoConsulta_entonces403SinCalcular() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get(RUTA.formatted(DOCENTE))
                                        .param("periodo", "FALLA")
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "PERMISO_OFERTA:LECTURA"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinadorConCarrera_cuandoConsulta_entoncesSoloPideSuCarrera() throws Exception {
        pedirCon(conPermisos("DS-01", "OFERTA:LECTURA:PROPIO", "DOCENTES:LECTURA"));

        assertThat(CARRERA.get()).contains("DS-01");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoConsulta_entonces403SinCalcular() throws Exception {
        CARRERA.set(Optional.of("sin consultar"));

        MvcResult resultado =
                pedirCon(conPermisos(null, "DISTRIBUTIVO:LECTURA:PROPIO", "DOCENTES:LECTURA"));

        assertError(resultado, 403, "COMUN_CARRERA_SIN_INDICAR");
        assertThat(CARRERA.get()).contains("sin consultar");
    }

    @Test
    void dadoQuienTieneLaOfertaSobreTodo_cuandoConsulta_entoncesVeTodas() throws Exception {
        pedirCon(conPermisos(null, "DISTRIBUTIVO:LECTURA"));

        assertThat(CARRERA.get()).isEmpty();
    }

    @Test
    void dadoQuienNoTienePermisosDeLaOferta_cuandoConsulta_entoncesVeTodas() throws Exception {
        pedirCon(conPermisos(null, "DOCENTES:LECTURA"));

        assertThat(CARRERA.get()).isEmpty();
    }

    private MvcResult pedirCon(
            org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
                    token)
            throws Exception {
        return mvc.perform(get(RUTA.formatted(DOCENTE)).param("periodo", "2026-I").with(token))
                .andReturn();
    }

    private static org.springframework.security.test.web.servlet.request
                    .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
            conPermisos(String carrera, String... permisos) {
        return jwt().jwt(
                        token -> {
                            token.claim("permisos", List.of(permisos));
                            if (carrera != null) {
                                token.claim("carrera", carrera);
                            }
                        })
                .authorities(new AutoridadesDelToken());
    }

    @Test
    void dadoQuienNoTieneToken_cuandoConsulta_entonces401() throws Exception {
        MvcResult resultado =
                mvc.perform(get(RUTA.formatted(DOCENTE)).param("periodo", "2026-I")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    private MvcResult pedir(String identificacion, String periodo) throws Exception {
        return mvc.perform(
                        get(RUTA.formatted(identificacion))
                                .param("periodo", periodo)
                                .with(coordinador()))
                .andReturn();
    }

    private static org.springframework.security.test.web.servlet.request
                    .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
            coordinador() {
        return jwt().authorities(new SimpleGrantedAuthority("PERMISO_DOCENTES:LECTURA"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarAfinidad consultarAfinidad() {
            return (identificacion, periodo, carrera) -> {
                CARRERA.set(carrera);
                switch (periodo) {
                    case "NADA" -> throw new PeriodoNoEncontrado();
                    case "2026-II" -> throw new PeriodoSinAbrir();
                    case "FALLA" -> throw new ProcesoConError("motivo-interno");
                    case "LENTO" -> throw new ProcesoSinRespuesta();
                    default -> {
                        if ("0000000000".equals(identificacion)) {
                            throw new DocenteNoEncontrado();
                        }
                    }
                }
                return new AfinidadDelDocente(
                        periodo,
                        2,
                        List.of(
                                new PuntajeAfinidad(
                                        "POO-204",
                                        "Programación",
                                        new BigDecimal("7"),
                                        true,
                                        List.of(
                                                new AporteDeAfinidad(
                                                        TipoDeAporte.CUARTO_NIVEL,
                                                        "Maestría",
                                                        CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                                                        new BigDecimal("7"))))));
            };
        }
    }
}
