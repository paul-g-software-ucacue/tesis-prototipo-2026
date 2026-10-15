package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class ValidadorDeContratoTest {

    private final ValidadorDeContrato validador =
            ValidadorDeContrato.desdeArchivo(Path.of("src/test/resources/contrato-de-prueba.yaml"));

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Saludo()).build();

    @Test
    void dadoUnaRespuestaQueCumpleElContrato_cuandoSeValida_entoncesPasa() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/prueba/saludo")).andReturn();

        assertThatCode(() -> validador.validar(resultado)).doesNotThrowAnyException();
    }

    @Test
    void dadoUnaRespuestaSinUnCampoObligatorio_cuandoSeValida_entoncesFallaConElInforme()
            throws Exception {
        MvcResult resultado =
                mvc.perform(get("/api/prueba/saludo").param("bien", "no")).andReturn();

        assertThatThrownBy(() -> validador.validar(resultado))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("mensaje");
    }

    @Test
    void dadoElNombreDeUnServicio_cuandoSeBuscaSuContrato_entoncesLoEncuentraEnContratos() {
        assertThatCode(() -> ValidadorDeContrato.para("usuarios")).doesNotThrowAnyException();
    }

    @Test
    void dadoUnCuerpoDePeticion_cuandoSeValidaContraElContrato_entoncesPasaOFallaConElInforme() {
        ValidadorDeContrato usuarios = ValidadorDeContrato.para("usuarios");
        String valido = "{\"proceso\":\"distributivo\",\"clave\":\"x\"}";

        assertThatCode(
                        () ->
                                usuarios.validarPeticion(
                                        "POST", "/interno/usuarios/token-proceso", valido))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                usuarios.validarPeticion(
                                        "POST", "/interno/usuarios/token-proceso", "{}"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("proceso");
    }

    @RestController
    static class Saludo {

        @GetMapping("/api/prueba/saludo")
        ResponseEntity<String> saludar(@RequestParam(defaultValue = "si") String bien) {
            String cuerpo = "si".equals(bien) ? "{\"mensaje\":\"hola\"}" : "{\"otro\":1}";
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cuerpo);
        }
    }
}
