package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code GET /api/usuarios/usuarios} cumple el contrato de Usuarios: usuario, nombre y si está
 * activo, ordenados, sin contraseñas ni hashes (F05-H02, research §10).
 */
@WebMvcTest(UsuariosControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({UsuariosContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class UsuariosContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElAdministrador_cuandoListaLosUsuarios_entonces200SinHashesSegunElContrato()
            throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/usuarios/usuarios")
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "PERMISO_USUARIOS_ROLES:LECTURA"))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"usuario\":\"admin.demo\",\"nombre\":\"Administración (demo)\","
                                + "\"activo\":true},{\"usuario\":\"autor.prueba\","
                                + "\"nombre\":\"Autor de prueba\",\"activo\":false}]")
                .doesNotContain("hash")
                .doesNotContain("clave");
        contrato.validar(resultado);
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        ListarUsuarios listarUsuarios() {
            return () ->
                    List.of(
                            new ResumenDeUsuario("admin.demo", "Administración (demo)", true),
                            new ResumenDeUsuario("autor.prueba", "Autor de prueba", false));
        }
    }
}
