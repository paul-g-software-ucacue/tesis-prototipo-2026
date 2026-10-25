package ec.mdd.docentes.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.Dedicacion;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.dominio.TipoDeIdentificacion;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerCargos;
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
 * Los cargos cumplen {@code contratos/docentes/openapi.yaml} (principio IV, F03-H05), con los
 * servicios reales sobre dobles en memoria de los puertos de salida.
 */
@WebMvcTest(CargosControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({CargosContratoTest.Dobles.class, ErroresDocentes.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class CargosContratoTest {

    /** El docente de prueba, activo a tiempo completo y con maestría. */
    static final String CON_MAESTRIA = "0109990010";

    /** Otro docente ficticio, activo a tiempo completo y sin títulos de cuarto nivel. */
    static final String SIN_CUARTO_NIVEL = "0109990028";

    /** Un docente ficticio de medio tiempo. */
    static final String MEDIO_TIEMPO = "0109990044";

    static final String COORDINADOR =
            "{\"docente\":\""
                    + CON_MAESTRIA
                    + "\",\"tipo\":\"COORDINADOR_CARRERA\",\"fechaDesignacion\":\"2025-04-01\","
                    + "\"carrera\":\"550613A01-P-0301\",\"documento\":\"RES-PRUEBA-2025-004\"}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("docentes");

    @Autowired private MockMvc mvc;

    @Autowired private DocentesEnMemoria docentes;

    @Autowired private CargosYProyectosEnMemoria datos;

    @BeforeEach
    void reiniciar() {
        docentes.reiniciar();
        datos.reiniciar();
        Docente base = CargasDePrueba.docente(true);
        docentes.registrar(base, "datos.demo", Instant.EPOCH);
        docentes.registrar(
                new Docente(
                        cedula(SIN_CUARTO_NIVEL),
                        CargasDePrueba.docente(false).datos(),
                        EstadoLaboral.ACTIVO),
                "datos.demo",
                Instant.EPOCH);
        docentes.registrar(
                new Docente(
                        cedula(MEDIO_TIEMPO),
                        CargasDePrueba.docente(true, Dedicacion.MT).datos(),
                        EstadoLaboral.ACTIVO),
                "datos.demo",
                Instant.EPOCH);
    }

    @Test
    void dadoUnCoordinadorValido_cuandoSeRegistra_entonces201VigenteSinAdvertenciasYConSuRegistro()
            throws Exception {
        MvcResult registrado = pedir(json(post("/api/docentes/cargos"), COORDINADOR), escritura());

        assertThat(registrado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(registrado), "$"))
                .containsEntry("estado", "VIGENTE")
                .containsEntry("nombreDeCarrera", "DESARROLLO DE SOFTWARE")
                .containsEntry("nombreDelDocente", "PERALTA MALDONADO JUAN ANDRES")
                .containsEntry("advertencias", List.of());
        assertThat(docentes.cambios())
                .singleElement()
                .extracting(CambioRegistrado::accion)
                .isEqualTo(CambioRegistrado.Accion.CARGO_REGISTRADO);
        contrato.validar(registrado);
    }

    @Test
    void dadosDatosInvalidos_cuandoSeRegistra_entonces400ConSuCampo() throws Exception {
        assertThat(campo(registrar(COORDINADOR.replace(",\"carrera\":\"550613A01-P-0301\"", ""))))
                .isEqualTo("carrera");
        assertThat(campo(registrar(COORDINADOR.replace("550613A01-P-0301", "NO-EXISTE"))))
                .isEqualTo("carrera");
        assertThat(
                        campo(
                                registrar(
                                        "{\"docente\":\""
                                                + CON_MAESTRIA
                                                + "\",\"tipo\":\"G07_GESTOR_INVESTIGACION\","
                                                + "\"fechaDesignacion\":\"2025-04-01\","
                                                + "\"unidad\":\"G-VIN\",\"documento\":\"RES-1\"}")))
                .isEqualTo("unidad");
        assertThat(campo(registrar(COORDINADOR.replace("}", ",\"fechaFin\":\"2027-04-02\"}"))))
                .isEqualTo("fechaFin");
        MvcResult documento = registrar(COORDINADOR.replace("RES-PRUEBA-2025-004", "R".repeat(61)));
        assertThat(documento.getResponse().getStatus()).isEqualTo(400);
        assertThat(campo(documento)).isEqualTo("documento");
        contrato.validar(documento);
        assertThat(docentes.cambios()).isEmpty();
    }

    @Test
    void dadoUnCargoFueraDelCatalogo_cuandoSeRegistra_entonces400ConLosValoresPermitidos()
            throws Exception {
        MvcResult fuera = registrar(COORDINADOR.replace("COORDINADOR_CARRERA", "DECANO"));

        assertThat(fuera.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(fuera), "$.detalles[0].descripcion"))
                .contains("no es un valor permitido")
                .contains("G21_GESTOR_ESTADISTICAS");
    }

    @Test
    void dadosLosTresConflictos_cuandoSeRegistra_entoncesSu409() throws Exception {
        registrar(COORDINADOR);

        MvcResult repetido = registrar(COORDINADOR);
        MvcResult segundo =
                registrar(
                        COORDINADOR
                                .replace(CON_MAESTRIA, SIN_CUARTO_NIVEL)
                                .replace("2025-04-01", "2025-05-01"));
        MvcResult medioTiempo = registrar(COORDINADOR.replace(CON_MAESTRIA, MEDIO_TIEMPO));

        assertThat(codigo(repetido)).isEqualTo("DOCENTES_CARGO_REPETIDO");
        assertThat(codigo(segundo)).isEqualTo("DOCENTES_COORDINADOR_VIGENTE");
        assertThat(codigo(medioTiempo)).isEqualTo("DOCENTES_DOCENTE_NO_HABILITADO");
        assertThat(cuerpo(medioTiempo)).doesNotContain(MEDIO_TIEMPO);
        contrato.validar(repetido);
        contrato.validar(segundo);
        contrato.validar(medioTiempo);
    }

    @Test
    void dadosVariosCargos_cuandoSeListan_entoncesSeFiltranPorEstadoYPorDocenteConSusNombres()
            throws Exception {
        registrar(COORDINADOR);
        registrar(
                "{\"docente\":\""
                        + SIN_CUARTO_NIVEL
                        + "\",\"tipo\":\"G07_GESTOR_INVESTIGACION\",\"fechaDesignacion\":"
                        + "\"2025-04-01\",\"unidad\":\"G-INV\",\"documento\":\"RES-2\","
                        + "\"estado\":\"FINALIZADO\",\"fechaFin\":\"2026-03-31\"}");

        MvcResult todos = pedir(get("/api/docentes/cargos"), lectura());
        MvcResult vigentes = pedir(get("/api/docentes/cargos?estado=VIGENTE"), lectura());
        MvcResult delGestor =
                pedir(get("/api/docentes/cargos?docente=" + SIN_CUARTO_NIVEL), lectura());

        assertThat(JsonPath.<List<String>>read(cuerpo(todos), "$[*].tipo"))
                .containsExactly("COORDINADOR_CARRERA", "G07_GESTOR_INVESTIGACION");
        assertThat(JsonPath.<String>read(cuerpo(todos), "$[1].nombreDeUnidad"))
                .isEqualTo("Gestión de Investigación, Desarrollo Tecnológico e Innovación");
        assertThat(JsonPath.<List<String>>read(cuerpo(vigentes), "$[*].tipo"))
                .containsExactly("COORDINADOR_CARRERA");
        assertThat(JsonPath.<List<String>>read(cuerpo(delGestor), "$[*].docente"))
                .containsExactly(SIN_CUARTO_NIVEL);
        contrato.validar(todos);
    }

    @Test
    void dadoUnCoordinadorSinCuartoNivel_cuandoSeRegistra_entonces201ConLaAdvertenciaDelRequisito()
            throws Exception {
        MvcResult registrado = registrar(COORDINADOR.replace(CON_MAESTRIA, SIN_CUARTO_NIVEL));

        assertThat(registrado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<List<Map<String, String>>>read(cuerpo(registrado), "$.advertencias"))
                .containsExactly(
                        Map.of(
                                "codigo",
                                "CUARTO_NIVEL",
                                "descripcion",
                                "Le falta un título de cuarto nivel registrado (RN-20)."));
        contrato.validar(registrado);

        // La lista la sigue mostrando, y deja de mostrarla cuando la ficha gana una maestría.
        assertThat(cuerpo(pedir(get("/api/docentes/cargos"), lectura()))).contains("CUARTO_NIVEL");
        docentes.guardar(
                new Docente(
                        cedula(SIN_CUARTO_NIVEL),
                        CargasDePrueba.docente(true).datos(),
                        EstadoLaboral.ACTIVO));
        assertThat(cuerpo(pedir(get("/api/docentes/cargos"), lectura())))
                .doesNotContain("CUARTO_NIVEL");
    }

    @Test
    void
            dadoUnCoordinadorVigenteCuyoDocentePasaALicencia_cuandoSeListan_entoncesLoAdvierteYNoLeDaOtro()
                    throws Exception {
        registrar(COORDINADOR);
        docentes.guardar(CargasDePrueba.docente(true).conEstado(EstadoLaboral.LICENCIA));

        assertThat(cuerpo(pedir(get("/api/docentes/cargos"), lectura())))
                .contains("DOCENTE_NO_HABILITADO");
        MvcResult otro =
                registrar(
                        "{\"docente\":\""
                                + CON_MAESTRIA
                                + "\",\"tipo\":\"RECTOR\",\"fechaDesignacion\":\"2025-04-01\","
                                + "\"unidad\":\"RECT\",\"documento\":\"RES-1\"}");
        assertThat(codigo(otro)).isEqualTo("DOCENTES_DOCENTE_NO_HABILITADO");
    }

    @Test
    void
            dadoUnCoordinadorVigente_cuandoSeFinaliza_entoncesSuCarreraPuedeRecibirOtroYQuedaRegistrado()
                    throws Exception {
        registrar(COORDINADOR);
        String ruta = "/api/docentes/cargos/" + CON_MAESTRIA + "/COORDINADOR_CARRERA/2025-04-01";

        MvcResult finalizado =
                pedir(
                        json(
                                put(ruta),
                                "{\"carrera\":\"550613A01-P-0301\",\"fechaFin\":\"2026-03-31\","
                                        + "\"documento\":\"RES-PRUEBA-2026-099\","
                                        + "\"estado\":\"FINALIZADO\"}"),
                        escritura());
        MvcResult otro =
                registrar(
                        COORDINADOR
                                .replace(CON_MAESTRIA, SIN_CUARTO_NIVEL)
                                .replace("2025-04-01", "2026-04-01"));
        MvcResult revivido =
                pedir(
                        json(
                                put(ruta),
                                "{\"carrera\":\"550613A01-P-0301\","
                                        + "\"documento\":\"RES-PRUEBA-2025-004\","
                                        + "\"estado\":\"VIGENTE\"}"),
                        escritura());

        assertThat(finalizado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(finalizado), "$.estado")).isEqualTo("FINALIZADO");
        assertThat(otro.getResponse().getStatus()).isEqualTo(201);
        assertThat(codigo(revivido)).isEqualTo("DOCENTES_COORDINADOR_VIGENTE");
        assertThat(docentes.cambios())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        CambioRegistrado.Accion.CARGO_REGISTRADO,
                        CambioRegistrado.Accion.CARGO_CORREGIDO,
                        CambioRegistrado.Accion.CARGO_REGISTRADO);
        contrato.validar(finalizado);
        contrato.validar(revivido);
    }

    @Test
    void dadoUnCargoQueNoExiste_cuandoSeCorrige_entonces404() throws Exception {
        MvcResult resultado =
                pedir(
                        json(
                                put("/api/docentes/cargos/" + CON_MAESTRIA + "/RECTOR/2025-04-01"),
                                "{\"unidad\":\"RECT\",\"documento\":\"RES-1\","
                                        + "\"estado\":\"VIGENTE\"}"),
                        escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    private MvcResult registrar(String cuerpo) throws Exception {
        return pedir(json(post("/api/docentes/cargos"), cuerpo), escritura());
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
        return JsonPath.read(cuerpo(resultado), "$.detalles[0].campo");
    }

    static String codigo(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        return JsonPath.read(cuerpo(resultado), "$.codigo");
    }

    static Identificacion cedula(String numero) {
        return new Identificacion(TipoDeIdentificacion.CEDULA, numero);
    }

    /** Talento Humano: lee y escribe «Docentes» (D-65). */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("datos.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_DOCENTES:ESCRITURA"));
    }

    /** El Responsable de consolidación: lee el distributivo y, con él, los cargos (D-70). */
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
        ConsultarCargos consultarCargos(CargosYProyectosEnMemoria datos) {
            return datos.consultarCargos();
        }

        @Bean
        MantenerCargos mantenerCargos(CargosYProyectosEnMemoria datos) {
            return datos.mantenerCargos();
        }
    }
}
