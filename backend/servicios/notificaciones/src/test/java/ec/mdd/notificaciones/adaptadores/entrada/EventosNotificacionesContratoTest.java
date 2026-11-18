package ec.mdd.notificaciones.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.notificaciones.aplicacion.RecibirNotificacionesServicio;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
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
 * {@code POST /interno/notificaciones/eventos} cumple el contrato de Notificaciones (F16-H01,
 * ADR-018), con la seguridad de los procesos, el receptor real y sus puertos en memoria.
 */
@WebMvcTest(EventosNotificacionesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({EventosNotificacionesContratoTest.Dobles.class, ErroresNotificaciones.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class EventosNotificacionesContratoTest {

    private static final BandejasEnMemoria MEMORIA = new BandejasEnMemoria();
    private static final String TIPO = "validacionocs.NotificacionesSolicitadas";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("notificaciones");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        MEMORIA.limpiar();
    }

    @Test
    void dadoUnTokenDeProceso_cuandoEntregaUnaSolicitud_entonces204YLaNotificacionQueda()
            throws Exception {
        MvcResult resultado = entregar(cuerpo(UUID.randomUUID(), TIPO, contenido()), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(MEMORIA.guardadas)
                .singleElement()
                .satisfies(
                        notificacion -> {
                            assertThat(notificacion.destinatario())
                                    .isEqualTo("vicerrectorado.demo");
                            assertThat(notificacion.autorNombre())
                                    .isEqualTo("Nombre de consolidacion.demo");
                        });
    }

    @Test
    void dadaUnaSolicitudRepetida_cuandoLlegaOtraVez_entoncesTambien204SinDuplicarla()
            throws Exception {
        UUID id = UUID.randomUUID();
        entregar(cuerpo(id, TIPO, contenido()), deProceso());

        MvcResult resultado = entregar(cuerpo(id, TIPO, contenido()), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(MEMORIA.guardadas).hasSize(1);
    }

    @Test
    void dadoUnTipoNoAdmitido_cuandoSeEntrega_entonces400EventoNoReconocido() throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(UUID.randomUUID(), "validacionocs.EntradaBitacora", contenido()),
                        deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_EVENTO_NO_RECONOCIDO\"");
        contrato.validar(resultado);
        assertThat(MEMORIA.recibidos).isEmpty();
    }

    @Test
    void dadaUnaSolicitudInvalida_cuandoSeEntrega_entonces400NotificacionInvalidaSinElContenido()
            throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(
                                UUID.randomUUID(),
                                TIPO,
                                contenido()
                                        .replace(
                                                "\"enlace\":\"/consolidado\"",
                                                "\"enlace\":\"https://otro.sitio\",\"token\":\"secreto-123\"")),
                        deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"NOTIFICACIONES_NOTIFICACION_INVALIDA\"")
                .doesNotContain("secreto-123");
        contrato.validar(resultado);
        assertThat(MEMORIA.recibidos).isEmpty();
        assertThat(MEMORIA.guardadas).isEmpty();
    }

    @Test
    void dadoUnCuerpoQueNoCumpleElContrato_cuandoSeEntrega_entonces400ConDetalles()
            throws Exception {
        MvcResult sinTipo =
                entregar(
                        "{\"idEvento\":\""
                                + UUID.randomUUID()
                                + "\",\"origen\":\"validacionocs\",\"momento\":\"2026-11-15T15:00:01Z\","
                                + "\"contenido\":{}}",
                        deProceso());

        assertThat(sinTipo.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinTipo.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"");
        contrato.validar(sinTipo);
    }

    @Test
    void dadaUnaEntregaSinTokenOConUnTokenDeUsuario_cuandoLlega_entonces401O403SinRegistrar()
            throws Exception {
        MvcResult sinToken =
                mvc.perform(
                                post("/interno/notificaciones/eventos")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo(UUID.randomUUID(), TIPO, contenido())))
                        .andReturn();
        MvcResult deUsuario =
                entregar(
                        cuerpo(UUID.randomUUID(), TIPO, contenido()),
                        jwt().authorities(new SimpleGrantedAuthority("ROLE_VICERRECTOR")));

        assertThat(sinToken.getResponse().getStatus()).isEqualTo(401);
        assertThat(deUsuario.getResponse().getStatus()).isEqualTo(403);
        for (MvcResult resultado : List.of(sinToken, deUsuario)) {
            contrato.validar(resultado);
        }
        assertThat(MEMORIA.guardadas).isEmpty();
    }

    private MvcResult entregar(String cuerpo, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(
                        post("/interno/notificaciones/eventos")
                                .with(token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andReturn();
    }

    // F16-H02, D-110: Firma pide la notificación de firma pendiente para una persona.
    @Test
    void dadaLaSolicitudDeFirmaConDestinoPorIdentificacion_cuandoSeEntrega_entonces204()
            throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(
                                UUID.randomUUID(),
                                "firma.NotificacionesSolicitadas",
                                "firma",
                                deFirma()),
                        deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
        assertThat(MEMORIA.guardadas)
                .singleElement()
                .satisfies(
                        notificacion -> {
                            assertThat(notificacion.destinatario())
                                    .isEqualTo("vicerrectorado.demo");
                            assertThat(notificacion.titulo())
                                    .isEqualTo("Tienes una firma pendiente: «Revisado por»");
                            assertThat(notificacion.enlace()).isEqualTo("/firmas-pendientes");
                        });
    }

    @Test
    void dadaLaSolicitudDeFirmaConUnTokenDeUsuario_cuandoLlega_entonces403SinRegistrar()
            throws Exception {
        MvcResult resultado =
                entregar(
                        cuerpo(
                                UUID.randomUUID(),
                                "firma.NotificacionesSolicitadas",
                                "firma",
                                deFirma()),
                        jwt().authorities(new SimpleGrantedAuthority("ROLE_VICERRECTOR")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(resultado);
        assertThat(MEMORIA.recibidos).isEmpty();
        assertThat(MEMORIA.guardadas).isEmpty();
    }

    private static String deFirma() {
        return "{\"periodo\":\"2026-I\",\"autor\":\"docente.demo\","
                + "\"momento\":\"2026-11-18T15:04:05Z\",\"mensajes\":[{\"destino\":"
                + "{\"identificacion\":\"0109990028\"},\"titulo\":\"Tienes una firma"
                + " pendiente: «Revisado por»\",\"detalle\":\"Periodo 2026-I, versión 1."
                + " «Elaborado por» la firmó Docente (demo).\",\"enlace\":\"/firmas-pendientes\"}]}";
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:distributivo"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    private static String contenido() {
        return "{\"periodo\":\"2026-I\",\"autor\":\"consolidacion.demo\","
                + "\"momento\":\"2026-11-15T15:00:00Z\",\"mensajes\":[{\"destino\":"
                + "{\"rol\":\"VICERRECTOR\"},\"titulo\":\"El distributivo llegó a «Revisión del"
                + " Vicerrectorado»\",\"detalle\":\"Periodo 2026-I, versión 1.\","
                + "\"enlace\":\"/consolidado\"}]}";
    }

    private static String cuerpo(UUID id, String tipo, String contenido) {
        return cuerpo(id, tipo, "validacionocs", contenido);
    }

    private static String cuerpo(UUID id, String tipo, String origen, String contenido) {
        return "{\"idEvento\":\""
                + id
                + "\",\"tipo\":\""
                + tipo
                + "\",\"origen\":\""
                + origen
                + "\",\"momento\":\"2026-11-15T15:00:01Z\","
                + "\"contenido\":"
                + contenido
                + "}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        RecibirNotificaciones recibirNotificaciones() {
            return new RecibirNotificacionesServicio(MEMORIA, MEMORIA, MEMORIA, Clock.systemUTC());
        }
    }
}
