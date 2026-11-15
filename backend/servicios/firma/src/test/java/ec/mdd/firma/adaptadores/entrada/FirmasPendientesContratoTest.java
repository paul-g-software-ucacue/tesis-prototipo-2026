package ec.mdd.firma.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.firma.dominio.ArchivoDemasiadoGrande;
import ec.mdd.firma.dominio.ArchivoFirmadoRechazado;
import ec.mdd.firma.dominio.ArchivoNoDisponible;
import ec.mdd.firma.dominio.ArchivoVacio;
import ec.mdd.firma.dominio.PasoSinNombre;
import ec.mdd.firma.dominio.RechazoDelFirmante;
import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.dominio.SinTurnoDeFirma;
import ec.mdd.firma.puertos.entrada.ArchivoVigente;
import ec.mdd.firma.puertos.entrada.ConsultarFirmasPendientes;
import ec.mdd.firma.puertos.entrada.DescargarArchivoVigente;
import ec.mdd.firma.puertos.entrada.FirmaPendiente;
import ec.mdd.firma.puertos.entrada.RechazarEnFirma;
import ec.mdd.firma.puertos.entrada.RechazoRegistrado;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

/**
 * Las firmas pendientes y la descarga del archivo vigente ({@code contratos/firma/openapi.yaml},
 * F14-H01): la lista según el esquema, el PDF con sus cabeceras, el 403 común fuera de turno, el
 * 400 con un identificador mal formado y el 500 del catálogo si el archivo no está.
 */
@WebMvcTest(FirmasPendientesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({FirmasPendientesContratoTest.Dobles.class, ErroresFirma.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class FirmasPendientesContratoTest {

    static final UUID EN_TURNO = UUID.fromString("3f2504e0-4f89-41d3-9a0c-0305e82c3301");
    static final UUID SIN_TURNO = UUID.fromString("9a8b7c6d-1111-4222-8333-944455556666");
    static final UUID SIN_ARCHIVO = UUID.fromString("0b1c2d3e-aaaa-4bbb-8ccc-dddddddddddd");
    static final byte[] PDF = "%PDF-1.7 consolidado".getBytes(StandardCharsets.US_ASCII);
    static final String SHA = "c".repeat(64);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("firma");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElDesignado_cuandoPideSusPendientes_entonces200SegunElEsquema() throws Exception {
        MvcResult resultado = pedir("/api/firma/pendientes", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"rotulo\":\"Elaborado por\"")
                .contains(EN_TURNO.toString());
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoTienePendientes_cuandoLosPide_entonces200ConListaVacia() throws Exception {
        MvcResult resultado = pedir("/api/firma/pendientes", "vicerrectorado.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("[]");
        contrato.validar(resultado);
    }

    @Test
    void dadoElFirmanteEnTurno_cuandoDescarga_entoncesElPdfConSuNombreYSuHuella() throws Exception {
        MvcResult resultado = pedir(archivo(EN_TURNO), "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"distributivo-2026-I-v1.pdf\"");
        assertThat(resultado.getResponse().getHeader("ETag")).isEqualTo("\"" + SHA + "\"");
        assertThat(resultado.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoEstaEnTurno_cuandoDescarga_entoncesElMismo403QueSinPermiso() throws Exception {
        MvcResult fueraDeTurno = pedir(archivo(SIN_TURNO), "vicerrectorado.demo");
        MvcResult sinPermiso =
                mvc.perform(
                                get(archivo(EN_TURNO))
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
        assertThat(cuerpoSinIncidente(fueraDeTurno)).isEqualTo(cuerpoSinIncidente(sinPermiso));
        assertThat(fueraDeTurno.getResponse().getContentAsString())
                .contains("\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"")
                .doesNotContain(SIN_TURNO.toString());
        contrato.validar(fueraDeTurno);
    }

    @Test
    void dadoUnArchivoQueNoEsElRegistrado_cuandoSeDescarga_entonces500DelCatalogo()
            throws Exception {
        MvcResult resultado = pedir(archivo(SIN_ARCHIVO), "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"FIRMA_ARCHIVO_NO_DISPONIBLE\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnIdentificadorMalFormado_cuandoSeDescarga_entonces400() throws Exception {
        MvcResult resultado = pedir("/api/firma/documentos/no-es-uuid/archivo", "docente.demo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"");
    }

    private static String archivo(UUID documento) {
        return "/api/firma/documentos/" + documento + "/archivo";
    }

    private MvcResult pedir(String ruta, String usuario) throws Exception {
        return mvc.perform(
                        get(ruta)
                                .with(
                                        jwt().jwt(token -> token.subject(usuario))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_DISTRIBUTIVO:FIRMA"),
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_RESPONSABLE_CONSOLIDACION"))))
                .andReturn();
    }

    // El cuerpo común sin el identificador del incidente ni la ruta, que cambian.
    private static String cuerpoSinIncidente(MvcResult resultado) throws Exception {
        return resultado
                .getResponse()
                .getContentAsString()
                .replaceAll("\"(incidente|instancia|ruta|momento)\":\"[^\"]*\",?", "");
    }

    /** Los casos de uso: un documento en turno, uno fuera de turno y uno sin archivo. */
    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarFirmasPendientes consultarFirmasPendientes() {
            return quien ->
                    quien.usuario().equals("docente.demo")
                            ? List.of(
                                    new FirmaPendiente(
                                            EN_TURNO,
                                            "2026-I",
                                            1,
                                            1,
                                            "Elaborado por",
                                            Instant.parse("2026-11-14T08:43:00Z")))
                            : List.of();
        }

        @Bean
        DescargarArchivoVigente descargarArchivoVigente() {
            return (documento, quien) -> {
                if (documento.equals(SIN_ARCHIVO)) {
                    throw new ArchivoNoDisponible("no coincide");
                }
                // El turno lo decide el caso de uso: aquí, SIN_TURNO no es de nadie y EN_TURNO, el
                // ejemplo del contrato, se entrega para que PermisosFirmaTest vea pasar el permiso.
                if (!documento.equals(EN_TURNO)) {
                    throw new SinTurnoDeFirma();
                }
                return new ArchivoVigente("distributivo-2026-I-v1.pdf", SHA, PDF);
            };
        }

        /**
         * El rechazo en firma (F14-H04): SIN_TURNO no es de nadie; en EN_TURNO, el ejemplo del
         * contrato, rechaza el paso 2 del Vicerrector, con la observación que exige el dominio.
         */
        @Bean
        RechazarEnFirma rechazarEnFirma() {
            return (documento, observacion, quien) -> {
                if (!documento.equals(EN_TURNO)) {
                    throw new SinTurnoDeFirma();
                }
                RechazoDelFirmante rechazo =
                        new RechazoDelFirmante(
                                2,
                                quien.usuario(),
                                Instant.parse("2026-11-15T10:00:00Z"),
                                observacion);
                return new RechazoRegistrado(
                        documento,
                        "2026-I",
                        1,
                        rechazo.orden(),
                        "Revisado por",
                        rechazo.rechazadoPor(),
                        rechazo.rechazadoEn(),
                        rechazo.observacion());
            };
        }

        /**
         * La subida (F14-H02): el contenido del archivo dice qué responde. SIN_TURNO no es de
         * nadie; EN_TURNO, el ejemplo del contrato, acepta un archivo cualquiera para que
         * PermisosFirmaTest vea pasar el permiso.
         */
        @Bean
        SubirFirma subirFirma() {
            return (documento, archivo, quien) -> {
                if (!documento.equals(EN_TURNO)) {
                    throw new SinTurnoDeFirma();
                }
                if (archivo.length == 0) {
                    throw new ArchivoVacio();
                }
                if (archivo.length > ArchivoDemasiadoGrande.LIMITE_EN_BYTES) {
                    throw new ArchivoDemasiadoGrande(archivo.length);
                }
                String texto = new String(archivo, StandardCharsets.US_ASCII);
                if (texto.equals("SIN_NOMBRE")) {
                    throw new PasoSinNombre();
                }
                ResultadoValidacion resultado = SubirFirmaContratoTest.resultado(texto);
                if (!resultado.aceptada()) {
                    throw ArchivoFirmadoRechazado.de(resultado);
                }
                return resultado;
            };
        }
    }
}
