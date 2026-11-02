package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.generacion.dominio.AvanceDelParcial;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.puertos.entrada.ConsultarDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarParciales;
import ec.mdd.generacion.puertos.entrada.CrearDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.HorasExcedidas;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParaleloNoEncontrado;
import ec.mdd.generacion.puertos.entrada.ParaleloRetirado;
import ec.mdd.generacion.puertos.entrada.ParcialConAvance;
import ec.mdd.generacion.puertos.entrada.ParcialesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.entrada.ProponerDocente;
import ec.mdd.generacion.puertos.entrada.PropuestaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDePropuesta;
import ec.mdd.generacion.puertos.entrada.TopeDeClase;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
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

/** Crear y consultar el distributivo cumple su contrato (RF-56, principio IV). */
@WebMvcTest(DistributivoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    DistributivoContratoTest.Dobles.class,
    ErroresGeneracion.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class DistributivoContratoTest {

    private static final String RUTA = "/api/generacion/periodos/%s/distributivo";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnPeriodoListo_cuandoCrea_entonces201ConSusCarreras() throws Exception {
        MvcResult resultado = crear("2026-I");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"version\":1,"
                                + "\"carreras\":[\"AGRO\",\"TDS\"]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoSinPreparar_cuandoCrea_entonces409QueLoMandaAlTablero() throws Exception {
        MvcResult resultado = crear("NO-LISTO");

        assertError(resultado, 409, "GENERACION_PERIODO_NO_LISTO");
        assertThat(resultado.getResponse().getContentAsString())
                .contains("Preparación del periodo");
    }

    @Test
    void dadoUnPeriodoConDistributivo_cuandoCrea_entonces409() throws Exception {
        assertError(crear("REPETIDO"), 409, "GENERACION_DISTRIBUTIVO_REPETIDO");
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoCrea_entonces404() throws Exception {
        assertError(crear("NADA"), 404, "GENERACION_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void dadoOtroProcesoConError_cuandoCrea_entonces502SinDetallesTecnicos() throws Exception {
        MvcResult resultado = crear("FALLA");

        assertError(resultado, 502, "COMUN_PROCESO_CON_ERROR");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("motivo-interno");
    }

    @Test
    void dadoOtroProcesoSinRespuesta_cuandoCrea_entonces504() throws Exception {
        assertError(crear("LENTO"), 504, "COMUN_PROCESO_SIN_RESPUESTA");
    }

    @Test
    void dadoQuienSoloLee_cuandoCrea_entonces403SinCrear() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                post(RUTA.formatted("FALLA"))
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "PERMISO_DISTRIBUTIVO:LECTURA"))))
                        .andReturn();

        assertError(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoQuienNoTieneToken_cuandoCrea_entonces401() throws Exception {
        MvcResult resultado = mvc.perform(post(RUTA.formatted("2026-I"))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({"2026 I", "periodo-demasiado-largo-para-el-campo"})
    void dadoUnPeriodoMalEscrito_cuandoCrea_entonces400(String periodo) throws Exception {
        assertThat(crear(periodo).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnDistributivo_cuandoConsulta_entonces200() throws Exception {
        MvcResult resultado = consultar("2026-I");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(resultado);
    }

    @Test
    void dadoQueNoTieneDistributivo_cuandoConsulta_entonces404() throws Exception {
        assertError(consultar("SIN"), 404, "GENERACION_DISTRIBUTIVO_NO_ENCONTRADO");
    }

    private MvcResult crear(String periodo) throws Exception {
        return mvc.perform(
                        post(RUTA.formatted(periodo))
                                .with(
                                        jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_DISTRIBUTIVO:ESCRITURA"))))
                .andReturn();
    }

    private MvcResult consultar(String periodo) throws Exception {
        return mvc.perform(
                        get(RUTA.formatted(periodo))
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_CARGAS:LECTURA"))))
                .andReturn();
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    /** Los casos de uso del distributivo: el periodo de la ruta elige la respuesta. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        private static final String TEXTO = "Tiene experiencia en la materia";

        private static void fallar(String periodo) {
            switch (periodo) {
                case "NO-LISTO" -> throw new PeriodoNoListo();
                case "REPETIDO" -> throw new DistributivoRepetido();
                case "SIN" -> throw new SinDistributivo();
                case "CERRADO" -> throw new PeriodoCerrado();
                case "RETIRADO" -> throw new ParaleloRetirado();
                case "NO-ACTIVO" -> throw new DocenteNoActivo();
                case "EXCEDIDAS" -> throw new HorasExcedidas(new BigDecimal("1.5"));
                case "TOPE" -> throw new TopeDeClase(new BigDecimal("2"));
                case "ENVIADO" -> throw new ParcialNoEditable();
                case "NADA" -> throw new PeriodoNoEncontrado();
                case "PARALELO" -> throw new ParaleloNoEncontrado();
                case "FALLA" -> throw new ProcesoConError("motivo-interno");
                case "LENTO" -> throw new ProcesoSinRespuesta();
                default -> {
                    // Los demás responden.
                }
            }
        }

        private static void exigirCarrera(Optional<String> carrera) {
            if (carrera.isPresent() && !"TDS".equals(carrera.get())) {
                throw new OtraCarrera();
            }
        }

        private static PropuestaVista vista(String docente, String texto) {
            return new PropuestaVista(
                    new Propuesta(
                            "POO-204",
                            "A",
                            "TDS",
                            docente,
                            "PEREZ LUIS",
                            texto == null
                                    ? new Justificacion(TipoDeJustificacion.AFINIDAD, null)
                                    : new Justificacion(TipoDeJustificacion.EXCEPCION, texto),
                            Instant.parse("2026-10-31T15:00:00Z"),
                            "coordinacion.demo"),
                    false,
                    true);
        }

        @Bean
        CrearDistributivo crearDistributivo() {
            return (periodo, quien) -> {
                fallar(periodo);
                return new DistributivoDelPeriodo(periodo, 1, List.of("AGRO", "TDS"));
            };
        }

        @Bean
        ConsultarDistributivo consultarDistributivo() {
            return periodo -> {
                if ("SIN".equals(periodo)) {
                    throw new DistributivoNoEncontrado();
                }
                return new DistributivoDelPeriodo(periodo, 1, List.of("TDS"));
            };
        }

        @Bean
        ProponerDocente proponerDocente() {
            return new ProponerDocente() {
                @Override
                public PropuestaVista proponer(SolicitudDePropuesta solicitud) {
                    exigirCarrera(solicitud.carrera());
                    fallar(solicitud.periodo());
                    if ("OBLIGADA".equals(solicitud.periodo()) && solicitud.texto() == null) {
                        throw new JustificacionObligatoria();
                    }
                    return vista(solicitud.docente(), solicitud.texto());
                }

                @Override
                public void quitar(
                        String periodo,
                        String asignatura,
                        String paralelo,
                        Optional<String> carrera,
                        String quien) {
                    exigirCarrera(carrera);
                    fallar(periodo);
                    if ("SIN-PROPUESTA".equals(periodo)) {
                        throw new PropuestaNoEncontrada();
                    }
                }
            };
        }

        @Bean
        ConsultarParciales consultarParciales() {
            return (periodo, carrera) -> {
                fallar(periodo);
                if ("SIN-CREAR".equals(periodo)) {
                    return new ParcialesDelPeriodo(false, List.of());
                }
                ParcialConAvance tds =
                        new ParcialConAvance(
                                ParcialCarrera.nuevo("TDS"),
                                new AvanceDelParcial(2, 1),
                                List.of(vista("0109990010", TEXTO)));
                ParcialConAvance agro =
                        new ParcialConAvance(
                                new ParcialCarrera("AGRO", EstadoDelParcial.EN_ELABORACION),
                                new AvanceDelParcial(1, 1),
                                List.of());
                return new ParcialesDelPeriodo(
                        true,
                        List.of(agro, tds).stream()
                                .filter(
                                        uno ->
                                                carrera.isEmpty()
                                                        || carrera.get()
                                                                .equals(uno.parcial().carrera()))
                                .toList());
            };
        }
    }
}
