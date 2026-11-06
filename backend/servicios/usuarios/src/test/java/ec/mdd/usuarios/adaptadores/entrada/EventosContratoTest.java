package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.RegistrarEventoRecibidoServicio;
import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
 * {@code POST /interno/usuarios/eventos} cumple el contrato de Usuarios (F17-H01, ADR-018), con la
 * seguridad de los procesos, el receptor real de la bitácora y sus repositorios en memoria.
 */
@WebMvcTest(EventosControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({EventosContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EventosContratoTest {

    private static final List<EntradaBitacora> REGISTRADOS = new CopyOnWriteArrayList<>();
    private static final Set<UUID> RECIBIDOS = ConcurrentHashMap.newKeySet();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    private static final String TIPO = "generacion.EntradaBitacora";

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        REGISTRADOS.clear();
        RECIBIDOS.clear();
    }

    @Test
    void dadoUnTokenDeProceso_cuandoEntregaUnaEntrada_entonces204YQuedaEnLaBitacora()
            throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = entregar(cuerpo(id, TIPO), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(REGISTRADOS)
                .singleElement()
                .satisfies(
                        evento -> {
                            assertThat(evento.idEvento()).isEqualTo(id);
                            assertThat(evento.servicio()).isEqualTo("generacion");
                            assertThat(evento.accion()).isEqualTo("ACTIVIDAD_GUARDADA");
                            assertThat(evento.anterior()).contains(java.util.Map.of("horas", "4"));
                            assertThat(evento.nuevo()).contains(java.util.Map.of("horas", "6"));
                        });
    }

    @Test
    void dadoUnaEntradaRepetida_cuandoLlegaOtraVez_entoncesTambien204SinDuplicarla()
            throws Exception {
        UUID id = UUID.randomUUID();
        entregar(cuerpo(id, TIPO), deProceso());

        MvcResult resultado = entregar(cuerpo(id, TIPO), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(REGISTRADOS).hasSize(1);
    }

    @Test
    void dadoUnTipoQueNoEsDeLaBitacora_cuandoSeEntrega_entonces400EventoNoReconocido()
            throws Exception {
        MvcResult otroTipo =
                entregar(cuerpo(UUID.randomUUID(), "generacion.EventoDePrueba"), deProceso());
        MvcResult otroOrigen =
                entregar(cuerpo(UUID.randomUUID(), "roles.EntradaBitacora"), deProceso());

        for (MvcResult resultado : List.of(otroTipo, otroOrigen)) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"COMUN_EVENTO_NO_RECONOCIDO\"");
            contrato.validar(resultado);
        }
        assertThat(REGISTRADOS).isEmpty();
    }

    @Test
    void dadoUnContenidoQueNoEsUnaEntradaValida_cuandoSeEntrega_entonces400EntradaInvalida()
            throws Exception {
        MvcResult sinValores =
                entregar(
                        cuerpo(
                                UUID.randomUUID(),
                                TIPO,
                                "{\"usuario\":\"coordinacion.demo\",\"accion\":\"ACTIVIDAD_GUARDADA\","
                                        + "\"momento\":\"2026-11-06T15:00:00Z\",\"objeto\":{}}"),
                        deProceso());
        MvcResult conToken =
                entregar(
                        cuerpo(
                                UUID.randomUUID(),
                                TIPO,
                                "{\"usuario\":\"coordinacion.demo\",\"accion\":\"ACTIVIDAD_GUARDADA\","
                                        + "\"momento\":\"2026-11-06T15:00:00Z\",\"objeto\":{},"
                                        + "\"nuevo\":{\"token\":\"secreto-123\"}}"),
                        deProceso());

        for (MvcResult resultado : List.of(sinValores, conToken)) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"USUARIOS_ENTRADA_BITACORA_INVALIDA\"")
                    .doesNotContain("secreto-123");
            contrato.validar(resultado);
        }
        assertThat(REGISTRADOS).isEmpty();
        assertThat(RECIBIDOS).isEmpty();
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
                                        .content(cuerpo(UUID.randomUUID(), TIPO)))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
        assertThat(REGISTRADOS).isEmpty();
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoEntregaUnEvento_entonces403SinRegistrarlo() throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(UUID.randomUUID(), TIPO),
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
        return cuerpo(
                id,
                tipo,
                "{\"usuario\":\"coordinacion.demo\",\"accion\":\"ACTIVIDAD_GUARDADA\","
                        + "\"momento\":\"2026-11-06T15:00:00Z\",\"periodo\":\"2026-I\","
                        + "\"objeto\":{\"actividad\":\"TUTORIAS\"},"
                        + "\"anterior\":{\"horas\":\"4\"},\"nuevo\":{\"horas\":\"6\"}}");
    }

    private static String cuerpo(UUID id, String tipo, String contenido) {
        return "{\"idEvento\":\""
                + id
                + "\",\"tipo\":\""
                + tipo
                + "\",\"origen\":\"generacion\",\"momento\":\"2026-11-06T15:00:01Z\","
                + "\"contenido\":"
                + contenido
                + "}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        RegistrarEventoRecibido registrarEventoRecibido() {
            return new RegistrarEventoRecibidoServicio(
                    (EventoRecibido evento) -> RECIBIDOS.add(evento.idEvento()), REGISTRADOS::add);
        }
    }
}
