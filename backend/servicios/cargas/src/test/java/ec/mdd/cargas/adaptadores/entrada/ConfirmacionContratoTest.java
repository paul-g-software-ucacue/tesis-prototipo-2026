package ec.mdd.cargas.adaptadores.entrada;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.cargas.adaptadores.salida.DatosDePrueba;
import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * La vista previa, la confirmación y el historial cumplen {@code contratos/cargas/openapi.yaml}
 * (principio IV, F01-H04), con un dueño y un historial en memoria y los datos de prueba de la ERS.
 */
@WebMvcTest({SubidaControlador.class, HistorialControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class, ReferenciasDePrueba.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ConfirmacionContratoTest {

    private static final String CARRERAS = "02_carreras.csv";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;
    @Autowired private AplicadorDePrueba dueno;
    @Autowired private HistorialEnMemoria historial;

    @BeforeEach
    void reiniciar() {
        dueno.reiniciar();
        historial.vaciar();
    }

    private MvcResult subir(byte[] contenido) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion =
                multipart("/api/cargas/plantillas/carreras/archivo");
        peticion.file(new MockMultipartFile("archivo", CARRERAS, "text/csv", contenido));
        return mvc.perform(peticion.with(SubidaContratoTest.escritura())).andReturn();
    }

    private MvcResult confirmar(byte[] contenido, String huella) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion =
                multipart("/api/cargas/plantillas/carreras/archivo/confirmacion");
        peticion.file(new MockMultipartFile("archivo", CARRERAS, "text/csv", contenido));
        peticion.param("huella", huella);
        return mvc.perform(peticion.with(SubidaContratoTest.escritura())).andReturn();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(UTF_8);
    }

    private String huella(byte[] contenido) throws Exception {
        return JsonPath.read(cuerpo(subir(contenido)), "$.vistaPrevia.huella");
    }

    @Test
    void dadoUnArchivoSinErrores_cuandoSeSube_entoncesLaVistaPreviaConSusTotalesYSuHuella()
            throws Exception {
        MvcResult resultado = subir(DatosDePrueba.contenido(CARRERAS));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.vistaPrevia"))
                .containsEntry("filasConErrores", 0)
                .containsEntry("nuevos", 7)
                .containsEntry("cambian", 0)
                .containsEntry("bajas", 0)
                .containsEntry("iguales", 0);
        assertThat(JsonPath.<List<Object>>read(cuerpo(resultado), "$.vistaPrevia.registros"))
                .hasSize(7);
        contrato.validar(resultado);
    }

    @Test
    void dadaLaVistaPrevia_cuandoSeConfirmaDosVeces_entoncesAplicadaYDespuesRepetida()
            throws Exception {
        byte[] carreras = DatosDePrueba.contenido(CARRERAS);
        String huella = huella(carreras);

        MvcResult aplicada = confirmar(carreras, huella);
        MvcResult repetida = confirmar(carreras, huella);

        assertThat(aplicada.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(aplicada), "$"))
                .containsEntry("resultado", "APLICADA")
                .containsEntry("nuevos", 7)
                .containsEntry("cambiados", 0)
                .containsEntry("bajasSinAplicar", 0);
        contrato.validar(aplicada);
        assertThat(JsonPath.<String>read(cuerpo(repetida), "$.resultado")).isEqualTo("REPETIDA");
        assertThat(JsonPath.<String>read(cuerpo(repetida), "$.confirmadaEl"))
                .isEqualTo(JsonPath.<String>read(cuerpo(aplicada), "$.confirmadaEl"));
        contrato.validar(repetida);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(subir(carreras)), "$.vistaPrevia"))
                .as("subirla otra vez, ya aplicada")
                .containsEntry("nuevos", 0)
                .containsEntry("iguales", 7);

        MvcResult lista =
                mvc.perform(
                                get("/api/cargas/plantillas/carreras/historial")
                                        .with(SubidaContratoTest.escritura()))
                        .andReturn();
        assertThat(JsonPath.<List<String>>read(cuerpo(lista), "$[*].resultado"))
                .containsExactly("REPETIDA", "APLICADA");
        contrato.validar(lista);
    }

    @Test
    void dadoUnArchivoConErrores_cuandoSeConfirma_entonces409ConErrores() throws Exception {
        String csv = new String(DatosDePrueba.contenido(CARRERAS), UTF_8);
        byte[] conError = csv.replaceFirst("CONTABILIDAD", "Contabilidad").getBytes(UTF_8);

        MvcResult resultado = confirmar(conError, "0".repeat(64));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_CARGA_CON_ERRORES");
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.detalles[0].descripcion"))
                .isEqualTo("1 fila tiene errores.");
        contrato.validar(resultado);
        assertThat(historial.deLaCarga("carreras"))
                .singleElement()
                .satisfies(entrada -> assertThat(entrada.motivo()).isPresent());
    }

    @Test
    void dadaOtraHuella_cuandoSeConfirma_entonces409DesactualizadaSinAplicar() throws Exception {
        byte[] carreras = DatosDePrueba.contenido(CARRERAS);

        MvcResult resultado = confirmar(carreras, "f".repeat(64));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_VISTA_PREVIA_DESACTUALIZADA");
        contrato.validar(resultado);
        assertThat(JsonPath.<Integer>read(cuerpo(subir(carreras)), "$.vistaPrevia.nuevos"))
                .isEqualTo(7);
    }

    @Test
    void dadaUnaReglaDelDuenoQueFalla_cuandoSeConfirma_entonces409RechazadaConLaFila()
            throws Exception {
        byte[] carreras = DatosDePrueba.contenido(CARRERAS);
        String huella = huella(carreras);
        dueno.rechazarCon(new ArchivoSinRegistros());

        MvcResult resultado = confirmar(carreras, huella);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo"))
                .isEqualTo("CARGAS_CARGA_RECHAZADA");
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$.detalles[0]"))
                .containsEntry("campo", "fila 2")
                .containsEntry(
                        "descripcion",
                        "Archivo sin registros: El archivo no tiene registros debajo de los"
                                + " encabezados.");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaHuellaQueNoEsHexadecimal_cuandoSeConfirma_entonces400() throws Exception {
        MvcResult resultado = confirmar(DatosDePrueba.contenido(CARRERAS), "no-es-una-huella");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaCargaQueNoExiste_cuandoSePideSuHistorial_entonces404() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/cargas/plantillas/comisiones/historial")
                                        .with(SubidaContratoTest.escritura()))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }
}
