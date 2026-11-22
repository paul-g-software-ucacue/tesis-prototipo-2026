package ec.mdd.reporting.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.FilaDeActividad;
import ec.mdd.reporting.dominio.FilaDeAsignatura;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
 * Las consultas de Reporting cumplen su contrato (F20-H01, F20-H02, RF-45, RF-46, principio IV).
 */
@WebMvcTest(ReportesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ReportingContratoTest.Dobles.class, ErroresReporting.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ReportingContratoTest {

    static final String HASH = "a".repeat(64);
    static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    false);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("reporting");

    @Autowired private MockMvc mvc;

    @Test
    void dadasLasPublicaciones_cuandoSeConsultan_entonces200() throws Exception {
        MvcResult resultado = pedir("/api/reporting/publicaciones", "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"periodo\":\"2026-I\",\"version\":1,\"hash\":\""
                                + HASH
                                + "\",\"publicadaEn\":\"2026-11-18T15:00:00Z\","
                                + "\"publicadaPor\":\"secretaria.demo\",\"enReforma\":false}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnPeriodoPublicado_cuandoSeConsultanSusDocentes_entonces200() throws Exception {
        MvcResult resultado = pedir("/api/reporting/periodos/2026-I/docentes", "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "\"docentes\":[{\"identificacion\":\"0109990010\",\"nombre\":\"PEREZ ANA\"}]");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocente_cuandoSeConsultaSuReporte_entonces200ConSusFilasYSusTotales()
            throws Exception {
        MvcResult resultado = pedir(reporte("2026-I", "0109990010"), "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo)
                .contains(
                        "\"docente\":{\"identificacion\":\"0109990010\",\"nombre\":\"PEREZ ANA\","
                                + "\"dedicacion\":\"TC\",\"horasContratadas\":40}")
                .contains(
                        "\"asignaturas\":[{\"carrera\":\"Software\",\"codigo\":\"BD-301\","
                                + "\"nombre\":\"Bases de datos\",\"paralelo\":\"A\","
                                + "\"horas\":8.00}]")
                .contains("\"unidad\":\"Suelos\"")
                .contains(
                        "\"horas\":{\"clase\":8.00,\"otrasDocencia\":0.00,"
                                + "\"investigacion\":2.50,\"vinculacion\":0.00,\"gestion\":0.00,"
                                + "\"total\":10.50}")
                .contains("\"generadoPor\":\"rectorado.demo\"");
        contrato.validar(resultado);
    }

    @Test
    void dadosLosErroresDelCatalogo_cuandoSeConsulta_entoncesSuCodigoYSuEstado() throws Exception {
        MvcResult sinPublicacion = pedir(reporte("2026-II", "0109990010"), "REPORTES:LECTURA");
        MvcResult sinDocente = pedir(reporte("2026-I", "0109990099"), "REPORTES:LECTURA");
        MvcResult inconsistente = pedir(reporte("2026-I", "0109990077"), "REPORTES:LECTURA");
        MvcResult caido = pedir(reporte("2026-I", "0109990066"), "REPORTES:LECTURA");
        MvcResult invalido = pedir(reporte("2026-I", "abc"), "REPORTES:LECTURA");

        assertThat(sinPublicacion.getResponse().getStatus()).isEqualTo(409);
        assertThat(sinPublicacion.getResponse().getContentAsString())
                .contains("REPORTING_SIN_PUBLICACION");
        assertThat(sinDocente.getResponse().getStatus()).isEqualTo(404);
        assertThat(sinDocente.getResponse().getContentAsString())
                .contains("REPORTING_DOCENTE_SIN_DISTRIBUTIVO");
        assertThat(inconsistente.getResponse().getStatus()).isEqualTo(500);
        assertThat(inconsistente.getResponse().getContentAsString())
                .contains("REPORTING_CONTENIDO_INCONSISTENTE")
                .doesNotContain(HASH)
                .doesNotContain("Exception");
        assertThat(caido.getResponse().getStatus()).isEqualTo(502);
        assertThat(invalido.getResponse().getStatus()).isEqualTo(400);
        for (MvcResult uno : List.of(sinPublicacion, sinDocente, inconsistente, caido, invalido)) {
            contrato.validar(uno);
        }
    }

    @Test
    void dadoUnPeriodoPublicado_cuandoSeConsultaElReporteGeneral_entonces200ConLosTotales()
            throws Exception {
        MvcResult resultado = pedir(general("2026-I"), "REPORTES:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "\"docentes\":[{\"identificacion\":\"0109990010\","
                                + "\"nombre\":\"PERALTA MALDONADO JUAN ANDRES\","
                                + "\"horas\":{\"clase\":8.00,"
                                + "\"otrasDocencia\":2.50,\"investigacion\":0.00,"
                                + "\"vinculacion\":0.00,\"gestion\":0.00,\"total\":10.50},"
                                + "\"dedicacion\":\"TC\"},"
                                + "{\"identificacion\":\"0109990020\","
                                + "\"nombre\":\"ZAMBRANO PAREDES ROSA ELENA\",\"horas\":")
                .contains(
                        "\"totales\":{\"clase\":20.25,\"otrasDocencia\":2.50,"
                                + "\"investigacion\":3.00,\"vinculacion\":0.00,"
                                + "\"gestion\":0.00,\"total\":25.75}")
                .contains("\"generadoPor\":\"rectorado.demo\"");
        contrato.validar(resultado);
    }

    @Test
    void dadosLosErroresDelCatalogo_cuandoSeConsultaElGeneral_entoncesSuCodigoYSuEstado()
            throws Exception {
        MvcResult sinPublicacion = pedir(general("2026-II"), "REPORTES:LECTURA");
        MvcResult inconsistente = pedir(general("2026-X"), "REPORTES:LECTURA");
        MvcResult caido = pedir(general("2026-Y"), "REPORTES:LECTURA");
        MvcResult invalido = pedir(general("p".repeat(21)), "REPORTES:LECTURA");

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
        for (MvcResult uno : List.of(sinPublicacion, inconsistente, caido, invalido)) {
            contrato.validar(uno);
        }
    }

    @Test
    void dadoUnUsuarioSinReportes_cuandoConsultaCualquierOperacion_entonces403() throws Exception {
        for (String ruta :
                List.of(
                        "/api/reporting/publicaciones",
                        "/api/reporting/periodos/2026-I/docentes",
                        reporte("2026-I", "0109990010"),
                        general("2026-I"))) {
            MvcResult resultado = pedir(ruta, "DISTRIBUTIVO:LECTURA");
            assertThat(resultado.getResponse().getStatus()).as(ruta).isEqualTo(403);
            contrato.validar(resultado);
        }
    }

    private static String reporte(String periodo, String identificacion) {
        return "/api/reporting/periodos/" + periodo + "/docentes/" + identificacion + "/reporte";
    }

    private static String general(String periodo) {
        return "/api/reporting/periodos/" + periodo + "/reporte-general";
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

    /** El reporte de Ana, el que usan también las descargas. */
    static ReporteIndividual reporteDeAna(String quien) {
        return new ReporteIndividual(
                PUBLICACION,
                new DatosDelDocente("0109990010", "PEREZ ANA", Dedicacion.TC, 40),
                List.of(
                        new FilaDeAsignatura(
                                "Software", "BD-301", "Bases de datos", "A", new BigDecimal("8"))),
                List.of(
                        new FilaDeActividad(
                                Componente.INVESTIGACION,
                                "141-a",
                                "Proyecto",
                                "Suelos",
                                new BigDecimal("2.5"))),
                new HorasPorComponente(
                        new BigDecimal("8"),
                        BigDecimal.ZERO,
                        new BigDecimal("2.5"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO),
                quien,
                Instant.parse("2026-11-21T15:00:00Z"));
    }

    /**
     * El reporte general de dos docentes de los datos de prueba, uno sin dedicación; el que usan
     * también las descargas (F20-H02).
     */
    static ReporteGeneral reporteGeneral(String quien) {
        return ReporteGeneral.de(
                PUBLICACION,
                new DocentesDeLaVersion(
                        HASH,
                        List.of(
                                new DocentesDeLaVersion.Docente(
                                        "0109990020",
                                        "ZAMBRANO PAREDES ROSA ELENA",
                                        null,
                                        new HorasPorComponente(
                                                new BigDecimal("12.25"),
                                                BigDecimal.ZERO,
                                                new BigDecimal("3"),
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO)),
                                new DocentesDeLaVersion.Docente(
                                        "0109990010",
                                        "PERALTA MALDONADO JUAN ANDRES",
                                        Dedicacion.TC,
                                        new HorasPorComponente(
                                                new BigDecimal("8"),
                                                new BigDecimal("2.5"),
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO)))),
                quien,
                Instant.parse("2026-11-21T15:00:00Z"));
    }

    /** El caso de uso, con un resultado por periodo y por docente. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarReportes consultarReportes() {
            return new ConsultarReportes() {
                @Override
                public List<Publicacion> publicaciones() {
                    return List.of(PUBLICACION);
                }

                @Override
                public DocentesDelPeriodo docentes(String periodo) {
                    exigirPublicado(periodo);
                    return new DocentesDelPeriodo(
                            PUBLICACION,
                            List.of(
                                    new DocentesDeLaVersion.Docente(
                                            "0109990010",
                                            "PEREZ ANA",
                                            Dedicacion.TC,
                                            HorasPorComponente.NINGUNA)));
                }

                @Override
                public ReporteIndividual reporte(
                        String periodo, String identificacion, String quien) {
                    exigirPublicado(periodo);
                    return switch (identificacion) {
                        case "0109990010" -> reporteDeAna(quien);
                        case "0109990077" -> throw new ContenidoInconsistente("otro hash");
                        case "0109990066" -> throw new ProcesoConError("distributivo caído");
                        default -> throw new DocenteSinDistributivo(periodo, 1);
                    };
                }

                @Override
                public ReporteGeneral reporteGeneral(String periodo, String quien) {
                    return switch (periodo) {
                        case "2026-I" -> ReportingContratoTest.reporteGeneral(quien);
                        case "2026-X" -> throw new ContenidoInconsistente("docente repetido");
                        case "2026-Y" -> throw new ProcesoConError("distributivo caído");
                        default -> throw new SinPublicacion(periodo);
                    };
                }

                private void exigirPublicado(String periodo) {
                    if (!"2026-I".equals(periodo)) {
                        throw new SinPublicacion(periodo);
                    }
                }
            };
        }
    }
}
