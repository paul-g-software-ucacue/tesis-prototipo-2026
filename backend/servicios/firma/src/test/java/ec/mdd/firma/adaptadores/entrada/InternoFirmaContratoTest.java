package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.firma.dominio.AnulacionDeFirmas;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.FirmaRegistrada;
import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.dominio.FirmasDelDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasDeLaVersion;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
 * La cadena de firmas de una versión, entre procesos, cumple su contrato (F19-H02, D-120, ADR-023):
 * el estado de cada paso, la anulación y el rechazo, el 404 sin documento y el 403 con un token de
 * usuario.
 */
@WebMvcTest(InternoFirmaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoFirmaContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoFirmaContratoTest {

    private static final String FIRMAS = "/interno/firma/periodos/%s/versiones/%s/firmas";
    private static final String HASH = "a".repeat(64);
    private static final Instant MOMENTO = Instant.parse("2026-11-18T09:00:00Z");
    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                    new PasoDeFirma(3, "Aprobado", "RECTOR"));
    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(
                            1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Docente Uno"),
                    FirmanteEsperado.porIdentificacion(
                            2, "VICERRECTOR", "0109990028", "Vicerrector Dos"),
                    FirmanteEsperado.porIdentificacion(3, "RECTOR", "0109990010", "Rector Tres"));

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("firma");

    @Autowired private MockMvc mvc;

    @Test
    void dadaUnaCadenaCompleta_cuandoUnProcesoPideLasFirmas_entonces200ConTodosLosPasosFirmados()
            throws Exception {
        MvcResult resultado = pedir(FIRMAS.formatted("2026-I", 1), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"version\":1,\"hashContenido\":\""
                                + HASH
                                + "\",\"pasos\":[{\"orden\":1,\"estado\":\"FIRMADO\"},"
                                + "{\"orden\":2,\"estado\":\"FIRMADO\"},"
                                + "{\"orden\":3,\"estado\":\"FIRMADO\"}],"
                                + "\"anulada\":false,\"rechazada\":false}");
        contrato.validar(resultado);
    }

    @Test
    void dadoElPasoEnTurno_cuandoUnProcesoPideLasFirmas_entoncesElEnTurnoYElSiguienteSonPendientes()
            throws Exception {
        MvcResult resultado = pedir(FIRMAS.formatted("2026-I", 2), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "\"pasos\":[{\"orden\":1,\"estado\":\"FIRMADO\"},"
                                + "{\"orden\":2,\"estado\":\"PENDIENTE\"},"
                                + "{\"orden\":3,\"estado\":\"PENDIENTE\"}]",
                        "\"anulada\":false,\"rechazada\":false");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnRechazoEnFirma_cuandoUnProcesoPideLasFirmas_entoncesElPasoRechazadoYRechazada()
            throws Exception {
        MvcResult resultado = pedir(FIRMAS.formatted("2026-I", 3), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "{\"orden\":2,\"estado\":\"RECHAZADO\"}",
                        "\"anulada\":false,\"rechazada\":true");
        contrato.validar(resultado);
    }

    @Test
    void dadasFirmasAnuladas_cuandoUnProcesoPideLasFirmas_entoncesAnulada() throws Exception {
        MvcResult resultado = pedir(FIRMAS.formatted("2026-I", 4), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"anulada\":true,\"rechazada\":false");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaVersionSinDocumento_cuandoUnProcesoPideLasFirmas_entonces404() throws Exception {
        MvcResult resultado = pedir(FIRMAS.formatted("2026-I", 9), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadosUnaRutaInvalidaUnTokenDeUsuarioOSinToken_cuandoSePide_entonces400O403O401()
            throws Exception {
        MvcResult invalida = pedir(FIRMAS.formatted("2026-I", 0), deProceso());
        MvcResult deUsuario = pedir(FIRMAS.formatted("2026-I", 1), jwt());
        MvcResult sinToken = mvc.perform(get(FIRMAS.formatted("2026-I", 1))).andReturn();

        assertThat(invalida.getResponse().getStatus()).isEqualTo(400);
        assertThat(deUsuario.getResponse().getStatus()).isEqualTo(403);
        assertThat(sinToken.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(invalida);
        contrato.validar(deUsuario);
        contrato.validar(sinToken);
    }

    private MvcResult pedir(String ruta, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(ruta).with(token)).andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:salida"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    private static DocumentoFirmable documento(int version) {
        return new DocumentoFirmable(
                UUID.randomUUID(),
                "2026-I",
                version,
                DocumentoFirmable.CONSOLIDADO,
                HASH,
                "b".repeat(64),
                "documentos/" + "b".repeat(64) + ".pdf",
                1024,
                "sistema",
                MOMENTO);
    }

    private static FirmaRegistrada firma(int orden) {
        return new FirmaRegistrada(
                orden,
                String.valueOf((char) ('c' + orden)).repeat(64),
                2048,
                "firmante" + orden,
                MOMENTO.plusSeconds(orden));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarFirmasDeLaVersion consultarFirmasDeLaVersion() {
            return (periodo, version) ->
                    switch (version) {
                        case 1 ->
                                Optional.of(
                                        new FirmasDelDocumento(
                                                documento(1),
                                                CADENA,
                                                FIRMANTES,
                                                List.of(firma(1), firma(2), firma(3))));
                        case 2 ->
                                Optional.of(
                                        new FirmasDelDocumento(
                                                documento(2),
                                                CADENA,
                                                FIRMANTES,
                                                List.of(firma(1))));
                        case 3 ->
                                Optional.of(
                                        new FirmasDelDocumento(
                                                documento(3),
                                                CADENA,
                                                FIRMANTES,
                                                List.of(firma(1)),
                                                Optional.of(
                                                        new RechazoDelFirmante(
                                                                2,
                                                                "vicerrectorado.demo",
                                                                MOMENTO,
                                                                "Faltan las horas de gestión."))));
                        case 4 ->
                                Optional.of(
                                        new FirmasDelDocumento(
                                                documento(4),
                                                CADENA,
                                                FIRMANTES,
                                                List.of(firma(1), firma(2)),
                                                Optional.empty(),
                                                Optional.of(
                                                        new AnulacionDeFirmas(
                                                                "Versión nueva",
                                                                2,
                                                                "sistema",
                                                                MOMENTO))));
                        default -> Optional.empty();
                    };
        }
    }
}
