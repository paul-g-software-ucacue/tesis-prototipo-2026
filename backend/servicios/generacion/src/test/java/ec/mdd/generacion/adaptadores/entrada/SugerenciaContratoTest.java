package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.generacion.dominio.AporteDeAfinidad;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Candidato;
import ec.mdd.generacion.dominio.CoincidenciaDeCampo;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.HorasDelDocente;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.PuntajeAfinidad;
import ec.mdd.generacion.dominio.Sugerencia;
import ec.mdd.generacion.dominio.TipoDeAporte;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SugerenciaDelParalelo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

/** La sugerencia cumple su contrato (RF-25, principio IV), con un doble escrito a mano. */
@WebMvcTest(SugerenciaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({SugerenciaContratoTest.Dobles.class, ErroresGeneracion.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class SugerenciaContratoTest {

    private static final String RUTA = "/api/generacion/periodos/%s/oferta/%s/%s/sugerencia";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnCoordinador_cuandoConsulta_entonces200ConLosCandidatos() throws Exception {
        MvcResult resultado = pedir("2026-I", "POO-204", "A_NOCTURNA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"asignatura\":\"POO-204\","
                                + "\"nombre\":\"Programación\",\"paralelo\":\"A_NOCTURNA\","
                                + "\"horasDelParalelo\":2.5,\"troncoComun\":false,\"version\":2,"
                                + "\"minimoAfinidad\":3.5,\"fueraPorElMinimo\":1,\"candidatos\":[{"
                                + "\"identificacion\":\"0109990010\",\"nombre\":\"PEREZ LUIS\","
                                + "\"dedicacion\":\"TC\",\"horasContratadas\":40,"
                                + "\"horasDisponibles\":30.5,\"horasEnOtrasCarreras\":6,"
                                + "\"horasSuficientes\":true,\"puntaje\":{"
                                + "\"asignatura\":\"POO-204\",\"nombre\":\"Programación\","
                                + "\"total\":7,\"topeAplicado\":false,\"desglose\":[{"
                                + "\"tipo\":\"CUARTO_NIVEL\",\"nombre\":\"Maestría\","
                                + "\"coincidencia\":\"CAMPO_ESPECIFICO\",\"aporte\":7}]}}],"
                                + "\"bajoElMinimo\":[{\"identificacion\":\"0109990020\","
                                + "\"nombre\":\"LOPEZ ANA\",\"dedicacion\":\"TC\","
                                + "\"horasContratadas\":40,\"horasDisponibles\":40,"
                                + "\"horasEnOtrasCarreras\":0,\"horasSuficientes\":true,"
                                + "\"puntaje\":{\"asignatura\":\"POO-204\","
                                + "\"nombre\":\"Programación\",\"total\":0,"
                                + "\"topeAplicado\":false,\"desglose\":[]}}],"
                                + "\"distributivoCreado\":false}")
                .doesNotContain("correo", "telefono");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaPropuesta_cuandoConsulta_entoncesLaTraeConSuJustificacion() throws Exception {
        MvcResult resultado = pedir("CON-PROPUESTA", "POO-204", "A");

        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "\"distributivoCreado\":true,\"propuesta\":{"
                                + "\"asignatura\":\"POO-204\",\"paralelo\":\"A\","
                                + "\"docente\":\"0109990010\",\"nombreDocente\":\"PEREZ LUIS\","
                                + "\"justificacion\":\"EXCEPCION\",\"retirada\":false,"
                                + "\"horasSuficientes\":false,"
                                + "\"texto\":\"Tiene experiencia en la materia\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoQueNoHayCandidatos_cuandoConsulta_entonces200ConLaListaVacia() throws Exception {
        MvcResult resultado = pedir("VACIO", "POO-204", "A");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"candidatos\":[]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnParaleloQueNoExiste_cuandoConsulta_entonces404DelCatalogo() throws Exception {
        assertError(pedir("NADA", "POO-204", "A"), 404, "GENERACION_PARALELO_NO_ENCONTRADO");
    }

    @Test
    void dadoUnParaleloRetirado_cuandoConsulta_entonces409DelCatalogo() throws Exception {
        assertError(pedir("RETIRADO", "POO-204", "A"), 409, "GENERACION_PARALELO_RETIRADO");
    }

    @Test
    void dadoUnPeriodoSinAbrir_cuandoConsulta_entonces409QueDiceQueLoAbras() throws Exception {
        MvcResult resultado = pedir("2026-II", "POO-204", "A");

        assertError(resultado, 409, "GENERACION_PERIODO_SIN_ABRIR");
        assertThat(resultado.getResponse().getContentAsString()).contains("Ábrelo");
    }

    @Test
    void dadoOtroProcesoConError_cuandoConsulta_entonces502SinDetallesTecnicos() throws Exception {
        MvcResult resultado = pedir("FALLA", "POO-204", "A");

        assertError(resultado, 502, "COMUN_PROCESO_CON_ERROR");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("motivo-interno");
    }

    @Test
    void dadoOtroProcesoSinRespuesta_cuandoConsulta_entonces504() throws Exception {
        assertError(pedir("LENTO", "POO-204", "A"), 504, "COMUN_PROCESO_SIN_RESPUESTA");
    }

    @ParameterizedTest
    @CsvSource({
        "2026 I,POO-204,A",
        "periodo-demasiado-largo-para-el-campo,POO-204,A",
        "2026-I,poo-204,A",
        "2026-I,CODIGO-DEMASIADO-LARGO-PARA-EL-CAMPO,A",
        "2026-I,POO-204,a",
        "2026-I,POO-204,A B",
        "2026-I,POO-204,PARALELO-DEMASIADO-LARGO"
    })
    void dadoUnCodigoMalEscrito_cuandoConsulta_entonces400(
            String periodo, String asignatura, String paralelo) throws Exception {
        assertThat(pedir(periodo, asignatura, paralelo).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadosCodigosEnSuLimite_cuandoConsulta_entoncesLosAcepta() throws Exception {
        MvcResult resultado =
                pedir("2026.I_a-b", "ABCDE-12345-ABCDE-12345-ABCD-1", "A_NOCTURNA-123456789");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoQuienNoPuedeLeerDocentes_cuandoConsulta_entonces403SinCalcular() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get(RUTA.formatted("FALLA", "POO-204", "A"))
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "PERMISO_OFERTA:LECTURA"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinadorDeSuCarrera_cuandoConsulta_entonces200() throws Exception {
        assertThat(
                        pedirCon("TDS", "OFERTA:LECTURA:PROPIO", "DOCENTES:LECTURA")
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoUnCoordinadorDeOtraCarrera_cuandoConsulta_entonces403SinDatos() throws Exception {
        MvcResult resultado = pedirCon("OTRA-01", "OFERTA:LECTURA:PROPIO", "DOCENTES:LECTURA");

        assertError(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("POO-204");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoConsulta_entonces403QueDiceQuePidaSuCarrera()
            throws Exception {
        assertError(
                pedirCon(null, "OFERTA:LECTURA:PROPIO", "DOCENTES:LECTURA"),
                403,
                "COMUN_CARRERA_SIN_INDICAR");
    }

    @Test
    void dadoQuienTieneLaOfertaSobreTodo_cuandoConsulta_entonces200SinCarrera() throws Exception {
        assertThat(pedirCon(null, "DISTRIBUTIVO:LECTURA").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoUnRolPropioYOtroSobreTodo_cuandoConsulta_entoncesValeElMasAmplio() throws Exception {
        assertThat(
                        pedirCon(null, "OFERTA:LECTURA:PROPIO", "CATALOGOS:LECTURA")
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    private MvcResult pedirCon(String carrera, String... permisos) throws Exception {
        return mvc.perform(
                        get(RUTA.formatted("2026-I", "POO-204", "A"))
                                .with(
                                        jwt().jwt(
                                                        token -> {
                                                            token.claim(
                                                                    "permisos", List.of(permisos));
                                                            if (carrera != null) {
                                                                token.claim("carrera", carrera);
                                                            }
                                                        })
                                                .authorities(new AutoridadesDelToken())))
                .andReturn();
    }

    @Test
    void dadoQuienNoTieneToken_cuandoConsulta_entonces401() throws Exception {
        MvcResult resultado =
                mvc.perform(get(RUTA.formatted("2026-I", "POO-204", "A"))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    private MvcResult pedir(String periodo, String asignatura, String paralelo) throws Exception {
        return mvc.perform(
                        get(RUTA.formatted(periodo, asignatura, paralelo))
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_DOCENTES:LECTURA"))))
                .andReturn();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        private static final PuntajeAfinidad PUNTAJE =
                new PuntajeAfinidad(
                        "POO-204",
                        "Programación",
                        new BigDecimal("7"),
                        false,
                        List.of(
                                new AporteDeAfinidad(
                                        TipoDeAporte.CUARTO_NIVEL,
                                        "Maestría",
                                        CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                                        new BigDecimal("7"))));

        private static final Candidato BAJO_EL_MINIMO =
                new Candidato(
                        "0109990020",
                        "LOPEZ ANA",
                        Dedicacion.TC,
                        40,
                        HorasDelDocente.NINGUNA,
                        new PuntajeAfinidad(
                                "POO-204", "Programación", BigDecimal.ZERO, false, List.of()));

        @Bean
        ConsultarSugerencia consultarSugerencia() {
            return (periodo, asignatura, paralelo, carrera) -> {
                if (carrera.isPresent() && !"TDS".equals(carrera.get())) {
                    throw new OtraCarrera();
                }
                switch (periodo) {
                    case "NADA" -> throw new ParaleloNoEncontrado();
                    case "RETIRADO" -> throw new ParaleloRetirado();
                    case "2026-II" -> throw new PeriodoSinAbrir();
                    case "FALLA" -> throw new ProcesoConError("motivo-interno");
                    case "LENTO" -> throw new ProcesoSinRespuesta();
                    default -> {
                        // Los demás responden la sugerencia.
                    }
                }
                List<Candidato> candidatos =
                        "VACIO".equals(periodo)
                                ? List.of()
                                : List.of(
                                        new Candidato(
                                                "0109990010",
                                                "PEREZ LUIS",
                                                Dedicacion.TC,
                                                40,
                                                new HorasDelDocente(
                                                        new BigDecimal("9.5"), new BigDecimal("6")),
                                                PUNTAJE));
                Sugerencia sugerencia =
                        new Sugerencia(
                                periodo,
                                new AsignaturaParalelo(
                                        new AsignaturaOfertada(
                                                "POO-204", "Programación", "06", "061"),
                                        paralelo,
                                        "TDS",
                                        new BigDecimal("2.5"),
                                        false,
                                        false),
                                2,
                                new BigDecimal("3.5"),
                                candidatos,
                                "VACIO".equals(periodo) ? List.of() : List.of(BAJO_EL_MINIMO));
                boolean conPropuesta = "CON-PROPUESTA".equals(periodo);
                return new SugerenciaDelParalelo(
                        sugerencia,
                        conPropuesta,
                        conPropuesta
                                ? Optional.of(
                                        new PropuestaVista(
                                                new Propuesta(
                                                        "POO-204",
                                                        "A",
                                                        "TDS",
                                                        "0109990010",
                                                        "PEREZ LUIS",
                                                        new Justificacion(
                                                                TipoDeJustificacion.EXCEPCION,
                                                                "Tiene experiencia en la materia"),
                                                        Instant.parse("2026-10-31T15:00:00Z"),
                                                        "coordinacion.demo"),
                                                false,
                                                false))
                                : Optional.empty());
            };
        }
    }
}
