package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion;
import ec.mdd.firma.dominio.GeneracionFallida;
import ec.mdd.firma.dominio.SinRolDeFirma;
import ec.mdd.firma.puertos.entrada.ConsultarDocumentoFirmable;
import ec.mdd.firma.puertos.entrada.EstadoDelDocumento;
import ec.mdd.firma.puertos.entrada.GenerarDocumentoFirmable;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * El documento firmable según {@code contratos/firma/openapi.yaml} (F13-H02, contracts/firma.md),
 * con el caso de uso simulado por periodo.
 */
@WebMvcTest(DocumentoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({DocumentoContratoTest.Dobles.class, ErroresFirma.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class DocumentoContratoTest {

    private static final String RUTA = "/api/firma/periodos/%s/documento";
    private static final String SHA = "b".repeat(64);
    private static final String HASH = "e".repeat(64);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("firma");

    @Autowired private MockMvc mvc;

    @Test
    void dadoCadaEstado_cuandoSeConsulta_entonces200SegunElContrato() throws Exception {
        for (String periodo : List.of("SIN", "CURSO", "FALLO", "LISTO")) {
            MvcResult resultado = pedir(get(RUTA.formatted(periodo)), "CONSOLIDACION:LECTURA");

            assertThat(resultado.getResponse().getStatus()).as(periodo).isEqualTo(200);
            contrato.validar(resultado);
        }
        assertThat(
                        pedir(get(RUTA.formatted("SIN")), "CONSOLIDACION:LECTURA")
                                .getResponse()
                                .getContentAsString())
                .isEqualTo("{\"periodo\":\"SIN\",\"estado\":\"SIN_GENERAR\",\"acciones\":[]}");
        assertThat(
                        pedir(get(RUTA.formatted("FALLO")), "CONSOLIDACION:LECTURA")
                                .getResponse()
                                .getContentAsString())
                .contains("\"motivo\":\"Falta el nombre del Rector.\"")
                .contains("\"acciones\":[\"GENERAR\"]");
        assertThat(
                        pedir(get(RUTA.formatted("LISTO")), "DISTRIBUTIVO:ESCRITURA")
                                .getResponse()
                                .getContentAsString())
                .contains("\"sha256\":\"" + SHA + "\"")
                .contains("\"hashContenido\":\"" + HASH + "\"");
    }

    @Test
    void dadoElResponsable_cuandoGenera_entonces200Generado() throws Exception {
        MvcResult resultado = pedir(post(RUTA.formatted("LISTO")), "CONSOLIDACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"estado\":\"GENERADO\"");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaGeneracionQueFalla_cuandoSeGenera_entonces409ConElMotivo() throws Exception {
        MvcResult resultado = pedir(post(RUTA.formatted("FALLO")), "CONSOLIDACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_GENERACION_FALLIDA\"")
                .contains("Falta el nombre del Rector.");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoQueNoEntroEnFirma_cuandoSeGenera_entonces409Etapa() throws Exception {
        MvcResult resultado = pedir(post(RUTA.formatted("SIN")), "CONSOLIDACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_ETAPA_NO_ADMITE_GENERACION\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoTieneElRolDeLaEtapa_cuandoGenera_entonces403Comun() throws Exception {
        MvcResult resultado = pedir(post(RUTA.formatted("OTRO-ROL")), "CONSOLIDACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"");
    }

    @Test
    void dadoUnPeriodoMalFormado_cuandoSeConsulta_entonces400() throws Exception {
        MvcResult resultado = pedir(get(RUTA.formatted("2026 I!")), "CONSOLIDACION:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"");
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion, String permiso)
            throws Exception {
        return mvc.perform(
                        peticion.with(
                                jwt().jwt(token -> token.subject("consolidacion.demo"))
                                        .authorities(
                                                new SimpleGrantedAuthority("PERMISO_" + permiso),
                                                new SimpleGrantedAuthority(
                                                        "ROLE_RESPONSABLE_CONSOLIDACION"))))
                .andReturn();
    }

    /** El caso de uso, con un estado por periodo. */
    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarDocumentoFirmable consultarDocumentoFirmable() {
            return (periodo, quien) -> estado(periodo);
        }

        @Bean
        GenerarDocumentoFirmable generarDocumentoFirmable() {
            return (periodo, quien) ->
                    switch (periodo) {
                        case "SIN" -> throw new EtapaNoAdmiteGeneracion();
                        case "FALLO" -> throw new GeneracionFallida("Falta el nombre del Rector.");
                        case "OTRO-ROL" -> throw new SinRolDeFirma();
                        default -> estado("LISTO");
                    };
        }

        private static EstadoDelDocumento estado(String periodo) {
            return switch (periodo) {
                case "SIN" ->
                        new EstadoDelDocumento(
                                periodo,
                                Optional.empty(),
                                EstadoDelDocumento.Estado.SIN_GENERAR,
                                Optional.empty(),
                                Optional.empty(),
                                List.of());
                case "CURSO" ->
                        new EstadoDelDocumento(
                                periodo,
                                Optional.of(1),
                                EstadoDelDocumento.Estado.EN_CURSO,
                                Optional.empty(),
                                Optional.empty(),
                                List.of());
                case "FALLO" ->
                        new EstadoDelDocumento(
                                periodo,
                                Optional.of(1),
                                EstadoDelDocumento.Estado.FALLIDA,
                                Optional.of("Falta el nombre del Rector."),
                                Optional.empty(),
                                List.of("GENERAR"));
                default ->
                        new EstadoDelDocumento(
                                periodo,
                                Optional.of(1),
                                EstadoDelDocumento.Estado.GENERADO,
                                Optional.empty(),
                                Optional.of(
                                        new EstadoDelDocumento.Documento(
                                                SHA,
                                                HASH,
                                                "sistema",
                                                Instant.parse("2026-11-11T15:00:05Z"))),
                                List.of());
            };
        }
    }
}
