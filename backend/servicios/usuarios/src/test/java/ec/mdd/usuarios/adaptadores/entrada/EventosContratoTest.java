package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.EventoNoReconocido;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
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
 * {@code POST /interno/usuarios/eventos} cumple el contrato de Usuarios (D-47, ADR-018), con la
 * seguridad de los procesos y un doble del caso de uso escrito a mano.
 */
@WebMvcTest(EventosControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({EventosContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EventosContratoTest {

    private static final List<EventoRecibido> REGISTRADOS = new CopyOnWriteArrayList<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        REGISTRADOS.clear();
    }

    @Test
    void dadoUnTokenDeProceso_cuandoEntregaUnEvento_entonces204YLoRegistra() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = entregar(cuerpo(id, "generacion.EventoDePrueba"), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(REGISTRADOS)
                .singleElement()
                .satisfies(
                        evento -> {
                            assertThat(evento.idEvento()).isEqualTo(id);
                            assertThat(evento.tipo()).isEqualTo("generacion.EventoDePrueba");
                            assertThat(evento.origen()).isEqualTo("generacion");
                            assertThat(evento.contenido())
                                    .containsEntry("nota", "evento de prueba B00-H05");
                        });
    }

    @Test
    void dadoUnEventoRepetido_cuandoLlegaOtraVez_entoncesTambien204() throws Exception {
        UUID id = UUID.randomUUID();
        entregar(cuerpo(id, "generacion.EventoDePrueba"), deProceso());

        MvcResult resultado = entregar(cuerpo(id, "generacion.EventoDePrueba"), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTipoSinManejador_cuandoSeEntrega_entonces400EventoNoReconocido() throws Exception {
        MvcResult resultado =
                entregar(cuerpo(UUID.randomUUID(), "generacion.OtroEvento"), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_EVENTO_NO_RECONOCIDO\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCuerpoQueNoCumpleElContrato_cuandoSeEntrega_entonces400ConDetalles()
            throws Exception {
        MvcResult sinUuid =
                entregar(
                        "{\"idEvento\":\"no-es-uuid\",\"tipo\":\"generacion.EventoDePrueba\","
                                + "\"origen\":\"generacion\",\"momento\":\"2026-10-14T15:04:05Z\","
                                + "\"contenido\":{}}",
                        deProceso());
        MvcResult sinTipo =
                entregar(
                        "{\"idEvento\":\""
                                + UUID.randomUUID()
                                + "\",\"origen\":\"generacion\",\"momento\":\"2026-10-14T15:04:05Z\","
                                + "\"contenido\":{}}",
                        deProceso());

        assertThat(sinUuid.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinTipo.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinTipo.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"")
                .contains("\"campo\":\"tipo\"");
        contrato.validar(sinUuid);
        contrato.validar(sinTipo);
        assertThat(REGISTRADOS).isEmpty();
    }

    @Test
    void dadoUnaEntregaSinToken_cuandoLlega_entonces401SegunElContrato() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                post("/interno/usuarios/eventos")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                cuerpo(
                                                        UUID.randomUUID(),
                                                        "generacion.EventoDePrueba")))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
        assertThat(REGISTRADOS).isEmpty();
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoEntregaUnEvento_entonces403SinRegistrarlo() throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(UUID.randomUUID(), "generacion.EventoDePrueba"),
                        jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
        assertThat(REGISTRADOS).isEmpty();
    }

    private MvcResult entregar(String cuerpo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(
                        post("/interno/usuarios/eventos")
                                .with(token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:distributivo"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    private static String cuerpo(UUID id, String tipo) {
        return "{\"idEvento\":\""
                + id
                + "\",\"tipo\":\""
                + tipo
                + "\",\"origen\":\"generacion\",\"momento\":\"2026-10-14T15:04:05Z\","
                + "\"contenido\":{\"nota\":\"evento de prueba B00-H05\"}}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        RegistrarEventoRecibido registrarEventoRecibido() {
            return evento -> {
                if (!"generacion.EventoDePrueba".equals(evento.tipo())) {
                    throw new EventoNoReconocido(evento.tipo());
                }
                REGISTRADOS.add(evento);
            };
        }
    }
}
