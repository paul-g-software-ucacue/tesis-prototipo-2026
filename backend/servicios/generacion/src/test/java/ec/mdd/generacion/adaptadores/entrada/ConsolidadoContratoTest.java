package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.generacion.dominio.ArchivoPdf;
import ec.mdd.generacion.dominio.AvanceDelParcial;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.CarreraConsolidada;
import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.InformeDemasiadoGrande;
import ec.mdd.generacion.dominio.InformeNoEsPdf;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.dominio.VersionCongelada;
import ec.mdd.generacion.puertos.entrada.ConsolidadoEnRevision;
import ec.mdd.generacion.puertos.entrada.Consolidar;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.EnRevision;
import ec.mdd.generacion.puertos.entrada.GestionarInforme;
import ec.mdd.generacion.puertos.entrada.InformeIlegible;
import ec.mdd.generacion.puertos.entrada.InformeNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.SinConsolidar;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** El consolidado cumple su contrato, con sus permisos (RF-32, principio IV). */
@WebMvcTest(ConsolidadoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConsolidadoContratoTest.Dobles.class, ErroresGeneracion.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ConsolidadoContratoTest {

    private static final String RUTA = "/api/generacion/periodos/%s/consolidado";
    private static final String LEE = "CONSOLIDACION:LECTURA";
    private static final String ESCRIBE = "CONSOLIDACION:ESCRITURA";
    private static final String VICERRECTOR = "DISTRIBUTIVO:ESCRITURA";
    private static final String[] COORDINADOR = {
        "OFERTA:LECTURA:PROPIO", "OFERTA:ESCRITURA:PROPIO", "DISTRIBUTIVO:LECTURA:PROPIO"
    };
    private static final byte[] PDF = "%PDF-1.7\nprueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    // #215: en revisión se consulta, y lo dice.
    @Test
    void dadoElDistributivoEnRevision_cuandoConsulta_entonces200ConEnRevision() throws Exception {
        MvcResult resultado = pedir("EN-REVISION", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"enRevision\":true");
        contrato.validar(resultado);
        assertThat(pedir("2026-I", LEE).getResponse().getContentAsString())
                .contains("\"enRevision\":false");
    }

    // F13-H01: con la versión congelada se consulta, y lo dice.
    @Test
    void dadaLaVersionCongelada_cuandoConsulta_entonces200ConVersionCongelada() throws Exception {
        MvcResult resultado = pedir("CONGELADA", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"versionCongelada\":true");
        contrato.validar(resultado);
        assertThat(pedir("2026-I", LEE).getResponse().getContentAsString())
                .contains("\"versionCongelada\":false");
        assertThat(pedir("SIN", LEE).getResponse().getContentAsString())
                .contains("\"versionCongelada\":false");
    }

    @Test
    void dadoConsolidacion_cuandoConsulta_entonces200ConLoCalculado() throws Exception {
        MvcResult resultado = pedir("2026-I", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"creado\":true,\"modificable\":true")
                .contains("\"version\":2")
                .contains("\"desactualizado\":true")
                .contains("\"consolidacion\":{\"por\":\"consolidacion.demo\"")
                .contains(
                        "\"carrera\":\"TDS\",\"estado\":\"EN_ELABORACION\",\"paralelos\":3,"
                                + "\"propuestos\":2,\"completo\":false")
                .contains("\"abiertas\":{\"duras\":2,\"blandas\":1}")
                .contains("\"nombre\":\"informe.pdf\"")
                .doesNotContain("\"clave\"");
        contrato.validar(resultado);
    }

    @Test
    void sinDistributivo_cuandoConsulta_entonces200SinErrorNiVersion() throws Exception {
        MvcResult resultado = pedir("SIN", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"creado\":false")
                .contains("\"carreras\":[]")
                .doesNotContain("\"version\"");
        contrato.validar(resultado);
    }

    @Test
    void conElPeriodoInexistente_cuandoConsulta_entonces404DelCatalogo() throws Exception {
        assertError(pedir("NADA", LEE), 404, "GENERACION_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void dadoConsolidacion_cuandoConsolida_entonces200ConElQueConsolido() throws Exception {
        MvcResult resultado = consolidar("2026-I", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(Dobles.CONSOLIDARON).contains("consolidacion.demo");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "SIN,409,GENERACION_SIN_DISTRIBUTIVO",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "EN-REVISION,409,GENERACION_CONSOLIDADO_EN_REVISION",
        "CONGELADA,409,GENERACION_VERSION_CONGELADA"
    })
    void dadoLoQueNoAdmite_cuandoConsolida_entoncesElCodigoDelCatalogo(
            String periodo, int estado, String codigo) throws Exception {
        assertError(consolidar(periodo, ESCRIBE), estado, codigo);
    }

    @Test
    void dadoConsolidacion_cuandoAdjunta_entonces200ConElInforme() throws Exception {
        MvcResult resultado = adjuntar("2026-I", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"nombre\":\"informe.pdf\"")
                .contains("\"por\":\"consolidacion.demo\"");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "NO-PDF,400,GENERACION_INFORME_NO_ES_PDF",
        "GRANDE,413,GENERACION_INFORME_DEMASIADO_GRANDE",
        "SIN-CONSOLIDAR,409,GENERACION_SIN_CONSOLIDAR",
        "SIN,409,GENERACION_SIN_DISTRIBUTIVO",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "EN-REVISION,409,GENERACION_CONSOLIDADO_EN_REVISION",
        "CONGELADA,409,GENERACION_VERSION_CONGELADA"
    })
    void dadoLoQueNoAdmite_cuandoAdjunta_entoncesElCodigoDelCatalogo(
            String periodo, int estado, String codigo) throws Exception {
        assertError(adjuntar(periodo, ESCRIBE), estado, codigo);
    }

    @Test
    void dadoUnInforme_cuandoDescarga_entoncesElMismoPdfConUnNombreFijo() throws Exception {
        MvcResult resultado = descargar("2026-I", LEE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).startsWith("application/pdf");
        assertThat(resultado.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .contains("informe-sustentado-2026-I.pdf");
    }

    @ParameterizedTest
    @CsvSource({
        "SIN-INFORME,404,GENERACION_INFORME_NO_ENCONTRADO",
        "ILEGIBLE,500,GENERACION_INFORME_ILEGIBLE"
    })
    void dadoLoQueNoSePuedeEntregar_cuandoDescarga_entoncesElCodigoDelCatalogo(
            String periodo, int estado, String codigo) throws Exception {
        assertError(descargar(periodo, LEE), estado, codigo);
    }

    @Test
    void dadoElVicerrector_cuandoLee_entoncesVeYDescargaPeroNoEscribe() throws Exception {
        assertThat(pedir("2026-I", VICERRECTOR).getResponse().getStatus()).isEqualTo(200);
        assertThat(descargar("2026-I", VICERRECTOR).getResponse().getStatus()).isEqualTo(200);
        assertThat(consolidar("2026-I", VICERRECTOR).getResponse().getStatus()).isEqualTo(403);
        assertThat(adjuntar("2026-I", VICERRECTOR).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void dadoQuienSoloLeeLaConsolidacion_cuandoEscribe_entonces403() throws Exception {
        assertThat(consolidar("2026-I", LEE).getResponse().getStatus()).isEqualTo(403);
        assertThat(adjuntar("2026-I", LEE).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void dadoUnCoordinador_cuandoIntentaCualquiera_entonces403() throws Exception {
        assertThat(pedir("2026-I", COORDINADOR).getResponse().getStatus()).isEqualTo(403);
        assertThat(consolidar("2026-I", COORDINADOR).getResponse().getStatus()).isEqualTo(403);
        assertThat(adjuntar("2026-I", COORDINADOR).getResponse().getStatus()).isEqualTo(403);
        assertThat(descargar("2026-I", COORDINADOR).getResponse().getStatus()).isEqualTo(403);
    }

    private MvcResult pedir(String periodo, String... permisos) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo)).with(token(permisos))).andReturn();
    }

    private MvcResult consolidar(String periodo, String... permisos) throws Exception {
        return mvc.perform(post(RUTA.formatted(periodo)).with(token(permisos))).andReturn();
    }

    private MvcResult adjuntar(String periodo, String... permisos) throws Exception {
        return mvc.perform(
                        multipart(RUTA.formatted(periodo) + "/informe")
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "informe.pdf", "application/pdf", PDF))
                                .with(
                                        peticion -> {
                                            peticion.setMethod("PUT");
                                            return peticion;
                                        })
                                .with(token(permisos)))
                .andReturn();
    }

    private MvcResult descargar(String periodo, String... permisos) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo) + "/informe").with(token(permisos)))
                .andReturn();
    }

    private static RequestPostProcessor token(String... permisos) {
        return jwt().jwt(
                        token -> {
                            token.subject("consolidacion.demo");
                            token.claim("permisos", List.of(permisos));
                        })
                .authorities(new AutoridadesDelToken());
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    /** Dobles escritos a mano. */
    @TestConfiguration
    static class Dobles {

        private static final List<String> CONSOLIDARON = new ArrayList<>();
        private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
        private static final InformeSustentado INFORME =
                new InformeSustentado(
                        "informe.pdf",
                        PDF.length,
                        ArchivoPdf.sha256(PDF),
                        UUID.randomUUID().toString(),
                        "consolidacion.demo",
                        MOMENTO);

        @Bean
        ConsultarConsolidado consultarConsolidado() {
            return periodo ->
                    switch (periodo) {
                        case "NADA" -> throw new PeriodoNoEncontrado();
                        case "SIN" -> ConsolidadoDelPeriodo.sinDistributivo(periodo, true);
                        default -> consolidado(periodo);
                    };
        }

        // #215: el periodo EN-REVISION tiene el distributivo en revisión; F13-H01: CONGELADA,
        // también su versión congelada.
        @Bean
        EnRevision enRevision() {
            return new EnRevision() {
                @Override
                public boolean enRevision(String periodo) {
                    return "EN-REVISION".equals(periodo) || "CONGELADA".equals(periodo);
                }

                @Override
                public java.util.OptionalInt versionCongelada(String periodo) {
                    return "CONGELADA".equals(periodo)
                            ? java.util.OptionalInt.of(2)
                            : java.util.OptionalInt.empty();
                }
            };
        }

        @Bean
        Consolidar consolidarElPeriodo() {
            return (periodo, quien) -> {
                switch (periodo) {
                    case "SIN" -> throw new SinDistributivo();
                    case "CERRADO" -> throw new PeriodoCerrado();
                    case "EN-REVISION" -> throw new ConsolidadoEnRevision();
                    case "CONGELADA" -> throw new VersionCongelada(1);
                    default -> CONSOLIDARON.add(quien);
                }
                return consolidado(periodo);
            };
        }

        @Bean
        GestionarInforme gestionarInforme() {
            return new GestionarInforme() {
                @Override
                public InformeSustentado adjuntar(
                        String periodo, String nombre, byte[] contenido, String quien) {
                    switch (periodo) {
                        case "NO-PDF" -> throw new InformeNoEsPdf();
                        case "GRANDE" -> throw new InformeDemasiadoGrande();
                        case "SIN-CONSOLIDAR" -> throw new SinConsolidar();
                        case "SIN" -> throw new SinDistributivo();
                        case "CERRADO" -> throw new PeriodoCerrado();
                        case "EN-REVISION" -> throw new ConsolidadoEnRevision();
                        case "CONGELADA" -> throw new VersionCongelada(1);
                        default -> {
                            return new InformeSustentado(
                                    nombre,
                                    contenido.length,
                                    ArchivoPdf.sha256(contenido),
                                    UUID.randomUUID().toString(),
                                    quien,
                                    MOMENTO);
                        }
                    }
                }

                @Override
                public byte[] descargar(String periodo) {
                    return switch (periodo) {
                        case "SIN-INFORME" -> throw new InformeNoEncontrado();
                        case "ILEGIBLE" -> throw new InformeIlegible();
                        default -> PDF.clone();
                    };
                }
            };
        }

        private static ConsolidadoDelPeriodo consolidado(String periodo) {
            CargaHoraria horas =
                    new CargaHoraria(
                            new BigDecimal("10"),
                            new BigDecimal("6"),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ONE);
            return new ConsolidadoDelPeriodo(
                    periodo,
                    true,
                    Optional.of(UUID.randomUUID()),
                    2,
                    Optional.of(new Consolidacion("consolidacion.demo", MOMENTO)),
                    true,
                    List.of(
                            new CarreraConsolidada(
                                    "TDS",
                                    EstadoDelParcial.EN_ELABORACION,
                                    new AvanceDelParcial(3, 2),
                                    horas)),
                    horas,
                    2,
                    1,
                    Optional.of(INFORME));
        }
    }
}
