package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.MetaDeProporcion;
import ec.mdd.generacion.puertos.entrada.CargaDelDocente;
import ec.mdd.generacion.puertos.entrada.ConsultarCargaHoraria;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** La carga horaria cumple su contrato: solo sumas, sin filtro de carrera (RF-27, D-82). */
@WebMvcTest(CargaHorariaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    CargaHorariaContratoTest.Dobles.class,
    ErroresGeneracion.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class CargaHorariaContratoTest {

    private static final String RUTA =
            "/api/generacion/periodos/{periodo}/docentes/{docente}/carga-horaria";
    private static final String LEE = "OFERTA:LECTURA:PROPIO";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnCoordinador_cuandoConsulta_entonces200ConLosTotalesYLaProporcion() throws Exception {
        MvcResult resultado = pedir("2026-I", "0109990010", "AGRO", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"horasContratadas\":20")
                .contains("\"clase\":8")
                .contains("\"otrasDocencia\":4")
                .contains("\"asignadas\":33")
                .contains("\"disponibles\":0")
                .contains("\"exceso\":13")
                .contains("\"proporcion\":{\"clase\":67,\"otrasDocencia\":33}")
                .contains("\"meta\":{\"clase\":60,\"otrasDocencia\":40}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocenteDeOtraCarrera_cuandoConsulta_entoncesSoloLlevanSumas() throws Exception {
        MvcResult resultado = pedir("2026-I", "0109990010", "AGRO", LEE);

        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain("asignatura", "actividades", "paralelo", "correo", "telefono");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoConsulta_entonces200PorqueNoSeFiltra() throws Exception {
        assertThat(pedir("2026-I", "0109990010", null, LEE).getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void sinHorasDeDocencia_cuandoConsulta_entoncesNoTraeProporcion() throws Exception {
        MvcResult resultado = pedir("SIN-HORAS", "0109990010", "TDS", LEE);

        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("proporcion");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "DISTRIBUTIVO:LECTURA, 200",
        "CATALOGOS:LECTURA, 200",
        "DOCENTES:LECTURA, 403",
        "MI_DISTRIBUTIVO:LECTURA:PROPIO, 403",
    })
    void dadoUnPermiso_cuandoConsulta_entoncesResponde(String permiso, int estado)
            throws Exception {
        MvcResult resultado = pedir("2026-I", "0109990010", null, permiso);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "NO-ACTIVO, 409, GENERACION_DOCENTE_NO_ACTIVO",
        "SIN-DISTRIBUTIVO, 409, GENERACION_SIN_DISTRIBUTIVO",
        "SIN-ABRIR, 409, GENERACION_PERIODO_SIN_ABRIR",
    })
    void dadoUnFallo_cuandoConsulta_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        MvcResult resultado = pedir(periodo, "0109990010", "TDS", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({"2026-I, 01099 90010", "2026 I, 0109990010", "2026-I, abc"})
    void dadosDatosMalEscritos_cuandoConsulta_entonces400(String periodo, String docente)
            throws Exception {
        assertThat(pedir(periodo, docente, "TDS", LEE).getResponse().getStatus()).isEqualTo(400);
    }

    private MvcResult pedir(String periodo, String docente, String carrera, String permiso)
            throws Exception {
        return mvc.perform(get(RUTA, periodo, docente).with(token(carrera, permiso))).andReturn();
    }

    private static RequestPostProcessor token(String carrera, String permiso) {
        return jwt().jwt(
                        token -> {
                            token.subject("coordinacion.demo");
                            token.claim("permisos", List.of(permiso));
                            if (carrera != null) {
                                token.claim("carrera", carrera);
                            }
                        })
                .authorities(new AutoridadesDelToken());
    }

    /** El caso de uso: el periodo de la ruta elige la respuesta. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        private static CargaDelDocente carga(String clase, String otras, String gestion) {
            return new CargaDelDocente(
                    "0109990010",
                    "PEREZ LUIS",
                    20,
                    new CargaHoraria(
                            new BigDecimal(clase),
                            new BigDecimal(otras),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            new BigDecimal(gestion)),
                    new MetaDeProporcion(new BigDecimal("60"), new BigDecimal("40")));
        }

        @Bean
        ConsultarCargaHoraria consultarCargaHoraria() {
            return (periodo, docente) ->
                    switch (periodo) {
                        case "NO-ACTIVO" -> throw new DocenteNoActivo();
                        case "SIN-DISTRIBUTIVO" -> throw new SinDistributivo();
                        case "SIN-ABRIR" -> throw new PeriodoSinAbrir();
                        case "SIN-HORAS" -> carga("0", "0", "4");
                        default -> carga("8", "4", "21");
                    };
        }
    }
}
