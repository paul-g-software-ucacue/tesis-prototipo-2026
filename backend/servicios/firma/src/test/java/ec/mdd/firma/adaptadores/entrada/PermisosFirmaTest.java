package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.VerificadorDePermisos;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cada operación de Firma exige el permiso de su {@code x-mdd-permiso} (ADR-021): sin él responde
 * 403, y con él pasa (F13-H02).
 */
@WebMvcTest(DocumentoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({DocumentoContratoTest.Dobles.class, ErroresFirma.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosFirmaTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("firma");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContrato_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .hasSize(2)
                .anySatisfy(operacion -> assertThat(operacion).contains("CONSOLIDACION:LECTURA"))
                .anySatisfy(
                        operacion -> assertThat(operacion).contains("CONSOLIDACION:APROBACION"));

        verificador.verificar(mvc, conversor);
    }
}
