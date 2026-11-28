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
import ec.mdd.reporting.dominio.CondicionesDeExportacion;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.CsvExportado;
import ec.mdd.reporting.dominio.ExportacionRechazada;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import ec.mdd.reporting.dominio.InconsistenciasAbiertas;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.dominio.ProblemaDelLayout;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import ec.mdd.reporting.puertos.entrada.ConsultarExportacionesSiga;
import ec.mdd.reporting.puertos.entrada.GenerarCsvSiga;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String EXPORTACIONES = "/api/reporting/periodos/%s/exportaciones-siga";
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
        // F19-H02 (D-120): la suma de verificación es el SHA-256 del cuerpo.
        assertThat(resultado.getResponse().getHeader("X-Suma-Verificacion"))
                .isEqualTo(
                        SumaDeVerificacion.de(resultado.getResponse().getContentAsByteArray())
                                .valor());
        contrato.validar(resultado);
    }

    @Test
    void dadasLasExportacionesDelPeriodo_cuandoSeConsultan_entoncesLaMasRecientePrimeroSinCache()
            throws Exception {
        MvcResult resultado = pedir(get(EXPORTACIONES.formatted("2026-I")), APROBACION);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"periodo\":\"2026-I\",\"exportaciones\":[{\"id\":"
                                + "\"00000000-0000-0000-0000-000000000002\",\"version\":1,"
                                + "\"layout\":2,\"nombre\":\"siga-2026-I-v1.csv\",\"filas\":165,"
                                + "\"sumaVerificacion\":\""
                                + "c".repeat(64)
                                + "\",\"exportadaPor\":\"secretaria.demo\","
                                + "\"exportadaEn\":\"2026-11-22T15:01:00Z\"},{\"id\":"
                                + "\"00000000-0000-0000-0000-000000000001\",\"version\":1,"
                                + "\"layout\":0,\"nombre\":\"siga-2026-I-v1.csv\",\"filas\":165,"
                                + "\"sumaVerificacion\":\""
                                + "b".repeat(64)
                                + "\",\"exportadaPor\":\"secretaria.demo\","
                                + "\"exportadaEn\":\"2026-11-22T15:00:00Z\"}]}");
        contrato.validar(resultado);
    }

    @Test
    void
            dadosUnPeriodoSinExportacionesUnoInvalidoYSinPermiso_cuandoSeConsultan_entonces200O400O403()
                    throws Exception {
        MvcResult vacia = pedir(get(EXPORTACIONES.formatted("2027-I")), APROBACION);
        MvcResult invalido = pedir(get(EXPORTACIONES.formatted("p".repeat(21))), APROBACION);
        MvcResult sinPermiso = pedir(get(EXPORTACIONES.formatted("2026-I")), "REPORTES:LECTURA");

        assertThat(vacia.getResponse().getStatus()).isEqualTo(200);
        assertThat(vacia.getResponse().getContentAsString())
                .isEqualTo("{\"periodo\":\"2027-I\",\"exportaciones\":[]}");
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(vacia);
        contrato.validar(invalido);
        contrato.validar(sinPermiso);
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
    // F19-H02 (D-120): la versión publicada sin todas sus firmas o con inconsistencias abiertas.
    @Test
    void dadaUnaVersionNoExportable_cuandoSeGenera_entonces409ConUnDetallePorCondicion()
            throws Exception {
        MvcResult faltan = pedir(get(CSV.formatted("2026-N")), APROBACION);
        MvcResult sinNada = pedir(get(CSV.formatted("2026-R")), APROBACION);

        assertThat(faltan.getResponse().getStatus()).isEqualTo(409);
        assertThat(faltan.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(
                        "REPORTING_VERSION_NO_EXPORTABLE",
                        "Versión no exportable",
                        "{\"campo\":\"firmas\",\"descripcion\":\"Tiene 2 de 3 firmas.\"}",
                        "{\"campo\":\"inconsistencias\",\"descripcion\":\"Tiene 1 dura y 2"
                                + " blandas sin justificar.\"}")
                .doesNotContain("a".repeat(64))
                .doesNotContain("Exception");
        assertThat(sinNada.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(
                        "{\"campo\":\"firmas\",\"descripcion\":\"No tiene documento firmado.\"}",
                        "{\"campo\":\"inconsistencias\",\"descripcion\":\"No se registraron sus"
                                + " inconsistencias al congelarla.\"}");
        contrato.validar(faltan);
        contrato.validar(sinNada);
    }

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
                    case "2026-N" -> throw rechazo(periodo, true);
                    case "2026-R" -> throw rechazo(periodo, false);
                    default -> throw new SinPublicacion(periodo);
                }
                ArchivoSiga archivo =
                        new ArchivoSiga(
                                "NUMERO_IDENTIFICACION,COMPONENTE,HORAS_SEMANALES\r\n"
                                        .getBytes(StandardCharsets.UTF_8),
                                "siga-2026-I-v1.csv");
                return new CsvExportado(
                        archivo,
                        ExportacionSiga.de(
                                new Publicacion(
                                        periodo, 1, "a".repeat(64), AHORA, "secretaria", false),
                                0,
                                archivo,
                                0,
                                quien,
                                AHORA));
            };
        }

        private static ExportacionRechazada rechazo(String periodo, boolean conDatos) {
            CondicionesDeExportacion condiciones =
                    CondicionesDeExportacion.de(
                            new Publicacion(periodo, 1, "a".repeat(64), AHORA, "secretaria", false),
                            conDatos
                                    ? Optional.of(
                                            new FirmasDeLaVersion(
                                                    "a".repeat(64),
                                                    List.of(
                                                            new FirmasDeLaVersion.PasoFirmado(
                                                                    1, true),
                                                            new FirmasDeLaVersion.PasoFirmado(
                                                                    2, true),
                                                            new FirmasDeLaVersion.PasoFirmado(
                                                                    3, false)),
                                                    false,
                                                    false))
                                    : Optional.empty(),
                            conDatos
                                    ? Optional.of(new InconsistenciasAbiertas(1, 2))
                                    : Optional.empty());
            return new ExportacionRechazada(periodo, 1, condiciones);
        }

        @Bean
        ConsultarExportacionesSiga consultarExportacionesSiga() {
            return periodo ->
                    "2026-I".equals(periodo)
                            ? List.of(
                                    new ExportacionSiga(
                                            UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                            "secretaria.demo",
                                            AHORA.plusSeconds(60),
                                            periodo,
                                            1,
                                            "a".repeat(64),
                                            2,
                                            "siga-2026-I-v1.csv",
                                            165,
                                            new SumaDeVerificacion("c".repeat(64))),
                                    new ExportacionSiga(
                                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                            "secretaria.demo",
                                            AHORA,
                                            periodo,
                                            1,
                                            "a".repeat(64),
                                            0,
                                            "siga-2026-I-v1.csv",
                                            165,
                                            new SumaDeVerificacion("b".repeat(64))))
                            : List.of();
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
