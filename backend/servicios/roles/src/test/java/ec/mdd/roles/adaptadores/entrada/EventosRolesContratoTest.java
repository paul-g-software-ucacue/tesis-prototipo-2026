package ec.mdd.roles.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.roles.aplicacion.EventoNoReconocido;
import ec.mdd.roles.puertos.entrada.EventoRecibido;
import ec.mdd.roles.puertos.entrada.RecibirEventos;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code POST /interno/roles/eventos} cumple el contrato de Roles (F05-H03, ADR-018), con la
 * seguridad de los procesos y un doble del caso de uso escrito a mano.
 */
@WebMvcTest(EventosRolesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({EventosRolesContratoTest.Dobles.class, ErroresRoles.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EventosRolesContratoTest {

    private static final List<EventoRecibido> RECIBIDOS = new CopyOnWriteArrayList<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("roles");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        RECIBIDOS.clear();
    }

    @Test
    void dadoUnTokenDeProceso_cuandoAcademicoAvisaDeUnPeriodo_entonces204YLoRecibe()
            throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = entregar(cuerpo(id, "academico.PeriodoActualizado"), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(RECIBIDOS)
                .singleElement()
                .satisfies(
                        evento -> {
                            assertThat(evento.idEvento()).isEqualTo(id);
                            assertThat(evento.origen()).isEqualTo("academico");
                            assertThat(evento.contenido())
                                    .containsEntry("codigo", "2026-2")
                                    .containsEntry("estado", "PLANIFICACION");
                        });
    }

    @Test
    void dadoUnTipoSinManejador_cuandoSeEntrega_entonces400EventoNoReconocido() throws Exception {
        MvcResult resultado =
                entregar(cuerpo(UUID.randomUUID(), "academico.OtroEvento"), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_EVENTO_NO_RECONOCIDO\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoEntregaUnEvento_entonces403SinRecibirlo() throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(UUID.randomUUID(), "academico.PeriodoActualizado"),
                        jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "PERMISO_DISTRIBUTIVO:ESCRITURA")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
        assertThat(RECIBIDOS).isEmpty();
    }

    private MvcResult entregar(String cuerpo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(
                        post("/interno/roles/eventos")
                                .with(token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:datos"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    private static String cuerpo(UUID id, String tipo) {
        return "{\"idEvento\":\""
                + id
                + "\",\"tipo\":\""
                + tipo
                + "\",\"origen\":\"academico\",\"momento\":\"2026-10-21T15:00:00Z\","
                + "\"contenido\":{\"codigo\":\"2026-2\",\"estado\":\"PLANIFICACION\"}}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        RecibirEventos recibirEventos() {
            return evento -> {
                if (!"academico.PeriodoActualizado".equals(evento.tipo())) {
                    throw new EventoNoReconocido(evento.tipo());
                }
                RECIBIDOS.add(evento);
            };
        }
    }
}
