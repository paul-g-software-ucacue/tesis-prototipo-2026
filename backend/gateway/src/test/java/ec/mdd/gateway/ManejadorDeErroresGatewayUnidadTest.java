package ec.mdd.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** El manejador de errores del API Gateway, sin servidor. */
@ExtendWith(OutputCaptureExtension.class)
class ManejadorDeErroresGatewayUnidadTest {

    private final ManejadorDeErroresGateway manejador =
            new ManejadorDeErroresGateway(JsonMapper.builder().build());

    @Test
    void dadoUnaExcepcionCualquiera_cuandoSeManeja_entonces500ConIncidenteYRegistroSaneado(
            CapturedOutput salida) {
        MockServerWebExchange intercambio =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/cargas/sesion"));

        manejador
                .handle(intercambio, new IllegalStateException("fallo con Bearer abc.def.ghi"))
                .block();

        assertThat(intercambio.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        String cuerpo = intercambio.getResponse().getBodyAsString().block();
        assertThat(cuerpo)
                .contains("\"codigo\":\"COMUN_ERROR_INTERNO\"")
                .contains("\"idIncidente\":");
        assertThat(cuerpo).doesNotContain("IllegalStateException");
        assertThat(salida.getOut())
                .contains("Incidente ")
                .contains("[oculto]")
                .doesNotContain("abc.def.ghi");
    }

    @Test
    void dadoUnMetodoNoPermitido_cuandoSeManeja_entonces405ConSuCodigo() {
        MockServerWebExchange intercambio =
                MockServerWebExchange.from(MockServerHttpRequest.delete("/api/cargas/sesion"));

        manejador
                .handle(intercambio, new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED))
                .block();

        assertThat(intercambio.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(intercambio.getResponse().getBodyAsString().block())
                .contains("COMUN_METODO_NO_PERMITIDO");
    }
}
