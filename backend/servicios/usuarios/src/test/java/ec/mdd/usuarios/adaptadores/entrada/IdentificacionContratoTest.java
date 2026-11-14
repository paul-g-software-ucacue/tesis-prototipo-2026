package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.ConsultarIdentificacionServicio;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.ConsultarIdentificacion;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * La identificación de una cuenta, solo entre procesos (F14-H01, D-102): 200 con la identificación,
 * el mismo 404 sin cuenta y sin identificación, y 403 con un token de usuario.
 */
@WebMvcTest(IdentificacionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({IdentificacionContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class IdentificacionContratoTest {

    private static final Map<String, Usuario> CUENTAS =
            Map.of(
                    "vicerrectorado.demo",
                    new Usuario(
                            "vicerrectorado.demo",
                            "Vicerrectorado",
                            "hash",
                            true,
                            Optional.of("0109990028")),
                    "docente.demo",
                    new Usuario("docente.demo", "Docente", "hash", true));

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @Test
    void dadaUnaCuentaConIdentificacion_cuandoLaPideUnProceso_entonces200ConLaIdentificacion()
            throws Exception {
        MvcResult resultado = pedir("Vicerrectorado.Demo", deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("{\"identificacion\":\"0109990028\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaCuentaSinIdentificacionOInexistente_cuandoLaPideUnProceso_entoncesElMismo404()
            throws Exception {
        for (String usuario : List.of("docente.demo", "nadie.demo")) {
            MvcResult resultado = pedir(usuario, deProceso());

            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"USUARIOS_SIN_IDENTIFICACION\"");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoPideUnaIdentificacion_entonces403() throws Exception {
        MvcResult resultado =
                pedir(
                        "vicerrectorado.demo",
                        jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                                .authorities(new SimpleGrantedAuthority("ROLE_VICERRECTOR")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("0109990028");
    }

    private MvcResult pedir(String usuario, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get("/interno/usuarios/{usuario}/identificacion", usuario).with(token))
                .andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:distributivo"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarIdentificacion consultarIdentificacion() {
            return new ConsultarIdentificacionServicio(
                    new RepositorioUsuarios() {
                        @Override
                        public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
                            return Optional.ofNullable(
                                    CUENTAS.get(Usuario.normalizar(nombreUsuario)));
                        }

                        @Override
                        public List<Usuario> todos() {
                            return List.copyOf(CUENTAS.values());
                        }

                        @Override
                        public void guardar(Usuario usuario) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public void actualizar(Usuario usuario) {
                            throw new UnsupportedOperationException();
                        }
                    });
        }
    }
}
