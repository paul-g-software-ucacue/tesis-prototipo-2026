package ec.mdd.plataforma.pruebas;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Valida una respuesta de MockMvc contra el contrato OpenAPI de un servicio (principios IV y VIII).
 *
 * <p>Usa el núcleo de openapi-request-validator con un adaptador propio, porque su módulo para
 * MockMvc depende de {@code javax.servlet} y no funciona con Spring 7 (ADR-015). Solo valida la
 * respuesta: así una petición sin token, que no cumple el esquema de seguridad, sigue sirviendo
 * para comprobar que el 401 cumple el contrato.
 */
public final class ValidadorDeContrato {

    private final OpenApiInteractionValidator validador;

    private ValidadorDeContrato(Path contrato) {
        this.validador =
                OpenApiInteractionValidator.createForSpecificationUrl(contrato.toUri().toString())
                        .build();
    }

    /**
     * Validador para el contrato de un servicio.
     *
     * @param servicio nombre del servicio, por ejemplo {@code usuarios}
     * @return el validador de {@code contratos/<servicio>/openapi.yaml}
     * @throws IllegalStateException si no encuentra la carpeta {@code contratos/}
     */
    public static ValidadorDeContrato para(String servicio) {
        return desdeArchivo(buscarContratos().resolve(servicio).resolve("openapi.yaml"));
    }

    /** Validador para un archivo de contrato cualquiera. */
    public static ValidadorDeContrato desdeArchivo(Path contrato) {
        return new ValidadorDeContrato(contrato);
    }

    /**
     * Comprueba que la respuesta cumple el contrato para la operación de la petición.
     *
     * @param resultado resultado de MockMvc
     * @throws AssertionError con el informe del validador si la respuesta no cumple el contrato
     */
    public void validar(MvcResult resultado) {
        MockHttpServletRequest peticion = resultado.getRequest();
        MockHttpServletResponse respuesta = resultado.getResponse();
        SimpleResponse.Builder simple = SimpleResponse.Builder.status(respuesta.getStatus());
        if (respuesta.getContentType() != null) {
            simple.withContentType(respuesta.getContentType());
        }
        for (String nombre : respuesta.getHeaderNames()) {
            simple.withHeader(nombre, respuesta.getHeaders(nombre));
        }
        try {
            simple.withBody(respuesta.getContentAsString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo leer la respuesta", e);
        }
        ValidationReport informe =
                validador.validateResponse(
                        peticion.getRequestURI(),
                        Request.Method.valueOf(peticion.getMethod().toUpperCase(Locale.ROOT)),
                        simple.build());
        if (informe.hasErrors()) {
            throw new AssertionError(
                    "La respuesta no cumple el contrato:\n"
                            + SimpleValidationReportFormat.getInstance().apply(informe));
        }
    }

    // Busca contratos/ subiendo desde el directorio de trabajo de la prueba (el del módulo).
    private static Path buscarContratos() {
        Path directorio = Path.of("").toAbsolutePath();
        while (directorio != null) {
            Path contratos = directorio.resolve("contratos");
            if (Files.isDirectory(contratos.resolve("comun"))) {
                return contratos;
            }
            directorio = directorio.getParent();
        }
        throw new IllegalStateException("No se encontró la carpeta contratos/");
    }
}
