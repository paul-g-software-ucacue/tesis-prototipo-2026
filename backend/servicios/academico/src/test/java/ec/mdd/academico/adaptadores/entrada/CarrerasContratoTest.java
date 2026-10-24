package ec.mdd.academico.adaptadores.entrada;

import static ec.mdd.academico.adaptadores.entrada.AcademicoContratoTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.charset.StandardCharsets;
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
 * Las carreras, mallas y asignaturas cumplen {@code contratos/academico/openapi.yaml} (principio
 * IV, F03-H02), con los servicios reales sobre un doble en memoria de los repositorios.
 */
@WebMvcTest(CarrerasControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({CarrerasContratoTest.Dobles.class, ErroresAcademico.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class CarrerasContratoTest {

    static final String DS =
            "{\"codigo\":\"550613A01-P-0301\",\"nombre\":\"DESARROLLO DE SOFTWARE\",\"siglas\":\"DS\","
                    + "\"sede\":\"MATRIZ\",\"nivelFormacion\":\"TECNOLOGICO_SUPERIOR\","
                    + "\"modalidad\":\"PRESENCIAL\",\"jornadas\":[\"NOCTURNA\"],"
                    + "\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\",\"campoDetallado\":\"0613\","
                    + "\"numeroCiclos\":5}";

    static final String MD =
            "{\"codigo\":\"DS-B-I-MD-101\",\"carrera\":\"550613A01-P-0301\",\"malla\":\"DS-2019\","
                    + "\"nombre\":\"MATEMÁTICA DISCRETA\",\"ciclo\":1,"
                    + "\"unidadOrganizacionCurricular\":\"BASICA\",\"horasSemanalesClase\":3,"
                    + "\"troncoComun\":true,\"campoAmplio\":\"05\",\"campoEspecifico\":\"054\","
                    + "\"palabrasClave\":[\"matemática\",\"lógica\",\"conjuntos\"]}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private CarrerasEnMemoria datos;

    @BeforeEach
    void reiniciar() {
        datos.reiniciar();
    }

    @Test
    void dadaUnaCarreraValida_cuandoSeRegistra_entonces201YSeListaConSusMallas() throws Exception {
        MvcResult registrada = pedir(json(post("/api/academico/carreras"), DS), escritura());

        assertThat(registrada.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(registrada), "$"))
                .containsEntry("estado", "VIGENTE")
                .containsEntry("campoDetallado", "0613")
                .containsEntry("mallas", List.of());
        contrato.validar(registrada);

        pedir(
                json(
                        post("/api/academico/carreras/550613A01-P-0301/mallas"),
                        "{\"codigo\":\"DS-2019\"}"),
                escritura());
        MvcResult lista = pedir(get("/api/academico/carreras"), lectura());
        assertThat(JsonPath.<List<String>>read(cuerpo(lista), "$[0].mallas[*].codigo"))
                .containsExactly("DS-2019");
        contrato.validar(lista);
        MvcResult una = pedir(get("/api/academico/carreras/550613A01-P-0301"), lectura());
        assertThat(una.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(una);
    }

    @Test
    void dadoUnCodigoOUnasSiglasRepetidos_cuandoSeRegistra_entonces409ConSuCodigo()
            throws Exception {
        pedir(json(post("/api/academico/carreras"), DS), escritura());

        MvcResult codigo = pedir(json(post("/api/academico/carreras"), DS), escritura());
        MvcResult siglas =
                pedir(
                        json(
                                post("/api/academico/carreras"),
                                DS.replace("550613A01-P-0301", "OTRA")),
                        escritura());

        assertThat(codigo.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(codigo)).contains("ACADEMICO_CARRERA_REPETIDA");
        assertThat(cuerpo(siglas)).contains("ACADEMICO_SIGLAS_REPETIDAS");
        contrato.validar(codigo);
        contrato.validar(siglas);
    }

    @Test
    void
            dadaUnaSedeQueNoEsSedeOUnCampoEspecificoDeOtroAmplio_cuandoSeRegistra_entonces400ConSuCampo()
                    throws Exception {
        MvcResult sede =
                pedir(
                        json(
                                post("/api/academico/carreras"),
                                DS.replace("\"sede\":\"MATRIZ\"", "\"sede\":\"RECT\"")),
                        escritura());
        MvcResult campo =
                pedir(
                        json(post("/api/academico/carreras"), DS.replace("\"061\"", "\"071\"")),
                        escritura());

        assertThat(sede.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(sede), "$.detalles[0].campo")).isEqualTo("sede");
        assertThat(JsonPath.<String>read(cuerpo(campo), "$.detalles[0].campo"))
                .isEqualTo("campoEspecifico");
        contrato.validar(sede);
    }

    @Test
    void dadaUnaCarrera_cuandoSeCorrigeYCambiaSuEstado_entonces200() throws Exception {
        pedir(json(post("/api/academico/carreras"), DS), escritura());
        String datos =
                "{\"nombre\":\"SOFTWARE\",\"siglas\":\"SW\",\"sede\":\"MATRIZ\","
                        + "\"nivelFormacion\":\"TECNOLOGICO_SUPERIOR\",\"modalidad\":\"DUAL\","
                        + "\"jornadas\":[\"MATUTINA\",\"NOCTURNA\"],\"campoAmplio\":\"06\","
                        + "\"campoEspecifico\":\"061\",\"numeroCiclos\":6}";

        MvcResult corregida =
                pedir(json(put("/api/academico/carreras/550613A01-P-0301"), datos), escritura());
        MvcResult enCierre =
                pedir(
                        json(
                                put("/api/academico/carreras/550613A01-P-0301/estado"),
                                "{\"estado\":\"EN_CIERRE\"}"),
                        escritura());

        assertThat(JsonPath.<String>read(cuerpo(corregida), "$.siglas")).isEqualTo("SW");
        assertThat(JsonPath.<List<String>>read(cuerpo(corregida), "$.jornadas"))
                .containsExactly("MATUTINA", "NOCTURNA");
        assertThat(JsonPath.<String>read(cuerpo(enCierre), "$.estado")).isEqualTo("EN_CIERRE");
        contrato.validar(corregida);
        contrato.validar(enCierre);
    }

    @Test
    void dadaUnaCarreraQueNoExiste_cuandoSeConsultaOCorrige_entonces404() throws Exception {
        for (MvcResult resultado :
                List.of(
                        pedir(get("/api/academico/carreras/NADA"), lectura()),
                        pedir(get("/api/academico/carreras/NADA/asignaturas"), lectura()),
                        pedir(
                                json(
                                        put("/api/academico/carreras/NADA/estado"),
                                        "{\"estado\":\"VIGENTE\"}"),
                                escritura()),
                        pedir(
                                json(
                                        post("/api/academico/carreras/NADA/mallas"),
                                        "{\"codigo\":\"M-1\"}"),
                                escritura()),
                        pedir(get("/api/academico/asignaturas/NADA"), lectura()))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            contrato.validar(resultado);
        }
    }

    @Test
    void dadosLosCamposAmplios_cuandoSeListan_entoncesLos11ConSuNombre() throws Exception {
        MvcResult resultado = pedir(get("/api/academico/campos-amplios"), catalogos());

        assertThat(JsonPath.<List<String>>read(cuerpo(resultado), "$[*].codigo")).hasSize(11);
        contrato.validar(resultado);
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** El Responsable de consolidación: lee el distributivo y, con él, las carreras (D-70). */
    static JwtRequestPostProcessor lectura() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    /** Solo «Catálogos y datos maestros: lectura». */
    static JwtRequestPostProcessor catalogos() {
        return jwt().jwt(token -> token.subject("catalogos.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_CATALOGOS:LECTURA"));
    }

    /** El Vicerrector: escribe con «Distributivo del periodo: escritura» (aclaración 1). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:ESCRITURA"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        CarrerasEnMemoria carrerasEnMemoria() {
            return new CarrerasEnMemoria();
        }

        @Bean
        ConsultarCarreras consultarCarreras(CarrerasEnMemoria datos) {
            return datos.consultar();
        }

        @Bean
        MantenerCarreras mantenerCarreras(CarrerasEnMemoria datos) {
            return datos.mantener();
        }
    }
}
