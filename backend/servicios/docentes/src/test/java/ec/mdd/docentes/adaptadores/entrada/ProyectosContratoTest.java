package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.ConsultarProyectos;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Los proyectos aprobados cumplen {@code contratos/docentes/openapi.yaml} (principio IV, F03-H05),
 * con los servicios reales sobre dobles en memoria y los 47 docentes de la carga 06 de prueba.
 */
@WebMvcTest(ProyectosControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ProyectosContratoTest.Dobles.class, ErroresDocentes.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ProyectosContratoTest {

    /** Los datos del proyecto INV-2025-01 de la carga 09 de prueba, sin su código. */
    static final String DATOS =
            "\"nombre\":\"Prototipo de monitoreo IoT para hatos lecheros\","
                    + "\"tipo\":\"INVESTIGACION\","
                    + "\"carreras\":[\"550613A01-P-0301\",\"550811F01-P-0301\"],"
                    + "\"resolucionOcs\":\"RES-OCS-PRUEBA-2025-040\","
                    + "\"fechaResolucion\":\"2025-03-20\",\"estado\":\"EN_EJECUCION\","
                    + "\"vigenciaInicio\":\"2025-04-01\",\"vigenciaFin\":\"2026-12-31\","
                    + "\"director\":\"0109990200\","
                    + "\"participantes\":[{\"docente\":\"0109990200\",\"rol\":\"DIRECTOR\","
                    + "\"horas\":14},{\"docente\":\"0109990226\",\"rol\":\"INVESTIGADOR\","
                    + "\"horas\":10}],\"horasAprobadas\":24";

    static final String NUEVO = "{\"codigo\":\"INV-2025-01\"," + DATOS + "}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("docentes");

    @Autowired private MockMvc mvc;

    @Autowired private DocentesEnMemoria docentes;

    @Autowired private CargosYProyectosEnMemoria datos;

    @BeforeEach
    void reiniciar() {
        docentes.reiniciar();
        datos.reiniciar();
        CargasDePrueba.docentes()
                .values()
                .forEach(docente -> docentes.registrar(docente, "datos.demo", Instant.EPOCH));
    }

    @Test
    void dadoUnProyectoValido_cuandoSeRegistra_entonces201ConLosNombresYSuRegistro()
            throws Exception {
        MvcResult registrado = registrar(NUEVO);

        assertThat(registrado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<String>read(cuerpo(registrado), "$.nombreDelDirector")).isNotBlank();
        assertThat(JsonPath.<List<String>>read(cuerpo(registrado), "$.nombresDeCarreras[*].nombre"))
                .hasSize(2);
        assertThat(JsonPath.<List<String>>read(cuerpo(registrado), "$.participantes[*].nombre"))
                .hasSize(2);
        assertThat(cuerpo(registrado)).doesNotContain("\"asignable\"");
        assertThat(docentes.cambios())
                .singleElement()
                .extracting(CambioRegistrado::accion)
                .isEqualTo(CambioRegistrado.Accion.PROYECTO_REGISTRADO);
        contrato.validar(registrado);
    }

    @Test
    void dadosDatosInvalidos_cuandoSeRegistra_entonces400ConSuCampoYSuElemento() throws Exception {
        assertThat(campo(registrar(NUEVO.replace("INV-2025-01", "inv-2025-01"))))
                .isEqualTo("codigo");
        assertThat(campo(registrar(NUEVO.replace("\"550811F01-P-0301\"", "\"550613A01-P-0301\""))))
                .isEqualTo("carreras[1]");
        assertThat(campo(registrar(NUEVO.replace("\"horas\":10", "\"horas\":2.555"))))
                .isEqualTo("participantes[1].horas");
        assertThat(campo(registrar(NUEVO.replace("0109990226", "0109999904"))))
                .isEqualTo("participantes[1].docente");
        assertThat(
                        campo(
                                registrar(
                                        NUEVO.replace(
                                                "\"director\":\"0109990200\"",
                                                "\"director\":\"0109999904\""))))
                .isEqualTo("director");
        assertThat(campo(registrar(NUEVO.replace("2026-12-31", "2025-04-01"))))
                .isEqualTo("vigenciaFin");
        assertThat(campo(registrar(NUEVO.replace("\"horasAprobadas\":24", "\"horasAprobadas\":0"))))
                .isEqualTo("horasAprobadas");
        MvcResult suman = registrar(NUEVO.replace("\"horas\":10", "\"horas\":10.5"));
        assertThat(campo(suman)).isEqualTo("participantes");
        assertThat(JsonPath.<String>read(cuerpo(suman), "$.detalles[0].descripcion"))
                .contains("suman 24.5 y pasan las 24 aprobadas");
        contrato.validar(suman);
        assertThat(docentes.cambios()).isEmpty();
    }

    @Test
    void dadoUnCodigoRepetido_cuandoSeRegistra_entonces409() throws Exception {
        registrar(NUEVO);

        MvcResult repetido = registrar(NUEVO);

        assertThat(repetido.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(repetido), "$.codigo"))
                .isEqualTo("DOCENTES_PROYECTO_REPETIDO");
        contrato.validar(repetido);
    }

    @Test
    void dadosProyectos_cuandoSeListanConUnPeriodo_entoncesCadaUnoDiceSiEsAsignableYPorQueNo()
            throws Exception {
        registrar(NUEVO);
        registrar(
                "{\"codigo\":\"PPP-2026-DS\","
                        + DATOS.replace("INVESTIGACION", "PRACTICAS_PREPROFESIONALES")
                                .replace("2025-04-01", "2026-04-06")
                                .replace("2026-12-31", "2026-08-14")
                        + "}");
        registrar(
                "{\"codigo\":\"INV-SUSPENDIDO\","
                        + DATOS.replace("EN_EJECUCION", "SUSPENDIDO")
                        + "}");

        MvcResult primero = pedir(get("/api/docentes/proyectos?periodo=2026-I"), lectura());
        MvcResult segundo = pedir(get("/api/docentes/proyectos?periodo=2026-II"), lectura());

        assertThat(JsonPath.<List<String>>read(cuerpo(primero), "$[?(@.asignable == true)].codigo"))
                .containsExactly("INV-2025-01", "PPP-2026-DS");
        assertThat(
                        JsonPath.<List<List<String>>>read(
                                cuerpo(primero), "$[?(@.codigo == 'INV-SUSPENDIDO')].motivos"))
                .containsExactly(List.of("ESTADO"));
        assertThat(
                        JsonPath.<List<List<String>>>read(
                                cuerpo(segundo), "$[?(@.codigo == 'PPP-2026-DS')].motivos"))
                .containsExactly(List.of("VIGENCIA"));
        assertThat(JsonPath.<List<String>>read(cuerpo(segundo), "$[?(@.asignable == true)].codigo"))
                .containsExactly("INV-2025-01");
        contrato.validar(primero);
        contrato.validar(segundo);

        MvcResult practicas =
                pedir(get("/api/docentes/proyectos?tipo=PRACTICAS_PREPROFESIONALES"), lectura());
        assertThat(JsonPath.<List<String>>read(cuerpo(practicas), "$[*].codigo"))
                .containsExactly("PPP-2026-DS");
        assertThat(cuerpo(practicas)).doesNotContain("\"asignable\"");
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeListan_entonces400EnPeriodo() throws Exception {
        MvcResult resultado = pedir(get("/api/docentes/proyectos?periodo=2030-I"), lectura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(campo(resultado)).isEqualTo("periodo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnProyecto_cuandoSeConsultaOSeCorrige_entoncesTraeSusNombresORegistraElCambio()
            throws Exception {
        registrar(NUEVO);

        MvcResult consultado =
                pedir(get("/api/docentes/proyectos/INV-2025-01?periodo=2026-I"), lectura());
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(consultado), "$"))
                .containsEntry("codigo", "INV-2025-01")
                .containsEntry("asignable", true);
        contrato.validar(consultado);

        MvcResult suspendido =
                pedir(
                        json(
                                put("/api/docentes/proyectos/INV-2025-01"),
                                "{" + DATOS.replace("EN_EJECUCION", "SUSPENDIDO") + "}"),
                        escritura());
        assertThat(JsonPath.<String>read(cuerpo(suspendido), "$.estado")).isEqualTo("SUSPENDIDO");
        assertThat(docentes.cambios())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        CambioRegistrado.Accion.PROYECTO_REGISTRADO,
                        CambioRegistrado.Accion.PROYECTO_CORREGIDO);
        contrato.validar(suspendido);
    }

    @Test
    void dadoUnProyectoQueNoExiste_cuandoSeConsultaOSeCorrige_entonces404() throws Exception {
        MvcResult consultado = pedir(get("/api/docentes/proyectos/NO-EXISTE"), lectura());
        MvcResult corregido =
                pedir(
                        json(put("/api/docentes/proyectos/NO-EXISTE"), "{" + DATOS + "}"),
                        escritura());

        assertThat(consultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(corregido.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(consultado);
    }

    private MvcResult registrar(String cuerpo) throws Exception {
        return pedir(json(post("/api/docentes/proyectos"), cuerpo), escritura());
    }

    static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder peticion, String cuerpo) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    static String campo(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        return JsonPath.read(cuerpo(resultado), "$.detalles[0].campo");
    }

    /** Talento Humano: lee y escribe «Docentes» (D-65). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("datos.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:ESCRITURA"));
    }

    /** El Responsable de consolidación: lee el distributivo y, con él, los proyectos (D-70). */
    static JwtRequestPostProcessor lectura() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        DocentesEnMemoria docentesEnMemoria() {
            return new DocentesEnMemoria();
        }

        @Bean
        CargosYProyectosEnMemoria cargosYProyectosEnMemoria(DocentesEnMemoria docentes) {
            return new CargosYProyectosEnMemoria(docentes);
        }

        @Bean
        ConsultarDocentes consultarDocentes(CargosYProyectosEnMemoria datos) {
            return datos.consultarDocentes();
        }

        @Bean
        ConsultarProyectos consultarProyectos(CargosYProyectosEnMemoria datos) {
            return datos.consultarProyectos();
        }

        @Bean
        MantenerProyectos mantenerProyectos(CargosYProyectosEnMemoria datos) {
            return datos.mantenerProyectos();
        }
    }
}
