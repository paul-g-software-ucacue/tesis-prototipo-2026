package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Los docentes cumplen {@code contratos/docentes/openapi.yaml} (principio IV, F03-H04), con los
 * servicios reales sobre un doble en memoria de los puertos de salida.
 */
@WebMvcTest(DocentesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({DocentesContratoTest.Dobles.class, ErroresDocentes.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class DocentesContratoTest {

    /** Los datos de un docente, sin su identificación. */
    static final String DATOS =
            "\"primerApellido\":\"Peralta\",\"segundoApellido\":\"MALDONADO\","
                    + "\"primerNombre\":\"JUAN\",\"segundoNombre\":\"ANDRES\","
                    + "\"correoInstitucional\":\"juan.peralta@instituto-prueba.edu.ec\","
                    + "\"telefono\":\"0990000001\",\"tipoPersonal\":\"OCASIONAL\","
                    + "\"categoriaEscalafon\":\"NO_APLICA\",\"dedicacion\":\"TC\","
                    + "\"horasContratadas\":40,\"fechaIngreso\":\"2019-10-01\","
                    + "\"inicioContrato\":\"2026-04-01\",\"finContrato\":\"2026-08-31\","
                    + "\"carreraBase\":\"C1\",\"ficha\":{\"tercerNivel\":{\"titulo\":"
                    + "\"INGENIERO DE SISTEMAS\",\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\"},"
                    + "\"cuartoNivel\":[{\"titulo\":\"MÁSTER EN TELEMÁTICA\",\"tipo\":\"MAESTRIA\","
                    + "\"campoEspecifico\":\"061\",\"anio\":2015}],"
                    + "\"cursos\":[{\"nombre\":\"PROGRAMACIÓN EN JAVA\",\"campoEspecifico\":\"061\","
                    + "\"horas\":40,\"anio\":2023},{\"nombre\":\"DOCENCIA UNIVERSITARIA\","
                    + "\"campoEspecifico\":\"011\",\"horas\":60,\"anio\":2021}],"
                    + "\"experiencia\":[{\"cargo\":\"DESARROLLADOR\",\"institucion\":\"EMPRESA XYZ\","
                    + "\"campoEspecifico\":\"061\",\"anios\":3}],\"asignaturasAfines\":[\"A1\"]}";

    static final String CEDULA = "0109990010";

    /** Un docente nuevo con la cédula de prueba. */
    static final String NUEVO =
            "{\"tipoIdentificacion\":\"CEDULA\",\"identificacion\":\""
                    + CEDULA
                    + "\","
                    + DATOS
                    + "}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("docentes");

    @Autowired private MockMvc mvc;

    @Autowired private DocentesEnMemoria datos;

    @BeforeEach
    void reiniciar() {
        datos.reiniciar();
    }

    @Test
    void dadoUnDocenteValido_cuandoSeRegistra_entonces201ActivoYConSuRegistro() throws Exception {
        MvcResult registrado = pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());

        assertThat(registrado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(registrado), "$"))
                .containsEntry("estado", "ACTIVO")
                .containsEntry("primerApellido", "PERALTA")
                .containsEntry("nombreCompleto", "PERALTA MALDONADO JUAN ANDRES")
                .containsEntry("nombreDeCarreraBase", "DESARROLLO DE SOFTWARE")
                .containsEntry("condicionEspecial", "NINGUNA");
        assertThat(datos.cambios())
                .singleElement()
                .extracting(CambioRegistrado::accion)
                .isEqualTo(CambioRegistrado.Accion.DOCENTE_REGISTRADO);
        contrato.validar(registrado);
    }

    @Test
    void dadosDatosInvalidos_cuandoSeRegistra_entonces400ConSuCampo() throws Exception {
        MvcResult horas =
                pedir(
                        json(post("/api/docentes/docentes"), NUEVO.replace(":40,", ":41,")),
                        escritura());
        MvcResult cedula =
                pedir(
                        json(post("/api/docentes/docentes"), NUEVO.replace(CEDULA, "0109990011")),
                        escritura());
        MvcResult carrera =
                pedir(
                        json(
                                post("/api/docentes/docentes"),
                                NUEVO.replace(
                                        "\"carreraBase\":\"C1\"", "\"carreraBase\":\"NO-EXISTE\"")),
                        escritura());
        MvcResult curso =
                pedir(
                        json(
                                post("/api/docentes/docentes"),
                                NUEVO.replace("\"horas\":40", "\"horas\":0")),
                        escritura());

        assertThat(List.of(horas, cedula, carrera, curso))
                .allSatisfy(
                        resultado ->
                                assertThat(resultado.getResponse().getStatus()).isEqualTo(400));
        assertThat(campo(horas)).isEqualTo("horasContratadas");
        assertThat(campo(cedula)).isEqualTo("identificacion");
        assertThat(cuerpo(cedula)).doesNotContain("0109990011");
        assertThat(campo(carrera)).isEqualTo("carreraBase");
        assertThat(campo(curso)).isEqualTo("ficha.cursos[0].horas");
        assertThat(datos.cambios()).isEmpty();
        contrato.validar(carrera);
    }

    @Test
    void dadaUnaDedicacionQueNoExiste_cuandoSeGuarda_entonces400QueDiceLosValoresPermitidos()
            throws Exception {
        MvcResult registrado = pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());
        String antes = cuerpo(registrado);

        MvcResult resultado =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA),
                                "{"
                                        + DATOS.replace(
                                                "\"dedicacion\":\"TC\"", "\"dedicacion\":\"XX\"")
                                        + "}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(campo(resultado)).isEqualTo("dedicacion");
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.detalles[0].descripcion"))
                .isEqualTo("no es un valor permitido: usa TC, MT o TP");
        assertThat(cuerpo(pedir(get("/api/docentes/docentes/" + CEDULA), lectura())))
                .isEqualTo(antes);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaIdentificacionOUnCorreoRepetidos_cuandoSeRegistra_entonces409() throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());

        MvcResult repetido = pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());
        MvcResult correo =
                pedir(
                        json(
                                post("/api/docentes/docentes"),
                                NUEVO.replace(CEDULA, "0109990028")
                                        .replace("juan.peralta@", "JUAN.PERALTA@")),
                        escritura());

        assertThat(repetido.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(repetido), "$.codigo"))
                .isEqualTo("DOCENTES_DOCENTE_REPETIDO");
        assertThat(JsonPath.<String>read(cuerpo(correo), "$.codigo"))
                .isEqualTo("DOCENTES_CORREO_REPETIDO");
        assertThat(cuerpo(repetido)).doesNotContain(CEDULA);
        contrato.validar(repetido);
        contrato.validar(correo);
    }

    @Test
    void dadosDocentes_cuandoSeBuscanPorNombreOCedula_entoncesTraeLosQueCoinciden()
            throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());
        pedir(
                json(
                        post("/api/docentes/docentes"),
                        NUEVO.replace(CEDULA, "0109990028")
                                .replace("Peralta", "CABRERA")
                                .replace("juan.peralta@", "ana.cabrera@")),
                escritura());

        MvcResult porNombre =
                pedir(get("/api/docentes/docentes").param("texto", "peraltá"), lectura());
        MvcResult porCedula =
                pedir(get("/api/docentes/docentes").param("texto", "0109990028"), lectura());
        MvcResult todos = pedir(get("/api/docentes/docentes"), lectura());
        MvcResult inactivos =
                pedir(get("/api/docentes/docentes").param("estado", "INACTIVO"), lectura());

        assertThat(JsonPath.<List<String>>read(cuerpo(porNombre), "$[*].identificacion"))
                .containsExactly(CEDULA);
        assertThat(JsonPath.<List<String>>read(cuerpo(porCedula), "$[*].nombreCompleto"))
                .containsExactly("CABRERA MALDONADO JUAN ANDRES");
        assertThat(JsonPath.<List<String>>read(cuerpo(todos), "$[*].identificacion"))
                .containsExactly("0109990028", CEDULA);
        assertThat(JsonPath.<String>read(cuerpo(todos), "$[0].nombreDeCarreraBase"))
                .isEqualTo("DESARROLLO DE SOFTWARE");
        assertThat(JsonPath.<List<Object>>read(cuerpo(inactivos), "$")).isEmpty();
        contrato.validar(porNombre);
    }

    @Test
    void
            dadoUnDocenteConSuFicha_cuandoSeConsulta_entoncesCadaTituloTraeSusCamposYCadaCursoSusHoras()
                    throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());

        MvcResult consultado = pedir(get("/api/docentes/docentes/" + CEDULA), lectura());

        String cuerpo = cuerpo(consultado);
        assertThat(JsonPath.<String>read(cuerpo, "$.ficha.tercerNivel.campoAmplio"))
                .isEqualTo("06");
        assertThat(JsonPath.<String>read(cuerpo, "$.ficha.tercerNivel.campoEspecifico"))
                .isEqualTo("061");
        assertThat(JsonPath.<String>read(cuerpo, "$.ficha.cuartoNivel[0].campoEspecifico"))
                .isEqualTo("061");
        assertThat(JsonPath.<List<Integer>>read(cuerpo, "$.ficha.cursos[*].horas"))
                .containsExactly(40, 60);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.camposAmplios[*].nombre"))
                .containsExactly("Educación", "Tecnologías de la información y la comunicación");
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.asignaturasAfines[*].nombre"))
                .containsExactly("PROGRAMACIÓN");
        contrato.validar(consultado);
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSeConsultaCorrigeOCambiaDeEstado_entonces404()
            throws Exception {
        MvcResult consultado = pedir(get("/api/docentes/docentes/0109990028"), lectura());
        MvcResult corregido =
                pedir(
                        json(put("/api/docentes/docentes/0109990028"), "{" + DATOS + "}"),
                        escritura());
        MvcResult estado =
                pedir(
                        json(
                                put("/api/docentes/docentes/0109990028/estado"),
                                "{\"estado\":\"INACTIVO\"}"),
                        escritura());

        assertThat(List.of(consultado, corregido, estado))
                .allSatisfy(
                        resultado ->
                                assertThat(resultado.getResponse().getStatus()).isEqualTo(404));
        contrato.validar(consultado);
        contrato.validar(corregido);
        contrato.validar(estado);
    }

    @Test
    void dadoUnDocente_cuandoSeCorrigeAgregandoYQuitando_entonces200ConSuRegistro()
            throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());
        String corregidos =
                "{"
                        + DATOS.replace("\"dedicacion\":\"TC\"", "\"dedicacion\":\"MT\"")
                                .replace("\"horasContratadas\":40", "\"horasContratadas\":20")
                                .replace(
                                        "\"experiencia\":[{\"cargo\":\"DESARROLLADOR\",\"institucion\":"
                                                + "\"EMPRESA XYZ\",\"campoEspecifico\":\"061\",\"anios\":3}]",
                                        "\"experiencia\":[]")
                                .replace(
                                        "\"anio\":2021}]",
                                        "\"anio\":2021},{\"nombre\":\"SCRUM\",\"campoEspecifico\":"
                                                + "\"061\",\"horas\":20,\"anio\":2024}]")
                        + "}";

        MvcResult corregido =
                pedir(json(put("/api/docentes/docentes/" + CEDULA), corregidos), escritura());

        assertThat(corregido.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(corregido), "$.dedicacion")).isEqualTo("MT");
        assertThat(JsonPath.<List<Object>>read(cuerpo(corregido), "$.ficha.experiencia")).isEmpty();
        assertThat(JsonPath.<List<String>>read(cuerpo(corregido), "$.ficha.cursos[*].nombre"))
                .containsExactly("PROGRAMACIÓN EN JAVA", "DOCENCIA UNIVERSITARIA", "SCRUM");
        assertThat(datos.cambios())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        CambioRegistrado.Accion.DOCENTE_REGISTRADO,
                        CambioRegistrado.Accion.DOCENTE_CORREGIDO);
        contrato.validar(corregido);
    }

    @Test
    void dadoElCorreoDeOtroDocenteOUnaAfinQueNoExiste_cuandoSeCorrige_entonces409O400()
            throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());
        pedir(
                json(
                        post("/api/docentes/docentes"),
                        NUEVO.replace(CEDULA, "0109990028").replace("juan.peralta@", "ana@")),
                escritura());

        MvcResult correo =
                pedir(
                        json(put("/api/docentes/docentes/0109990028"), "{" + DATOS + "}"),
                        escritura());
        MvcResult propio =
                pedir(
                        json(put("/api/docentes/docentes/" + CEDULA), "{" + DATOS + "}"),
                        escritura());
        MvcResult afin =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA),
                                "{" + DATOS.replace("[\"A1\"]", "[\"A1\",\"ZZ-1\"]") + "}"),
                        escritura());

        assertThat(JsonPath.<String>read(cuerpo(correo), "$.codigo"))
                .isEqualTo("DOCENTES_CORREO_REPETIDO");
        assertThat(propio.getResponse().getStatus()).isEqualTo(200);
        assertThat(afin.getResponse().getStatus()).isEqualTo(400);
        assertThat(campo(afin)).isEqualTo("ficha.asignaturasAfines[1]");
        contrato.validar(correo);
        contrato.validar(afin);
    }

    @Test
    void dadoUnDocente_cuandoCambiaDeEstadoYVuelve_entonces200ConSuRegistro() throws Exception {
        pedir(json(post("/api/docentes/docentes"), NUEVO), escritura());

        MvcResult licencia =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA + "/estado"),
                                "{\"estado\":\"LICENCIA\"}"),
                        escritura());
        MvcResult mismo =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA + "/estado"),
                                "{\"estado\":\"LICENCIA\"}"),
                        escritura());
        MvcResult activo =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA + "/estado"),
                                "{\"estado\":\"ACTIVO\"}"),
                        escritura());
        MvcResult invalido =
                pedir(
                        json(
                                put("/api/docentes/docentes/" + CEDULA + "/estado"),
                                "{\"estado\":\"JUBILADO\"}"),
                        escritura());

        assertThat(JsonPath.<String>read(cuerpo(licencia), "$.estado")).isEqualTo("LICENCIA");
        assertThat(mismo.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(activo), "$.estado")).isEqualTo("ACTIVO");
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(invalido), "$.detalles[0].descripcion"))
                .isEqualTo("no es un valor permitido: usa ACTIVO, LICENCIA o INACTIVO");
        assertThat(datos.cambios())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        CambioRegistrado.Accion.DOCENTE_REGISTRADO,
                        CambioRegistrado.Accion.DOCENTE_ESTADO_CAMBIADO,
                        CambioRegistrado.Accion.DOCENTE_ESTADO_CAMBIADO);
        contrato.validar(licencia);
        contrato.validar(invalido);
    }

    @Test
    void dadasLasReferencias_cuandoSePiden_entoncesTraenCarrerasAsignaturasYLos11Campos()
            throws Exception {
        MvcResult referencias = pedir(get("/api/docentes/referencias"), lectura());

        assertThat(JsonPath.<List<String>>read(cuerpo(referencias), "$.carreras[*].codigo"))
                .contains("C1", "550613A01-P-0301");
        assertThat(
                        JsonPath.<List<String>>read(
                                cuerpo(referencias), "$.asignaturas[?(@.codigo == 'A1')].carrera"))
                .containsExactly("C1");
        assertThat(JsonPath.<List<Object>>read(cuerpo(referencias), "$.camposAmplios")).hasSize(11);
        contrato.validar(referencias);
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

    private static String campo(MvcResult resultado) throws Exception {
        return JsonPath.read(cuerpo(resultado), "$.detalles[0].campo");
    }

    /** Talento Humano: lee y escribe «Docentes» (D-65). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("datos.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:ESCRITURA"));
    }

    /** El Responsable de consolidación: lee el distributivo y, con él, los docentes (D-70). */
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
        ConsultarDocentes consultarDocentes(DocentesEnMemoria datos) {
            return datos.consultar();
        }

        @Bean
        MantenerDocentes mantenerDocentes(DocentesEnMemoria datos) {
            return datos.mantener();
        }
    }
}
