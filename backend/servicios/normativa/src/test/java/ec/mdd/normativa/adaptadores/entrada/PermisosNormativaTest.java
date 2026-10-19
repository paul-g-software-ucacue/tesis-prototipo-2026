package ec.mdd.normativa.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
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
 * La prueba de autorización por operación de Normativa (RNF-SEG-03, FR-008): sin su permiso de
 * «Parámetros y normativa», cada una de las seis operaciones responde 403.
 */
@WebMvcTest(NormativaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({NormativaContratoTest.Dobles.class, ErroresNormativa.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosNormativaTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("normativa");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeNormativa_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/normativa/versiones (PARAMETROS:LECTURA)",
                        "POST /api/normativa/versiones (PARAMETROS:ESCRITURA)",
                        "GET /api/normativa/versiones/1 (PARAMETROS:LECTURA)",
                        "PUT /api/normativa/versiones/1 (PARAMETROS:ESCRITURA)",
                        "GET /api/normativa/periodos/2025-1 (PARAMETROS:LECTURA)",
                        "PUT /api/normativa/periodos/2025-1 (PARAMETROS:ESCRITURA)");

        verificador.verificar(mvc, conversor);
    }
}
