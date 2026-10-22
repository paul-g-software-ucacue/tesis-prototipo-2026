package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link VerificadorDePermisos} une el contrato y el código (RNF-SEG-03, ADR-021): acepta una
 * operación cuyo {@code @PreAuthorize} exige el permiso de su {@code x-mdd-permiso}, e informa una
 * sin anotación o con otro permiso.
 */
@WebMvcTest(AplicacionDePermisos.Registros.class)
@ImportAutoConfiguration(SeguridadProcesoAutoConfiguracion.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class VerificadorDePermisosTest {

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoUnContrato_cuandoSeLee_entoncesListaSoloLasOperacionesConPermiso() {
        VerificadorDePermisos verificador = verificador("permisos-correctos.yaml");

        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/prueba/registros (PRUEBA:LECTURA)",
                        "POST /api/prueba/registros (PRUEBA:ESCRITURA)",
                        "DELETE /api/prueba/registros/REGISTRO_UNO (PRUEBA:ESCRITURA)");
    }

    @Test
    void dadoElCodigoQueCumpleElContrato_cuandoSeVerifica_entoncesPasa() {
        assertThatCode(() -> verificador("permisos-correctos.yaml").verificar(mvc, conversor))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoUnaOperacionSinAnotacionYOtraConOtroPermiso_cuandoSeVerifica_entoncesInformaLasDos() {
        assertThatThrownBy(() -> verificador("permisos-con-errores.yaml").verificar(mvc, conversor))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("GET /api/prueba/olvidada (PRUEBA:LECTURA)")
                .hasMessageContaining("GET /api/prueba/equivocada (PRUEBA:LECTURA)")
                .hasMessageNotContaining("/api/prueba/registros");
    }

    @Test
    void dadoUnaOperacionConPermisosAlternativos_cuandoSeLee_entoncesLosTraeTodos() {
        assertThat(verificador("permisos-alternativos.yaml").operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/prueba/alternativa ([PRUEBA:LECTURA, OTRA:LECTURA])",
                        "POST /api/prueba/registros (PRUEBA:ESCRITURA)");
    }

    @Test
    void dadoElCodigoQueAceptaCualquieraDeLosPermisos_cuandoSeVerifica_entoncesPasa() {
        assertThatCode(() -> verificador("permisos-alternativos.yaml").verificar(mvc, conversor))
                .doesNotThrowAnyException();
    }

    @Test
    void dadoElCodigoQueSoloAceptaUnoDeLosPermisos_cuandoSeVerifica_entoncesInformaElOtro() {
        assertThatThrownBy(
                        () ->
                                verificador("permisos-alternativos-con-errores.yaml")
                                        .verificar(mvc, conversor))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("GET /api/prueba/solo-una")
                .hasMessageContaining("con OTRA:LECTURA respondió 403");
    }

    @Test
    void dadoUnContratoSinOperacionesProtegidas_cuandoSeVerifica_entoncesFalla() {
        assertThatThrownBy(
                        () ->
                                verificador("permisos-sin-operaciones.yaml")
                                        .verificar(mvc, conversor))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("x-mdd-permiso");
    }

    private static VerificadorDePermisos verificador(String archivo) {
        return VerificadorDePermisos.desdeArchivo(Path.of("src/test/resources", archivo));
    }
}
