package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * La subida del PDF firmado ({@code contratos/firma/openapi.yaml}, F14-H02): 201 con el resultado
 * según el esquema, el 422 de cada comprobación con las siete en los detalles (F14-H03), el 409 del
 * paso sin nombre, el 400 y el 413 del catálogo, y el 403 común fuera de turno, igual que sin
 * permiso.
 */
@WebMvcTest(FirmasPendientesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({FirmasPendientesContratoTest.Dobles.class, ErroresFirma.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class SubirFirmaContratoTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T10:00:00Z");

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("firma");

    @Autowired private MockMvc mvc;

    /**
     * El resultado que devuelve el doble según el contenido del archivo: «aceptado», o el nombre de
     * la comprobación que falla.
     */
    static ResultadoValidacion resultado(String contenido) {
        // El nombre de una comprobación la rechaza, con las anteriores aprobadas; otro contenido,
        // todas aprobadas.
        List<ResultadoDeComprobacion> comprobaciones =
                java.util.Arrays.stream(Comprobacion.values())
                        .filter(una -> una.name().equals(contenido))
                        .findFirst()
                        .map(
                                falla -> {
                                    List<ResultadoDeComprobacion> evaluadas =
                                            new java.util.ArrayList<>();
                                    for (Comprobacion una : Comprobacion.values()) {
                                        if (una == falla) {
                                            evaluadas.add(
                                                    ResultadoDeComprobacion.rechazada(
                                                            una, "Rechazada en la prueba."));
                                            break;
                                        }
                                        evaluadas.add(ResultadoDeComprobacion.aprobada(una));
                                    }
                                    return ResultadoDeComprobacion.desde(
                                            evaluadas.toArray(ResultadoDeComprobacion[]::new));
                                })
                        .orElseGet(
                                () ->
                                        java.util.Arrays.stream(Comprobacion.values())
                                                .map(ResultadoDeComprobacion::aprobada)
                                                .toList());
        boolean conCertificado =
                !contenido.equals("MISMO_DOCUMENTO") && !contenido.equals("UNA_FIRMA_NUEVA");
        return new ResultadoValidacion(
                UUID.fromString("5d0c6f1e-2222-4333-8444-955566667777"),
                FirmasPendientesContratoTest.EN_TURNO,
                1,
                "docente.demo",
                AHORA,
                "e".repeat(64),
                Math.max(1, contenido.length()),
                comprobaciones,
                conCertificado
                        ? Optional.of(
                                new CertificadoDeLaFirma(
                                        "Firmante Uno Prueba (F1)",
                                        "CN=CA de Prueba MDD Demo,O=MDD Demo,C=EC",
                                        "19a2b3",
                                        AHORA.minusSeconds(86_400),
                                        AHORA.plusSeconds(86_400 * 365L)))
                        : Optional.empty());
    }

    @Test
    void dadoElFirmanteEnTurnoConUnaFirmaValida_cuandoSube_entonces201ConElResultado()
            throws Exception {
        MvcResult resultado =
                subir(FirmasPendientesContratoTest.EN_TURNO, "aceptado", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"aceptada\":true")
                .contains("\"comprobacion\":\"CONFIANZA\"")
                .contains("\"nombre\":\"Firmante Uno Prueba (F1)\"")
                .contains("\"serie\":\"19a2b3\"");
        contrato.validar(resultado);
    }

    @Test
    void dadaCadaComprobacionRechazada_cuandoSube_entonces422ConSuCodigoYLasCuatroEnLosDetalles()
            throws Exception {
        record Caso(String falla, String codigo) {}
        for (Caso caso :
                List.of(
                        new Caso("MISMO_DOCUMENTO", "FIRMA_NO_ES_LA_VERSION_VIGENTE"),
                        new Caso("UNA_FIRMA_NUEVA", "FIRMA_SIN_UNA_FIRMA_NUEVA"),
                        new Caso("INTEGRIDAD", "FIRMA_NO_INTEGRA"),
                        new Caso("CONFIANZA", "FIRMA_SIN_CONFIANZA"),
                        new Caso("VIGENCIA", "FIRMA_CERTIFICADO_NO_VIGENTE"),
                        new Caso("ORDEN", "FIRMA_FUERA_DE_ORDEN"),
                        new Caso("FIRMANTE", "FIRMA_FIRMANTE_EQUIVOCADO"))) {
            MvcResult resultado =
                    subir(FirmasPendientesContratoTest.EN_TURNO, caso.falla(), "docente.demo");

            String cuerpo = resultado.getResponse().getContentAsString();
            assertThat(resultado.getResponse().getStatus()).as(caso.falla()).isEqualTo(422);
            assertThat(cuerpo)
                    .contains("\"codigo\":\"" + caso.codigo() + "\"")
                    .contains("\"campo\":\"MISMO_DOCUMENTO\"")
                    .contains("\"campo\":\"UNA_FIRMA_NUEVA\"")
                    .contains("\"campo\":\"INTEGRIDAD\"")
                    .contains("\"campo\":\"CONFIANZA\"")
                    .contains("\"campo\":\"VIGENCIA\"")
                    .contains("\"campo\":\"ORDEN\"")
                    .contains("\"campo\":\"FIRMANTE\"");
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoElPasoEnTurnoSinNombreImpreso_cuandoSube_entonces409DelCatalogo() throws Exception {
        MvcResult resultado =
                subir(FirmasPendientesContratoTest.EN_TURNO, "SIN_NOMBRE", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_PASO_SIN_NOMBRE\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnArchivoVacio_cuandoSube_entonces400DelCatalogo() throws Exception {
        MvcResult resultado = subir(FirmasPendientesContratoTest.EN_TURNO, "", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_ARCHIVO_VACIO\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnArchivoDeMasDeDiezMegas_cuandoSube_entonces413DelCatalogo() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                conToken(
                                        multipart(
                                                        "/api/firma/documentos/"
                                                                + FirmasPendientesContratoTest
                                                                        .EN_TURNO
                                                                + "/firmas")
                                                .file(
                                                        new MockMultipartFile(
                                                                "archivo",
                                                                "firmado.pdf",
                                                                "application/pdf",
                                                                new byte
                                                                        [ArchivoDemasiadoGrande
                                                                                        .LIMITE_EN_BYTES
                                                                                + 1])),
                                        "docente.demo"))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(413);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_ARCHIVO_DEMASIADO_GRANDE\"");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaSubidaSinLaParteArchivoOConUnIdentificadorMalFormado_cuandoSube_entonces400()
            throws Exception {
        MvcResult sinArchivo =
                mvc.perform(
                                conToken(
                                        multipart(
                                                "/api/firma/documentos/"
                                                        + FirmasPendientesContratoTest.EN_TURNO
                                                        + "/firmas"),
                                        "docente.demo"))
                        .andReturn();
        MvcResult malFormado =
                mvc.perform(
                                conToken(
                                        multipart("/api/firma/documentos/no-es-uuid/firmas")
                                                .file(archivo("aceptado")),
                                        "docente.demo"))
                        .andReturn();

        assertThat(sinArchivo.getResponse().getStatus()).isEqualTo(400);
        assertThat(malFormado.getResponse().getStatus()).isEqualTo(400);
        assertThat(malFormado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"");
    }

    @Test
    void dadoQuienNoEstaEnTurno_cuandoSube_entoncesElMismo403QueSinPermisoYSinNombrarElDocumento()
            throws Exception {
        MvcResult fueraDeTurno =
                subir(FirmasPendientesContratoTest.SIN_TURNO, "aceptado", "vicerrectorado.demo");
        MvcResult sinPermiso =
                mvc.perform(
                                multipart(
                                                "/api/firma/documentos/"
                                                        + FirmasPendientesContratoTest.EN_TURNO
                                                        + "/firmas")
                                        .file(archivo("aceptado"))
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

    private static MockMultipartFile archivo(String contenido) {
        return new MockMultipartFile(
                "archivo",
                "firmado.pdf",
                "application/pdf",
                contenido.getBytes(StandardCharsets.US_ASCII));
    }

    private MvcResult subir(UUID documento, String contenido, String usuario) throws Exception {
        return mvc.perform(
                        conToken(
                                multipart("/api/firma/documentos/" + documento + "/firmas")
                                        .file(archivo(contenido)),
                                usuario))
                .andReturn();
    }

    private static MockMultipartHttpServletRequestBuilder conToken(
            MockMultipartHttpServletRequestBuilder pedido, String usuario) {
        pedido.with(
                jwt().jwt(token -> token.subject(usuario))
                        .authorities(
                                new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:FIRMA"),
                                new SimpleGrantedAuthority("ROLE_RESPONSABLE_CONSOLIDACION")));
        return pedido;
    }

    private static String sinIncidente(MvcResult resultado) throws Exception {
        return resultado
                .getResponse()
                .getContentAsString()
                .replaceAll("\"(incidente|instancia|ruta|momento)\":\"[^\"]*\",?", "");
    }
}
