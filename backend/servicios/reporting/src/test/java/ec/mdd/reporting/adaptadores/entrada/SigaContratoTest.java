package ec.mdd.reporting.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CampoDelLayout;
import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.dominio.ProblemaDelLayout;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/** El CSV para SIGA y el layout de exportación cumplen su contrato (F19-H01, RF-52, D-119). */
@WebMvcTest(SigaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({SigaContratoTest.Dobles.class, ErroresReporting.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class SigaContratoTest {

    private static final String CSV = "/api/reporting/periodos/%s/csv-siga";
    private static final String LAYOUT = "/api/reporting/layout-siga";
    private static final String PLANTILLA = "/api/reporting/layout-siga/plantilla.yaml";
    private static final String APROBACION = "PUBLICACION:APROBACION";
    private static final List<String> PEDIDOS = new ArrayList<>();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("reporting");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void limpiar() {
        PEDIDOS.clear();
    }

    @Test
    void dadoElPeriodoPublicado_cuandoSeGeneraElCsv_entoncesEsCsvConSuNombreYSinCache()
            throws Exception {
        MvcResult resultado = pedir(get(CSV.formatted("2026-I")), APROBACION);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"siga-2026-I-v1.csv\"");
        assertThat(resultado.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .startsWith("NUMERO_IDENTIFICACION,");
        assertThat(PEDIDOS).containsExactly("csv/secretaria.demo/2026-I");
        contrato.validar(resultado);
    }

    @Test
    void dadosLosErroresDelCsv_cuandoSeGenera_entoncesSuCodigoYNingunArchivo() throws Exception {
        MvcResult sinPublicacion = pedir(get(CSV.formatted("2026-II")), APROBACION);
        MvcResult inconsistente = pedir(get(CSV.formatted("2026-X")), APROBACION);
        MvcResult caido = pedir(get(CSV.formatted("2026-Y")), APROBACION);
        MvcResult lento = pedir(get(CSV.formatted("2026-Z")), APROBACION);
        MvcResult invalido = pedir(get(CSV.formatted("p".repeat(21))), APROBACION);
        MvcResult sinPermiso = pedir(get(CSV.formatted("2026-I")), "REPORTES:LECTURA");

        assertThat(sinPublicacion.getResponse().getStatus()).isEqualTo(409);
        assertThat(sinPublicacion.getResponse().getContentAsString())
                .contains("REPORTING_SIN_PUBLICACION");
        assertThat(inconsistente.getResponse().getStatus()).isEqualTo(500);
        assertThat(inconsistente.getResponse().getContentAsString())
                .contains("REPORTING_CONTENIDO_INCONSISTENTE")
                .doesNotContain("0109990010")
                .doesNotContain("Exception")
                .doesNotContain("a".repeat(64));
        assertThat(caido.getResponse().getStatus()).isEqualTo(502);
        assertThat(lento.getResponse().getStatus()).isEqualTo(504);
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);
        assertThat(PEDIDOS).isEmpty();
        for (MvcResult uno :
                List.of(sinPublicacion, inconsistente, caido, lento, invalido, sinPermiso)) {
            assertThat(uno.getResponse().getContentType()).startsWith("application/json");
            contrato.validar(uno);
        }
    }

    @Test
    void dadoElLayout_cuandoSeConsulta_entoncesElVigenteYLosAnteriores() throws Exception {
        MvcResult resultado = pedir(get(LAYOUT), APROBACION);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "\"numero\":2",
                        "\"porDefecto\":false",
                        "\"cargadoPor\":\"secretaria.demo\"")
                .contains("\"traduccion\":[{\"codigo\":\"CLASE\",\"valor\":\"01\"}")
                .contains("\"anteriores\":[{\"numero\":1");
        contrato.validar(resultado);
    }

    @Test
    void laPlantilla_seDescargaComoYaml() throws Exception {
        MvcResult resultado = pedir(get(PLANTILLA), APROBACION);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).startsWith("text/yaml");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"layout-siga-plantilla.yaml\"");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .startsWith("# Layout");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnArchivoValido_cuandoSeCarga_entoncesLaVersionNueva() throws Exception {
        MvcResult resultado =
                cargar("columnas: valido".getBytes(StandardCharsets.UTF_8), APROBACION);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"numero\":3");
        assertThat(PEDIDOS).containsExactly("cargar/secretaria.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadosLosErroresDeLaCarga_cuandoSeCarga_entoncesSuCodigoYSusDetalles() throws Exception {
        MvcResult invalido = cargar("invalido".getBytes(StandardCharsets.UTF_8), APROBACION);
        MvcResult ilegible = cargar("ilegible".getBytes(StandardCharsets.UTF_8), APROBACION);
        MvcResult grande = cargar(new byte[262_145], APROBACION);
        MvcResult sinArchivo = pedir(multipart(HttpMethod.PUT, LAYOUT), APROBACION);

        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(invalido.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("REPORTING_LAYOUT_INVALIDO")
                .contains("\"campo\":\"columnas[2] (HORAS)\"")
                .contains("El campo «horas_semanales» no existe.");
        assertThat(ilegible.getResponse().getStatus()).isEqualTo(400);
        assertThat(ilegible.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("REPORTING_ARCHIVO_ILEGIBLE")
                .contains("Revisa la línea 3.")
                .doesNotContain("snakeyaml");
        assertThat(grande.getResponse().getStatus()).isEqualTo(413);
        assertThat(grande.getResponse().getContentAsString())
                .contains("REPORTING_ARCHIVO_DEMASIADO_GRANDE");
        assertThat(sinArchivo.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinArchivo.getResponse().getContentAsString())
                .contains("COMUN_SOLICITUD_INVALIDA");
        assertThat(PEDIDOS).isEmpty();
        for (MvcResult uno : List.of(invalido, ilegible, grande, sinArchivo)) {
            contrato.validar(uno);
        }
    }

    @Test
    void sinElPermisoDeAprobacion_lasOperacionesDelLayout_dan403() throws Exception {
        String reportes = "REPORTES:LECTURA";
        List<MvcResult> resultados =
                List.of(
                        pedir(get(LAYOUT), reportes),
                        pedir(get(PLANTILLA), reportes),
                        cargar("columnas: valido".getBytes(StandardCharsets.UTF_8), reportes));

        for (MvcResult uno : resultados) {
            assertThat(uno.getResponse().getStatus()).isEqualTo(403);
            contrato.validar(uno);
        }
        assertThat(PEDIDOS).isEmpty();
    }

    private MvcResult cargar(byte[] contenido, String permiso) throws Exception {
        return pedir(
                multipart(HttpMethod.PUT, LAYOUT)
                        .file(
                                new MockMultipartFile(
                                        "archivo", "layout.yaml", "text/yaml", contenido)),
                permiso);
    }

    private MvcResult pedir(AbstractMockHttpServletRequestBuilder<?> pedido, String permiso)
            throws Exception {
        return mvc.perform(
                        pedido.with(
                                jwt().jwt(token -> token.subject("secretaria.demo"))
                                        .authorities(
                                                new SimpleGrantedAuthority("PERMISO_" + permiso))))
                .andReturn();
    }

    /** Los casos de uso, que anotan quién pide. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        private static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");

        @Bean
        GenerarCsvSiga generarCsvSiga() {
            return (periodo, quien) -> {
                switch (periodo) {
                    case "2026-I" -> PEDIDOS.add("csv/" + quien + "/" + periodo);
                    case "2026-X" ->
                            throw new ContenidoInconsistente(
                                    "Las filas del docente 0109990010 no cuadran, hash "
                                            + "a".repeat(64));
                    case "2026-Y" -> throw new ProcesoConError("distributivo caído");
                    case "2026-Z" -> throw new ProcesoSinRespuesta();
                    default -> throw new SinPublicacion(periodo);
                }
                return new ArchivoSiga(
                        "NUMERO_IDENTIFICACION,COMPONENTE,HORAS_SEMANALES\r\n"
                                .getBytes(StandardCharsets.UTF_8),
                        "siga-2026-I-v1.csv");
            };
        }

        @Bean
        GestionarLayoutSiga gestionarLayoutSiga() {
            return new GestionarLayoutSiga() {
                @Override
                public LayoutsSiga consultar() {
                    return new LayoutsSiga(cargado(2), List.of(cargado(1)));
                }

                @Override
                public String plantilla() {
                    return "# Layout de exportación\ncolumnas: []\n";
                }

                @Override
                public LayoutExportacion cargar(byte[] archivo, String quien) {
                    String texto = new String(archivo, StandardCharsets.UTF_8);
                    if (texto.equals("invalido")) {
                        throw new LayoutInvalido(
                                List.of(
                                        new ProblemaDelLayout(
                                                "columnas[2] (HORAS)",
                                                "El campo «horas_semanales» no existe. Usa uno de"
                                                        + " la lista de la plantilla.")));
                    }
                    if (texto.equals("ilegible")) {
                        throw new ArchivoIlegible("snakeyaml: algo", 3, null);
                    }
                    PEDIDOS.add("cargar/" + quien);
                    return cargado(3);
                }
            };
        }

        private static LayoutExportacion cargado(int numero) {
            return LayoutExportacion.cargado(
                    numero,
                    "columnas: []",
                    List.of(
                            new ColumnaDelLayout(
                                    "NUMERO_IDENTIFICACION",
                                    CampoDelLayout.IDENTIFICACION,
                                    Map.of()),
                            new ColumnaDelLayout(
                                    "COMPONENTE",
                                    CampoDelLayout.COMPONENTE,
                                    Map.of(
                                            "CLASE", "01",
                                            "OTRAS_DOCENCIA", "02",
                                            "INVESTIGACION", "03",
                                            "VINCULACION", "04",
                                            "GESTION", "05")),
                            new ColumnaDelLayout("HORAS", CampoDelLayout.HORAS, Map.of())),
                    "secretaria.demo",
                    AHORA);
        }
    }
}
