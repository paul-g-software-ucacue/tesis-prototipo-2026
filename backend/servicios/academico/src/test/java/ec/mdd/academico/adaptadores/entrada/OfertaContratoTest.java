package ec.mdd.academico.adaptadores.entrada;

import static ec.mdd.academico.adaptadores.entrada.AcademicoContratoTest.json;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.cuerpo;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.escritura;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.lectura;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * La oferta del periodo cumple {@code contratos/academico/openapi.yaml} (principio IV, F03-H03),
 * con los servicios reales sobre dobles en memoria de los repositorios.
 */
@WebMvcTest(OfertaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    OfertaContratoTest.Dobles.class,
    CarrerasContratoTest.Dobles.class,
    ErroresAcademico.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class OfertaContratoTest {

    static final String RUTA = "/api/academico/periodos/2026-I/oferta";
    static final String POO = "DS-P-II-POO-204";

    static final String NOCTURNA =
            "{\"asignatura\":\""
                    + POO
                    + "\",\"paralelo\":\"A_NOCTURNA\",\"jornada\":\"NOCTURNA\","
                    + "\"modalidad\":\"PRESENCIAL\",\"estudiantes\":28}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private CarrerasEnMemoria carreras;

    @Autowired private OfertaEnMemoria oferta;

    /** DS vigente y nocturna, con POO de 4 horas y CONT, de otra carrera, inactiva. */
    @BeforeEach
    void reiniciar() {
        carreras.reiniciar();
        oferta.reiniciar();
        Instant ahora = Instant.now();
        carreras.registrar(
                Carrera.nueva(
                        "550613A01-P-0301",
                        "DESARROLLO DE SOFTWARE",
                        "DS",
                        "MATRIZ",
                        NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                        Modalidad.PRESENCIAL,
                        List.of(Jornada.NOCTURNA),
                        new CampoConocimiento("06", "061"),
                        null,
                        5),
                "x",
                ahora);
        carreras.registrar(Malla.nueva("DS-2019", "550613A01-P-0301"), "x", ahora);
        carreras.registrar(asignatura(POO, true), "x", ahora);
        carreras.registrar(asignatura("DS-INACTIVA", false), "x", ahora);
    }

    static Asignatura asignatura(String codigo, boolean activa) {
        return Asignatura.nueva(
                        codigo,
                        "550613A01-P-0301",
                        "DS-2019",
                        "PROGRAMACIÓN ORIENTADA A OBJETOS",
                        2,
                        UnidadDeOrganizacionCurricular.PROFESIONAL,
                        new BigDecimal("4"),
                        null,
                        null,
                        false,
                        new CampoConocimiento("06", "061"),
                        List.of("programación", "objetos"))
                .conEstado(activa);
    }

    @Test
    void
            dadaUnaAsignaturaActiva_cuandoSeOferta_entonces201ConLasHorasDeLaAsignaturaYLaSedeDeLaCarrera()
                    throws Exception {
        MvcResult resultado = pedir(json(post(RUTA), NOCTURNA), escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("estado", "OFERTADA")
                .containsEntry("sede", "MATRIZ")
                .containsEntry("sedeIndicada", false)
                .containsEntry("horasIndicadas", false)
                .containsEntry("ciclo", 2);
        assertThat(JsonPath.<Number>read(cuerpo(resultado), "$.horasSemanales").doubleValue())
                .isEqualTo(4.0);
        contrato.validar(resultado);
    }

    @Test
    void dadosDatosQueNoCumplenSusReglas_cuandoSeOferta_entonces400ConSuCampo() throws Exception {
        Map<String, String> casos =
                Map.of(
                        "jornada",
                                NOCTURNA.replace(
                                        "\"jornada\":\"NOCTURNA\"", "\"jornada\":\"MATUTINA\""),
                        "horasSemanales",
                                NOCTURNA.replace(
                                        "\"estudiantes\":28",
                                        "\"estudiantes\":28,\"horasSemanales\":5"),
                        "asignatura", NOCTURNA.replace(POO, "DS-INACTIVA"),
                        "sede",
                                NOCTURNA.replace(
                                        "\"estudiantes\":28",
                                        "\"estudiantes\":28,\"sede\":\"RECT\""));
        for (Map.Entry<String, String> caso : casos.entrySet()) {
            MvcResult resultado = pedir(json(post(RUTA), caso.getValue()), escritura());

            assertThat(resultado.getResponse().getStatus()).as(caso.getKey()).isEqualTo(400);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.detalles[0].campo"))
                    .isEqualTo(caso.getKey());
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaUnaRepetidaOUnPeriodoFueraDePlanificacion_cuandoSeOferta_entonces409ConSuCodigo()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult repetida = pedir(json(post(RUTA), NOCTURNA), escritura());
        MvcResult cerrada =
                pedir(json(post("/api/academico/periodos/2025-II/oferta"), NOCTURNA), escritura());

        assertThat(repetida.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(repetida)).contains("ACADEMICO_ASIGNATURA_PARALELO_REPETIDA");
        assertThat(cuerpo(cerrada)).contains("ACADEMICO_OFERTA_CERRADA");
        contrato.validar(repetida);
        contrato.validar(cerrada);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeOfertaOSeConsulta_entonces404() throws Exception {
        for (MvcResult resultado :
                List.of(
                        pedir(
                                json(post("/api/academico/periodos/NADA/oferta"), NOCTURNA),
                                escritura()),
                        pedir(get("/api/academico/periodos/NADA/oferta"), lectura()))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            contrato.validar(resultado);
        }
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    /** El Responsable de consolidación: lee la oferta con «Oferta: lectura» (D-65). */
    static JwtRequestPostProcessor oferta() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_OFERTA:LECTURA"));
    }

    /** La oferta en memoria; las carreras son las de {@link CarrerasContratoTest.Dobles}. */
    @TestConfiguration
    static class Dobles {

        @Bean
        OfertaEnMemoria ofertaEnMemoria() {
            return new OfertaEnMemoria();
        }

        @Bean
        ConsultarOferta consultarOferta(OfertaEnMemoria oferta, CarrerasEnMemoria carreras) {
            return oferta.consultar(carreras);
        }

        @Bean
        MantenerOferta mantenerOferta(OfertaEnMemoria oferta, CarrerasEnMemoria carreras) {
            return oferta.mantener(carreras);
        }
    }
}
