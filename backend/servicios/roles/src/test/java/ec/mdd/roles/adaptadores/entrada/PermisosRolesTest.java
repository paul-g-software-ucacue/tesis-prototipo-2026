package ec.mdd.roles.adaptadores.entrada;

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
 * La prueba de autorización por operación de Roles (RNF-SEG-03, SC-003): cada operación con {@code
 * x-mdd-permiso} responde 403 sin ese permiso y otra cosa con él.
 */
@WebMvcTest({RolesControlador.class, ResponsablesControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({RolesContratoTest.Dobles.class, ResponsablesContratoTest.Dobles.class, ErroresRoles.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosRolesTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("roles");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeRoles_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        // 8 de F05-H02 y 3 del Responsable de consolidación (F05-H03, D-69).
        assertThat(verificador.operaciones()).hasSize(13);
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .contains(
                        "GET /api/roles/responsables (DISTRIBUTIVO:LECTURA)",
                        "PUT /api/roles/responsables/2026-2 (DISTRIBUTIVO:ESCRITURA)",
                        "GET /api/roles/docentes (DISTRIBUTIVO:ESCRITURA)");

        verificador.verificar(mvc, conversor);
    }
}
