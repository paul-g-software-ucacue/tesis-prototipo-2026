package ec.mdd.cargas.adaptadores.entrada;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.docentes.puertos.entrada.AsignablesDelPeriodo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El estado de las cargas y la confirmación «sin proyectos» cumplen {@code
 * contratos/cargas/openapi.yaml} (principio IV, F01-H05), con un dueño, un historial y los
 * proyectos asignables en memoria.
 */
@WebMvcTest({EstadoControlador.class, HistorialControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class, ReferenciasDePrueba.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EstadoContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;
    @Autowired private AplicadorDePrueba dueno;
    @Autowired private HistorialEnMemoria historial;
    @Autowired private AsignablesDePrueba asignables;

    @BeforeEach
    void reiniciar() {
        dueno.reiniciar();
        historial.vaciar();
        asignables.reiniciar();
    }

    private MvcResult estado() throws Exception {
        return mvc.perform(get("/api/cargas/estado").with(SubidaContratoTest.escritura()))
                .andReturn();
    }

    private MvcResult sinProyectos(String cuerpo) throws Exception {
        return mvc.perform(
                        post("/api/cargas/sin-proyectos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(SubidaContratoTest.escritura()))
                .andReturn();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(UTF_8);
    }

    @Test
    void elEstado_traeLasDiezEnSuOrdenConLoQueLesFalta() throws Exception {
        dueno.sinDatosDe("carreras", "asignaturas_malla", "proyectos_practicas");

        MvcResult resultado = estado();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<List<Integer>>read(cuerpo(resultado), "$.cargas[*].numero"))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.cargas[3]"))
                .containsEntry("codigo", "asignaturas_malla")
                .containsEntry("estado", "PENDIENTE");
        assertThat(
                        JsonPath.<List<Map<String, Object>>>read(
                                cuerpo(resultado), "$.cargas[3].faltan"))
                .containsExactly(Map.of("numero", 2, "codigo", "carreras", "nombre", "Carreras"));
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.cargas[8].estado"))
                .isEqualTo("PENDIENTE");
        contrato.validar(resultado);
    }

    @Test
    void sinProyectosDeUnPeriodoSinAsignables_es201_laSegundaVez200_yLaCarga9QuedaSinProyectos()
            throws Exception {
        dueno.sinDatosDe("proyectos_practicas");

        MvcResult primera = sinProyectos("{\"periodo\": \"2026-II\"}");
        MvcResult segunda = sinProyectos("{\"periodo\": \"2026-II\"}");

        assertThat(primera.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(primera), "$"))
                .containsEntry("resultado", "SIN_PROYECTOS")
                .containsEntry("periodo", "2026-II")
                .containsEntry("usuario", "consolidacion.demo")
                .doesNotContainKeys("archivo", "hash");
        contrato.validar(primera);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(segunda);
        assertThat(historial.deLaCarga("proyectos_practicas")).hasSize(1);
        MvcResult resultado = estado();
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.cargas[8]"))
                .containsEntry("estado", "SIN_PROYECTOS")
                .containsEntry("periodosSinProyectos", List.of("2026-II"));
        MvcResult delHistorial =
                mvc.perform(
                                get("/api/cargas/plantillas/proyectos_practicas/historial")
                                        .with(SubidaContratoTest.escritura()))
                        .andReturn();
        assertThat(JsonPath.<String>read(cuerpo(delHistorial), "$[0].periodo"))
                .isEqualTo("2026-II");
        contrato.validar(delHistorial);
    }

    @Test
    void sinProyectosDeUnPeriodoConAsignables_es409SinGuardar() throws Exception {
        asignables.con("2026-II", AsignablesDelPeriodo.ALGUNO);

        MvcResult resultado = sinProyectos("{\"periodo\": \"2026-II\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_PERIODO_CON_PROYECTOS");
        contrato.validar(resultado);
        assertThat(historial.deLaCarga("proyectos_practicas")).isEmpty();
    }

    @Test
    void sinProyectosDeUnPeriodoQueNoExiste_es400ConElDetalleDelPeriodo() throws Exception {
        MvcResult resultado = sinProyectos("{\"periodo\": \"2027-I\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_PERIODO_NO_ENCONTRADO");
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.detalles[0].campo"))
                .isEqualTo("periodo");
        contrato.validar(resultado);
        assertThat(historial.deLaCarga("proyectos_practicas")).isEmpty();
    }

    /** Válidos e inválidos del periodo (principio XIV): el contrato rechaza los que no cumplen. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "{}",
                "{\"periodo\": \"\"}",
                "{\"periodo\": \"2026-III\"}",
                "{\"periodo\": \"26-I\"}",
                "{\"periodo\": \"2026-i\"}",
                "{\"periodo\": \"20260-IIII\"}"
            })
    void unPeriodoQueNoCumpleSuFormato_es400SolicitudInvalidaSinGuardar(String invalido)
            throws Exception {
        MvcResult resultado = sinProyectos(invalido);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(historial.deLaCarga("proyectos_practicas")).isEmpty();
    }

    @Test
    void losDosPeriodosDePrueba_sonValidos() throws Exception {
        assertThat(sinProyectos("{\"periodo\": \"2026-I\"}").getResponse().getStatus())
                .isEqualTo(201);
        assertThat(sinProyectos("{\"periodo\": \"2026-II\"}").getResponse().getStatus())
                .isEqualTo(201);
    }
}
