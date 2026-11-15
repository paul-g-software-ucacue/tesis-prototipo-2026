package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El rechazo en firma ({@code contratos/firma/openapi.yaml}, F14-H04): 201 con el rechazo según el
 * esquema, el 400 de la observación corta del catálogo, el 400 común de un cuerpo que no cumple el
 * contrato, y el 403 común fuera de turno, igual que sin permiso.
 */
@WebMvcTest(FirmasPendientesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({FirmasPendientesContratoTest.Dobles.class, ErroresFirma.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class RechazarEnFirmaContratoTest {

    private static final String OBSERVACION = "Falta la carga de un docente de Software.";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("firma");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElFirmanteEnTurno_cuandoRechazaConObservacion_entonces201SegunElEsquema()
            throws Exception {
        MvcResult resultado =
                rechazar(FirmasPendientesContratoTest.EN_TURNO, cuerpo("  " + OBSERVACION + " "));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"rotulo\":\"Revisado por\"")
                .contains("\"orden\":2")
                .contains("\"observacion\":\"" + OBSERVACION + "\"")
                .contains("\"rechazadoPor\":\"vicerrectorado.demo\"");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaObservacionCorta_cuandoRechaza_entonces400DelCatalogo() throws Exception {
        MvcResult resultado =
                rechazar(FirmasPendientesContratoTest.EN_TURNO, cuerpo("   corta   "));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_OBSERVACION_INVALIDA\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCuerpoSinObservacionOConMasDe500_cuandoRechaza_entonces400Comun() throws Exception {
        for (String cuerpo : new String[] {"{}", cuerpo("a".repeat(501))}) {
            MvcResult resultado = rechazar(FirmasPendientesContratoTest.EN_TURNO, cuerpo);

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"");
        }
    }

    @Test
    void dadoQuienNoEstaEnTurno_cuandoRechaza_entoncesElMismo403QueSinPermiso() throws Exception {
        MvcResult fueraDeTurno =
                rechazar(FirmasPendientesContratoTest.SIN_TURNO, cuerpo(OBSERVACION));
        MvcResult sinPermiso =
                mvc.perform(
                                post(ruta(FirmasPendientesContratoTest.EN_TURNO))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo(OBSERVACION))
                                        .with(
                                                jwt().jwt(
                                                                token ->
                                                                        token.subject(
                                                                                "coordinacion.demo"))
                                                        .authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_COORDINADOR_CARRERA"))))
                        .andReturn();

        assertThat(fueraDeTurno.getResponse().getStatus()).isEqualTo(403);
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);
        assertThat(sinIncidente(fueraDeTurno)).isEqualTo(sinIncidente(sinPermiso));
        assertThat(fueraDeTurno.getResponse().getContentAsString())
                .contains("\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"")
                .doesNotContain(FirmasPendientesContratoTest.SIN_TURNO.toString());
        contrato.validar(fueraDeTurno);
    }

    @Test
    void dadoQuienNoEstaEnTurno_cuandoRechazaSinObservacion_entonces403Igual() throws Exception {
        MvcResult resultado = rechazar(FirmasPendientesContratoTest.SIN_TURNO, cuerpo(""));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void dadoQuienNoInicioSesion_cuandoRechaza_entonces401() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                post(ruta(FirmasPendientesContratoTest.EN_TURNO))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo(OBSERVACION)))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    private static String ruta(UUID documento) {
        return "/api/firma/documentos/" + documento + "/rechazos";
    }

    private static String cuerpo(String observacion) {
        return "{\"observacion\":\"" + observacion + "\"}";
    }

    private MvcResult rechazar(UUID documento, String cuerpo) throws Exception {
        return mvc.perform(
                        post(ruta(documento))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(
                                        jwt().jwt(token -> token.subject("vicerrectorado.demo"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_DISTRIBUTIVO:FIRMA"),
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_VICERRECTOR"))))
                .andReturn();
    }

    private static String sinIncidente(MvcResult resultado) throws Exception {
        return resultado
                .getResponse()
                .getContentAsString()
                .replaceAll("\"(incidente|instancia|ruta|momento)\":\"[^\"]*\",?", "");
    }
}
