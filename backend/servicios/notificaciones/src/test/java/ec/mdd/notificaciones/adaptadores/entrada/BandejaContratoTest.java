package ec.mdd.notificaciones.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.notificaciones.aplicacion.BandejaServicio;
import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * La bandeja cumple el contrato de Notificaciones (F16-H01, US2): cada usuario ve y marca solo lo
 * suyo, con solo la sesión, sin un permiso de la matriz (D-108).
 */
@WebMvcTest(BandejaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({BandejaContratoTest.Dobles.class, ErroresNotificaciones.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class BandejaContratoTest {

    static final BandejasEnMemoria MEMORIA = new BandejasEnMemoria();
    private static final Instant MOMENTO = Instant.parse("2026-11-15T15:00:00Z");

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("notificaciones");

    @Autowired private MockMvc mvc;

    private Notificacion vieja;
    private Notificacion nueva;
    private Notificacion ajena;

    @BeforeEach
    void preparar() {
        MEMORIA.limpiar();
        vieja = notificacion("vicerrectorado.demo", MOMENTO, Optional.of(MOMENTO.plusSeconds(5)));
        nueva = notificacion("vicerrectorado.demo", MOMENTO.plusSeconds(60), Optional.empty());
        ajena = notificacion("secretaria.demo", MOMENTO, Optional.empty());
        MEMORIA.guardar(List.of(vieja, nueva, ajena));
    }

    @Test
    void dadoElVicerrector_cuandoConsultaSuBandeja_entoncesSoloLasSuyasOrdenadasYElTotalSinLeer()
            throws Exception {
        MvcResult resultado =
                mvc.perform(get("/api/notificaciones/bandeja").with(de("vicerrectorado.demo")))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(resultado);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"sinLeer\":1").doesNotContain(ajena.id().toString());
        assertThat(cuerpo.indexOf(nueva.id().toString()))
                .isLessThan(cuerpo.indexOf(vieja.id().toString()));
        assertThat(cuerpo)
                .contains("\"leida\":true")
                .contains("\"leidaEn\":\"2026-11-15T15:00:05Z\"")
                .contains("\"autorNombre\":\"Responsable de consolidación\"");
    }

    @Test
    void dadoUnLimite_cuandoConsulta_entoncesTraeEsasYUnLimiteFueraDeRangoDa400() throws Exception {
        MvcResult uno =
                mvc.perform(
                                get("/api/notificaciones/bandeja")
                                        .param("limite", "1")
                                        .with(de("vicerrectorado.demo")))
                        .andReturn();
        MvcResult fuera =
                mvc.perform(
                                get("/api/notificaciones/bandeja")
                                        .param("limite", "101")
                                        .with(de("vicerrectorado.demo")))
                        .andReturn();

        assertThat(uno.getResponse().getContentAsString())
                .contains(nueva.id().toString())
                .doesNotContain(vieja.id().toString());
        assertThat(fuera.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(uno);
        contrato.validar(fuera);
    }

    @Test
    void dadoElVicerrector_cuandoConsultaSinLeer_entoncesElNumero() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get("/api/notificaciones/bandeja/sin-leer")
                                        .with(de("vicerrectorado.demo")))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("{\"sinLeer\":1}");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaSinLeer_cuandoLaMarcaDosVeces_entonces200ConLaMismaLectura() throws Exception {
        MvcResult primera = marcar(nueva.id(), "vicerrectorado.demo");
        String leidaEn = leidaEn(primera.getResponse().getContentAsString());
        MvcResult segunda = marcar(nueva.id(), "vicerrectorado.demo");

        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(primera);
        contrato.validar(segunda);
        assertThat(primera.getResponse().getContentAsString()).contains("\"leida\":true");
        assertThat(leidaEn(segunda.getResponse().getContentAsString())).isEqualTo(leidaEn);
        assertThat(MEMORIA.sinLeer("vicerrectorado.demo")).isZero();
    }

    @Test
    void dadaLaDeOtroUsuarioOUnIdInvalido_cuandoSeMarca_entonces404O400SinMarcar()
            throws Exception {
        MvcResult ajenaMarcada = marcar(ajena.id(), "vicerrectorado.demo");
        MvcResult inexistente = marcar(UUID.randomUUID(), "vicerrectorado.demo");
        MvcResult invalido =
                mvc.perform(
                                post("/api/notificaciones/bandeja/no-es-uuid/lectura")
                                        .with(de("vicerrectorado.demo")))
                        .andReturn();

        for (MvcResult resultado : List.of(ajenaMarcada, inexistente)) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA\"");
            contrato.validar(resultado);
        }
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(invalido);
        assertThat(MEMORIA.sinLeer("secretaria.demo")).isOne();
    }

    @Test
    void sinSesion_cuandoSeConsultaOSeMarca_entonces401() throws Exception {
        for (MvcResult resultado :
                List.of(
                        mvc.perform(get("/api/notificaciones/bandeja")).andReturn(),
                        mvc.perform(get("/api/notificaciones/bandeja/sin-leer")).andReturn(),
                        mvc.perform(post("/api/notificaciones/bandeja/" + nueva.id() + "/lectura"))
                                .andReturn())) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            contrato.validar(resultado);
        }
    }

    private MvcResult marcar(UUID id, String usuario) throws Exception {
        return mvc.perform(post("/api/notificaciones/bandeja/" + id + "/lectura").with(de(usuario)))
                .andReturn();
    }

    private static String leidaEn(String cuerpo) {
        int inicio = cuerpo.indexOf("\"leidaEn\":\"") + 11;
        return cuerpo.substring(inicio, cuerpo.indexOf('"', inicio));
    }

    /** Un usuario con sesión y un rol cualquiera, sin ningún permiso de la matriz. */
    static JwtRequestPostProcessor de(String usuario) {
        return jwt().jwt(token -> token.subject(usuario))
                .authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));
    }

    private static Notificacion notificacion(
            String destinatario, Instant momento, Optional<Instant> leidaEn) {
        return new Notificacion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                destinatario,
                Optional.empty(),
                "2026-I",
                "El distributivo llegó a «Revisión del Vicerrectorado»",
                "Periodo 2026-I, versión 1.",
                "/consolidado",
                "consolidacion.demo",
                "Responsable de consolidación",
                momento,
                momento.plusSeconds(1),
                leidaEn);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        BandejaServicio bandejaServicio() {
            return new BandejaServicio(MEMORIA, Clock.systemUTC());
        }
    }
}
