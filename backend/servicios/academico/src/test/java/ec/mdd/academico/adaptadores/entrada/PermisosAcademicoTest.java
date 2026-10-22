package ec.mdd.academico.adaptadores.entrada;

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
 * La prueba de autorización por operación de Académico (RNF-SEG-03): sin su permiso, cada una de
 * las doce operaciones responde 403. Los periodos exigen «Distributivo del periodo» (D-69); la
 * estructura orgánica se lee con «Distributivo del periodo: lectura» o «Catálogos y datos maestros:
 * lectura», y se mantiene con «Catálogos y datos maestros: escritura» (D-70).
 */
@WebMvcTest({AcademicoControlador.class, EstructuraControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    AcademicoContratoTest.Dobles.class,
    EstructuraContratoTest.Dobles.class,
    ErroresAcademico.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosAcademicoTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeAcademico_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/academico/periodos (DISTRIBUTIVO:LECTURA)",
                        "POST /api/academico/periodos (DISTRIBUTIVO:ESCRITURA)",
                        "GET /api/academico/periodos/2026-2 (DISTRIBUTIVO:LECTURA)",
                        "PUT /api/academico/periodos/2026-2 (DISTRIBUTIVO:ESCRITURA)",
                        "PUT /api/academico/periodos/2026-2/normativa (DISTRIBUTIVO:ESCRITURA)",
                        "PUT /api/academico/periodos/2026-2/estado (DISTRIBUTIVO:ESCRITURA)",
                        "GET /api/academico/gestiones-autorizadas"
                                + " ([DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA])",
                        "GET /api/academico/unidades ([DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA])",
                        "POST /api/academico/unidades (CATALOGOS:ESCRITURA)",
                        "GET /api/academico/unidades/MATRIZ"
                                + " ([DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA])",
                        "PUT /api/academico/unidades/MATRIZ (CATALOGOS:ESCRITURA)",
                        "PUT /api/academico/unidades/MATRIZ/estado (CATALOGOS:ESCRITURA)");

        verificador.verificar(mvc, conversor);
    }
}
