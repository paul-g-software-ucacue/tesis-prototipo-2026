package ec.mdd.reporting.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.Formato;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.DescargarReporte;
import ec.mdd.reporting.puertos.entrada.DescargarReporte.ArchivoDelReporte;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

/** Las descargas de los reportes cumplen su contrato (F20-H01, F20-H02, RF-48, D-115, D-117). */
@WebMvcTest(DescargasControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({DescargasContratoTest.Dobles.class, ErroresReporting.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class DescargasContratoTest {

    private static final String RUTA = "/api/reporting/periodos/%s/docentes/%s/reporte.%s";
    private static final String GENERAL = "/api/reporting/periodos/%s/reporte-general.%s";
    private static final List<String> DESCARGAS = new ArrayList<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("reporting");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        DESCARGAS.clear();
    }

    @Test
    void dadoUnDocente_cuandoSeDescargaElPdf_entoncesEsPdfConSuNombreYSinCache() throws Exception {
        MvcResult resultado =
                pedir(RUTA.formatted("2026-I", "0109990010", "pdf"), "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"reporte-individual-2026-I-0109990010.pdf\"");
        assertThat(resultado.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(resultado.getResponse().getContentAsByteArray()).startsWith('%', 'P');
        assertThat(DESCARGAS).containsExactly("rectorado.demo/PDF");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocente_cuandoSeDescargaElCsv_entoncesEsCsvEnUtf8() throws Exception {
        MvcResult resultado =
                pedir(RUTA.formatted("2026-I", "0109990010", "csv"), "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"reporte-individual-2026-I-0109990010.csv\"");
        assertThat(
                        new String(
                                resultado.getResponse().getContentAsByteArray(),
                                StandardCharsets.UTF_8))
                .startsWith("﻿");
        assertThat(DESCARGAS).containsExactly("rectorado.demo/CSV");
        contrato.validar(resultado);
    }

    @Test
    void dadosLosErrores_cuandoSeDescarga_entoncesSuCodigoYNingunArchivo() throws Exception {
        MvcResult sinPublicacion =
                pedir(RUTA.formatted("2026-II", "0109990010", "pdf"), "REPORTES:LECTURA");
        MvcResult sinDocente =
                pedir(RUTA.formatted("2026-I", "0109990099", "csv"), "REPORTES:LECTURA");
        MvcResult sinPermiso =
                pedir(RUTA.formatted("2026-I", "0109990010", "pdf"), "DISTRIBUTIVO:LECTURA");

        assertThat(sinPublicacion.getResponse().getStatus()).isEqualTo(409);
        assertThat(sinDocente.getResponse().getStatus()).isEqualTo(404);
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);
        assertThat(DESCARGAS).isEmpty();
        for (MvcResult uno : List.of(sinPublicacion, sinDocente, sinPermiso)) {
            assertThat(uno.getResponse().getContentType()).startsWith("application/json");
            contrato.validar(uno);
        }
    }

    @Test
    void dadoElPeriodo_cuandoSeDescarganLosDosArchivosDelGeneral_entoncesConSuNombreYSinCache()
            throws Exception {
        MvcResult elPdf = pedir(GENERAL.formatted("2026-I", "pdf"), "REPORTES:LECTURA");
        MvcResult elCsv = pedir(GENERAL.formatted("2026-I", "csv"), "REPORTES:LECTURA");

        assertThat(elPdf.getResponse().getStatus()).isEqualTo(200);
        assertThat(elPdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(elPdf.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"reporte-general-2026-I.pdf\"");
        assertThat(elPdf.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(elCsv.getResponse().getStatus()).isEqualTo(200);
        assertThat(elCsv.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(elCsv.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"reporte-general-2026-I.csv\"");
        assertThat(elCsv.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(DESCARGAS)
                .containsExactly("general/rectorado.demo/PDF", "general/rectorado.demo/CSV");
        contrato.validar(elPdf);
        contrato.validar(elCsv);
    }

    @Test
    void dadosLosErroresDelGeneral_cuandoSeDescarga_entoncesSuCodigoYNingunArchivo()
            throws Exception {
        MvcResult sinPublicacion = pedir(GENERAL.formatted("2026-II", "pdf"), "REPORTES:LECTURA");
        MvcResult inconsistente = pedir(GENERAL.formatted("2026-X", "csv"), "REPORTES:LECTURA");
        MvcResult caido = pedir(GENERAL.formatted("2026-Y", "pdf"), "REPORTES:LECTURA");
        MvcResult invalido = pedir(GENERAL.formatted("p".repeat(21), "csv"), "REPORTES:LECTURA");
        MvcResult sinPermiso = pedir(GENERAL.formatted("2026-I", "pdf"), "DISTRIBUTIVO:LECTURA");

        assertThat(sinPublicacion.getResponse().getStatus()).isEqualTo(409);
        assertThat(sinPublicacion.getResponse().getContentAsString())
                .contains("REPORTING_SIN_PUBLICACION");
        assertThat(inconsistente.getResponse().getStatus()).isEqualTo(500);
        assertThat(inconsistente.getResponse().getContentAsString())
                .contains("REPORTING_CONTENIDO_INCONSISTENTE")
                .doesNotContain("repetido")
                .doesNotContain("Exception");
        assertThat(caido.getResponse().getStatus()).isEqualTo(502);
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);
        assertThat(DESCARGAS).isEmpty();
        for (MvcResult uno : List.of(sinPublicacion, inconsistente, caido, invalido, sinPermiso)) {
            assertThat(uno.getResponse().getContentType()).startsWith("application/json");
            contrato.validar(uno);
        }
    }

    private MvcResult pedir(String ruta, String permiso) throws Exception {
        return mvc.perform(
                        get(ruta)
                                .with(
                                        jwt().jwt(token -> token.subject("rectorado.demo"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "PERMISO_" + permiso))))
                .andReturn();
    }

    /** La descarga, que anota quién y en qué formato, como el registro de la bitácora. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        DescargarReporte descargarReporte() {
            return new DescargarReporte() {
                @Override
                public ArchivoDelReporte descargar(
                        String periodo, String identificacion, Formato formato, String quien) {
                    if (!"2026-I".equals(periodo)) {
                        throw new SinPublicacion(periodo);
                    }
                    if (!"0109990010".equals(identificacion)) {
                        throw new DocenteSinDistributivo(periodo, 1);
                    }
                    DESCARGAS.add(quien + "/" + formato.name());
                    return archivo(
                            formato,
                            "reporte-individual-"
                                    + periodo
                                    + "-"
                                    + identificacion
                                    + "."
                                    + formato.extension());
                }

                @Override
                public ArchivoDelReporte descargarGeneral(
                        String periodo, Formato formato, String quien) {
                    switch (periodo) {
                        case "2026-I" -> DESCARGAS.add("general/" + quien + "/" + formato.name());
                        case "2026-X" -> throw new ContenidoInconsistente("docente repetido");
                        case "2026-Y" -> throw new ProcesoConError("distributivo caído");
                        default -> throw new SinPublicacion(periodo);
                    }
                    return archivo(
                            formato, "reporte-general-" + periodo + "." + formato.extension());
                }
            };
        }

        private static ArchivoDelReporte archivo(Formato formato, String nombre) {
            byte[] contenido =
                    formato == Formato.PDF
                            ? "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
                            : "\uFEFF\"Periodo\"\r\n".getBytes(StandardCharsets.UTF_8);
            return new ArchivoDelReporte(contenido, formato, nombre);
        }
    }
}
