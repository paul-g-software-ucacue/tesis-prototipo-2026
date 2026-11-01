package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Proponer, quitar y ver los parciales cumple su contrato (RF-24, principio IV), con tokens con
 * reclamos reales: el alcance sale del permiso que se usa (D-64).
 */
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
class PropuestaContratoTest {

    private static final String RUTA = "/api/generacion/periodos/%s/oferta/POO-204/A/propuesta";
    private static final String PARCIALES = "/api/generacion/periodos/%s/parciales";
    private static final String CUERPO = "{\"docente\":\"0109990010\"}";
    private static final String ESCRIBE = "OFERTA:ESCRITURA:PROPIO";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnCoordinador_cuandoPropone_entonces200ConLaPropuesta() throws Exception {
        MvcResult resultado = proponer("2026-I", CUERPO, "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"asignatura\":\"POO-204\",\"paralelo\":\"A\","
                                + "\"docente\":\"0109990010\",\"nombreDocente\":\"PEREZ LUIS\","
                                + "\"justificacion\":\"AFINIDAD\",\"retirada\":false,"
                                + "\"horasSuficientes\":true}");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaExcepcion_cuandoPropone_entoncesLaRespuestaTraeElTexto() throws Exception {
        MvcResult resultado =
                proponer(
                        "2026-I",
                        "{\"docente\":\"0109990010\",\"texto\":\"Dictó la materia tres años\"}",
                        "TDS",
                        ESCRIBE);

        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"justificacion\":\"EXCEPCION\"", "Dictó la materia tres años");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaExcepcionSinTexto_cuandoPropone_entonces400ConElCampoTexto() throws Exception {
        MvcResult resultado = proponer("OBLIGADA", CUERPO, "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"campo\":\"texto\"", "10 a 500 caracteres");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "{}",
        "'{\"docente\":\"\"}'",
        "'{\"docente\":\"abc\"}'",
        "'{\"docente\":\"12345678901234\"}'",
        "'{\"docente\":\"0109 990\"}'"
    })
    void dadoUnDocenteMalEscrito_cuandoPropone_entonces400(String cuerpo) throws Exception {
        MvcResult resultado = proponer("FALLA", cuerpo, "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocenteEnElLimite_cuandoPropone_entoncesLoAcepta() throws Exception {
        assertThat(
                        proponer("2026-I", "{\"docente\":\"1234567890ABC\"}", "TDS", ESCRIBE)
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoUnTextoDe500Caracteres_cuandoPropone_entoncesLoAcepta() throws Exception {
        assertThat(conTexto(500).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoUnTextoDe501Caracteres_cuandoPropone_entonces400() throws Exception {
        assertThat(conTexto(501).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnParaleloDeOtraCarrera_cuandoPropone_entonces403SinDatos() throws Exception {
        MvcResult resultado = proponer("2026-I", CUERPO, "OTRA-01", ESCRIBE);

        assertError(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("POO-204");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoPropone_entonces403QueDiceQuePidaSuCarrera()
            throws Exception {
        assertError(proponer("2026-I", CUERPO, null, ESCRIBE), 403, "COMUN_CARRERA_SIN_INDICAR");
    }

    @Test
    void dadoQuienEscribeSinLeerLaOferta_cuandoProponeEnOtraCarrera_entonces403() throws Exception {
        // El alcance se mira en el permiso de escribir: leer sobre todo no lo amplía.
        assertError(
                proponer("2026-I", CUERPO, "OTRA-01", ESCRIBE, "CATALOGOS:LECTURA"),
                403,
                "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoQuienEscribeSobreTodo_cuandoPropone_entonces200SinCarrera() throws Exception {
        assertThat(proponer("2026-I", CUERPO, null, "OFERTA:ESCRITURA").getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoQuienSoloLee_cuandoPropone_entonces403() throws Exception {
        assertError(
                proponer("FALLA", CUERPO, "TDS", "OFERTA:LECTURA:PROPIO"),
                403,
                "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @ParameterizedTest
    @CsvSource({
        "PARALELO,404,GENERACION_PARALELO_NO_ENCONTRADO",
        "RETIRADO,409,GENERACION_PARALELO_RETIRADO",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "SIN,409,GENERACION_SIN_DISTRIBUTIVO",
        "NO-ACTIVO,409,GENERACION_DOCENTE_NO_ACTIVO",
        "ENVIADO,409,GENERACION_PARCIAL_NO_EDITABLE",
        "FALLA,502,COMUN_PROCESO_CON_ERROR",
        "LENTO,504,COMUN_PROCESO_SIN_RESPUESTA"
    })
    void dadoUnFallo_cuandoPropone_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        assertError(proponer(periodo, CUERPO, "TDS", ESCRIBE), estado, codigo);
    }

    @Test
    void dadaUnaPropuesta_cuandoQuita_entonces204() throws Exception {
        MvcResult resultado = quitar("2026-I", "TDS");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "SIN-PROPUESTA,404,GENERACION_PROPUESTA_NO_ENCONTRADA",
        "ENVIADO,409,GENERACION_PARCIAL_NO_EDITABLE"
    })
    void dadoUnFallo_cuandoQuita_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        assertError(quitar(periodo, "TDS"), estado, codigo);
    }

    @Test
    void dadoUnParaleloDeOtraCarrera_cuandoQuita_entonces403() throws Exception {
        assertError(quitar("2026-I", "OTRA-01"), 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoUnPeriodoConDistributivo_cuandoVeLosParciales_entonces200ConSuAvance()
            throws Exception {
        MvcResult resultado = parciales("2026-I", null, "DISTRIBUTIVO:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .startsWith("{\"creado\":true,\"parciales\":[{\"carrera\":\"AGRO\"")
                .contains(
                        "{\"carrera\":\"TDS\",\"estado\":\"EN_ELABORACION\",\"ofertadas\":2,"
                                + "\"conPropuesta\":1,\"completo\":false,\"propuestas\":[{");
        contrato.validar(resultado);
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoVeLosParciales_entoncesCreadoEnFalso() throws Exception {
        MvcResult resultado = parciales("SIN-CREAR", null, "DISTRIBUTIVO:LECTURA");

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("{\"creado\":false,\"parciales\":[]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinador_cuandoVeLosParciales_entoncesSoloVeElDeSuCarrera() throws Exception {
        assertThat(
                        parciales("2026-I", "TDS", "OFERTA:LECTURA:PROPIO")
                                .getResponse()
                                .getContentAsString())
                .contains("\"TDS\"")
                .doesNotContain("AGRO");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoVeLosParciales_entonces403() throws Exception {
        assertError(
                parciales("2026-I", null, "OFERTA:LECTURA:PROPIO"),
                403,
                "COMUN_CARRERA_SIN_INDICAR");
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoVeLosParciales_entonces404() throws Exception {
        assertError(
                parciales("NADA", null, "DISTRIBUTIVO:LECTURA"),
                404,
                "GENERACION_PERIODO_NO_ENCONTRADO");
    }

    private MvcResult conTexto(int largo) throws Exception {
        return proponer(
                "2026-I",
                "{\"docente\":\"0109990010\",\"texto\":\"" + "a".repeat(largo) + "\"}",
                "TDS",
                ESCRIBE);
    }

    private MvcResult proponer(String periodo, String cuerpo, String carrera, String... permisos)
            throws Exception {
        return mvc.perform(
                        put(RUTA.formatted(periodo))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(token(carrera, permisos)))
                .andReturn();
    }

    private MvcResult quitar(String periodo, String carrera) throws Exception {
        return mvc.perform(delete(RUTA.formatted(periodo)).with(token(carrera, ESCRIBE)))
                .andReturn();
    }

    private MvcResult parciales(String periodo, String carrera, String... permisos)
            throws Exception {
        return mvc.perform(get(PARCIALES.formatted(periodo)).with(token(carrera, permisos)))
                .andReturn();
    }

    private static RequestPostProcessor token(String carrera, String... permisos) {
        return jwt().jwt(
                        token -> {
                            token.subject("coordinacion.demo");
                            token.claim("permisos", List.of(permisos));
                            if (carrera != null) {
                                token.claim("carrera", carrera);
                            }
                        })
                .authorities(new AutoridadesDelToken());
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }
}
