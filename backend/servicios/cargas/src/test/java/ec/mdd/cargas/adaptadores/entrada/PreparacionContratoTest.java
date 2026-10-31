package ec.mdd.cargas.adaptadores.entrada;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El tablero de preparación cumple {@code contratos/cargas/openapi.yaml} (principio IV, F02-H02),
 * con un dueño, un historial y los proyectos asignables en memoria, y los periodos de prueba:
 * 2026-I abierto y 2026-II sin abrir.
 */
@WebMvcTest(PreparacionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class, ReferenciasDePrueba.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PreparacionContratoTest {

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
        asignables.con("2026-I", AsignablesDelPeriodo.ALGUNO);
    }

    private MvcResult tablero(String consulta) throws Exception {
        return mvc.perform(
                        get("/api/cargas/preparacion" + consulta)
                                .with(SubidaContratoTest.escritura()))
                .andReturn();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(UTF_8);
    }

    @Test
    void sinPeriodo_esElQueEmpiezaMasTarde_conLaListaParaElegir() throws Exception {
        MvcResult resultado = tablero("");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(cuerpo(resultado), "$.periodos[*].codigo"))
                .containsExactly("2026-II", "2026-I");
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.periodo")).isEqualTo("2026-II");
        assertThat(JsonPath.<Boolean>read(cuerpo(resultado), "$.listo")).isFalse();
        assertThat(JsonPath.<List<String>>read(cuerpo(resultado), "$.faltan[*].tipo"))
                .as("2026-II no tiene proyectos asignables ni está abierto")
                .containsExactly("CARGA", "PERIODO_SIN_ABRIR");
        contrato.validar(resultado);
    }

    @Test
    void conTodoYElPeriodoAbierto_estaListo() throws Exception {
        MvcResult resultado = tablero("?periodo=2026-I");

        assertThat(JsonPath.<Boolean>read(cuerpo(resultado), "$.listo")).isTrue();
        assertThat(JsonPath.<List<Object>>read(cuerpo(resultado), "$.faltan")).isEmpty();
        assertThat(JsonPath.<List<String>>read(cuerpo(resultado), "$.cargas[*].estado"))
                .containsOnly("CARGADA")
                .hasSize(10);
        contrato.validar(resultado);
    }

    @Test
    void conDocentesPendiente_noEstaListoYDiceQueFaltaDocentes() throws Exception {
        dueno.sinDatosDe("docentes");

        MvcResult resultado = tablero("?periodo=2026-I");

        assertThat(JsonPath.<Boolean>read(cuerpo(resultado), "$.listo")).isFalse();
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.faltan[0]"))
                .containsEntry("tipo", "CARGA")
                .containsEntry("estado", "PENDIENTE");
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.faltan[0].carga.codigo"))
                .isEqualTo("docentes");
        contrato.validar(resultado);
    }

    @Test
    void unPeriodoQueNoExiste_es400ConElDetalleDelPeriodo() throws Exception {
        MvcResult resultado = tablero("?periodo=2027-I");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_PERIODO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    /** Válidos e inválidos del periodo (principio XIV). */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"2026/I", "año-1", "123456789012345678901", "2026-I'"})
    void unPeriodoQueNoCumpleSuFormato_es400SolicitudInvalida(String invalido) throws Exception {
        MvcResult resultado = tablero("?periodo=" + invalido);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("COMUN_SOLICITUD_INVALIDA");
    }

    @Test
    void losDosPeriodosDePrueba_sonValidos() throws Exception {
        assertThat(tablero("?periodo=2026-I").getResponse().getStatus()).isEqualTo(200);
        assertThat(tablero("?periodo=2026-II").getResponse().getStatus()).isEqualTo(200);
    }
}
