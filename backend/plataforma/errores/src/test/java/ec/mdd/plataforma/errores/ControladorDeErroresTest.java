package ec.mdd.plataforma.errores;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.RespuestaError;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** La página /error propia, para los errores que no pasan por el advice. */
@ExtendWith(OutputCaptureExtension.class)
class ControladorDeErroresTest {

    private final ControladorDeErrores controlador = new ControladorDeErrores();

    @Test
    void dadoUn404_cuandoSeEscribeLaPaginaDeError_entoncesUsaElCodigoComun() {
        ResponseEntity<RespuestaError> respuesta = controlador.error(peticion(404, null));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(404);
        assertThat(respuesta.getBody().codigo()).isEqualTo("COMUN_RECURSO_NO_ENCONTRADO");
    }

    @Test
    void dadoUn500ConExcepcion_cuandoSeEscribeLaPaginaDeError_entoncesRegistraElIncidente(
            CapturedOutput salida) {
        ResponseEntity<RespuestaError> respuesta =
                controlador.error(peticion(500, new IllegalStateException("fallo en un filtro")));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getBody().codigo()).isEqualTo("COMUN_ERROR_INTERNO");
        assertThat(salida.getOut())
                .contains("Incidente " + respuesta.getBody().idIncidente())
                .contains("fallo en un filtro");
    }

    @Test
    void dadoUn403DeLaSeguridad_cuandoSeEscribeLaPaginaDeError_entoncesEsAccesoNoAutorizado() {
        ResponseEntity<RespuestaError> respuesta = controlador.error(peticion(403, null));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(403);
        assertThat(respuesta.getBody().codigo()).isEqualTo("SEGURIDAD_ACCESO_NO_AUTORIZADO");
        assertThat(respuesta.getBody().titulo()).isEqualTo("Acceso no autorizado");
    }

    @Test
    void dadoUnaPeticionSinEstado_cuandoSeEscribeLaPaginaDeError_entoncesEsUn500() {
        assertThat(controlador.error(new MockHttpServletRequest()).getStatusCode().value())
                .isEqualTo(500);
    }

    private static MockHttpServletRequest peticion(int estado, Throwable excepcion) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/error");
        peticion.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, estado);
        peticion.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/prueba");
        if (excepcion != null) {
            peticion.setAttribute(RequestDispatcher.ERROR_EXCEPTION, excepcion);
        }
        return peticion;
    }
}
