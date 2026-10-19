package ec.mdd.normativa.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Las operaciones de {@code /api/normativa} cumplen {@code contratos/normativa/openapi.yaml}
 * (principio IV), con un doble de la consulta escrito a mano (principio VIII).
 */
@WebMvcTest(NormativaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(NormativaContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class NormativaContratoTest {

    static final VersionNormativa VERSION_1 = version();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("normativa");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElVicerrector_cuandoListaLasVersiones_entonces200SegunElContrato() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones"));

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"numero\":1,\"codigo\":\"SENESCYT-2024-03\","
                                + "\"origen\":\"Política SENESCYT 2024\","
                                + "\"vigenteDesde\":\"2024-03-11\",\"tipoIst\":\"T2\"}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionUno_cuandoSeConsulta_entonces200ConSusReglasYSuTabla() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones/1"));

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.reglas[*].identificador"))
                .containsExactly("RN-01", "RN-02");
        assertThat(JsonPath.<Integer>read(cuerpo, "$.reglas[0].parametros[0].valor")).isEqualTo(40);
        // RN-01 es solo parámetros: sin severidad, el campo se omite.
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$.reglas[0]"))
                .doesNotContainKey("severidad");
        assertThat(JsonPath.<String>read(cuerpo, "$.reglas[1].severidad")).isEqualTo("DURA");
        Map<String, Object> docente = JsonPath.read(cuerpo, "$.tablaCargaHoraria[0]");
        assertThat(docente)
                .containsEntry("aplicaA", "TODOS")
                .containsEntry("perfil", "DOCENTE_TC")
                .containsEntry("horasClase", 22)
                .containsEntry("horasClaseMinimo", 18)
                .containsEntry("horasClaseMaximo", 24)
                .containsEntry("horasOtrasDocencia", 18)
                .containsEntry("horasGestion", 0)
                .doesNotContainKey("horasInvestigacionVinculacionMaximo")
                .doesNotContainKey("observacion");
        assertThat(JsonPath.<String>read(cuerpo, "$.tablaCargaHoraria[1].aplicaA")).isEqualTo("T2");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaVersionQueNoExiste_cuandoSeConsulta_entonces404Comun() throws Exception {
        MvcResult resultado = pedir(get("/api/normativa/versiones/9"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoLaVersionUno_cuandoSeIntentaCambiar_entonces405PorqueNoHayComo() throws Exception {
        // El contrato no tiene estas operaciones: se comprueba el estado y el código, sin validar.
        MvcResult crear =
                pedir(
                        post("/api/normativa/versiones")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"));
        MvcResult cambiar =
                pedir(
                        put("/api/normativa/versiones/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"));

        for (MvcResult resultado : List.of(crear, cambiar)) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(405);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("COMUN_METODO_NO_PERMITIDO");
        }
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.with(vicerrector())).andReturn();
    }

    private static JwtRequestPostProcessor vicerrector() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_PARAMETROS:LECTURA"));
    }

    private static VersionNormativa version() {
        Map<String, BigDecimal> dedicaciones = new LinkedHashMap<>();
        dedicaciones.put("dedicacion_tiempo_completo", BigDecimal.valueOf(40));
        return new VersionNormativa(
                1,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                new TablaCargaHoraria(
                        List.of(
                                new FilaCargaHoraria(
                                        null,
                                        "DOCENTE_TC",
                                        40,
                                        22,
                                        18,
                                        24,
                                        18,
                                        0,
                                        null,
                                        Severidad.DURA,
                                        "POL 6",
                                        null),
                                new FilaCargaHoraria(
                                        TipoIst.T2,
                                        "GESTOR",
                                        40,
                                        14,
                                        14,
                                        14,
                                        16,
                                        10,
                                        null,
                                        Severidad.DURA,
                                        "POL 6 (docentes gestores)",
                                        null))),
                List.of(
                        new Regla("RN-01", null, dedicaciones),
                        new Regla(
                                "RN-02",
                                Severidad.DURA,
                                Map.of("horas_maximas_docente", BigDecimal.valueOf(40)))));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarNormativa consultarNormativa() {
            return new ConsultarNormativa() {

                @Override
                public List<VersionNormativa> versiones() {
                    return List.of(VERSION_1);
                }

                @Override
                public VersionNormativa version(int numero) {
                    if (numero != 1) {
                        throw new VersionNoEncontrada(numero);
                    }
                    return VERSION_1;
                }
            };
        }
    }
}
