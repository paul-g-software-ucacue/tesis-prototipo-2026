package ec.mdd.usuarios.adaptadores.entrada;

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
 * La prueba de autorización por operación de Usuarios (RNF-SEG-03, SC-003): {@code listarUsuarios}
 * responde 403 sin «Usuarios y roles: lectura», y las dos operaciones de la bitácora sin «Bitácora:
 * lectura» (F17-H02); otra cosa con su permiso.
 */
@WebMvcTest({UsuariosControlador.class, BitacoraControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    UsuariosContratoTest.Dobles.class,
    BitacoraContratoTest.Dobles.class,
    ErroresUsuarios.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosUsuariosTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("usuarios");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeUsuarios_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/usuarios/usuarios (USUARIOS_ROLES:LECTURA)",
                        "GET /api/usuarios/bitacora (BITACORA:LECTURA)",
                        "GET /api/usuarios/bitacora/acciones (BITACORA:LECTURA)");

        verificador.verificar(mvc, conversor);
    }
}
