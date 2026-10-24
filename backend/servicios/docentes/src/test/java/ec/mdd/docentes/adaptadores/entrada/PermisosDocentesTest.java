package ec.mdd.docentes.adaptadores.entrada;

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
 * La prueba de autorización por operación de Docentes (RNF-SEG-03): sin su permiso, cada una de las
 * 6 operaciones responde 403. Las leen quienes tienen «Docentes», «Distributivo del periodo» o
 * «Catálogos y datos maestros» de lectura, y las escriben quienes tienen esos mismos de escritura
 * (D-70, aclaración 1 de F03-H04).
 */
@WebMvcTest(DocentesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({DocentesContratoTest.Dobles.class, ErroresDocentes.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosDocentesTest {

    private static final String LECTURA =
            "([DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA])";
    private static final String ESCRITURA =
            "([DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA])";

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("docentes");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeDocentes_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/docentes/docentes " + LECTURA,
                        "POST /api/docentes/docentes " + ESCRITURA,
                        "GET /api/docentes/docentes/0109990010 " + LECTURA,
                        "PUT /api/docentes/docentes/0109990010 " + ESCRITURA,
                        "PUT /api/docentes/docentes/0109990010/estado " + ESCRITURA,
                        "GET /api/docentes/referencias " + LECTURA);

        verificador.verificar(mvc, conversor);
    }
}
