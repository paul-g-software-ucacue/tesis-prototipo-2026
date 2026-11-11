package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.mdd.generacion.dominio.DatosDeLaInconsistencia;
import ec.mdd.generacion.dominio.EntradaDelPanel;
import ec.mdd.generacion.dominio.InconsistenciaGlobal;
import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.dominio.ObjetoAfectado;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.dominio.VersionCongelada;
import ec.mdd.generacion.puertos.entrada.ConsolidadoEnRevision;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.EnRevision;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoJustificable;
import ec.mdd.generacion.puertos.entrada.JustificarInconsistencia;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** El panel cumple su contrato, con sus permisos y sus textos (RF-29, principio IV). */
@WebMvcTest(PanelControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({PanelContratoTest.Dobles.class, ErroresGeneracion.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PanelContratoTest {

    private static final String RUTA = "/api/generacion/periodos/%s/inconsistencias";
    private static final String CONSOLIDA = "CONSOLIDACION:LECTURA";
    private static final String CONSOLIDA_Y_ESCRIBE = "CONSOLIDACION:ESCRITURA";
    private static final String VICERRECTOR = "DISTRIBUTIVO:ESCRITURA";
    private static final String CUERPO =
            "{\"tipo\":\"PROPORCION\",\"objeto\":\"0109990010\","
                    + "\"texto\":\"Tiene horas de tronco común\"}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoConsolidacion_cuandoConsulta_entonces200ConElPanel() throws Exception {
        MvcResult resultado = pedir("2026-I", CONSOLIDA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"periodo\":\"2026-I\",\"creado\":true,\"modificable\":true")
                .contains("\"version\":2")
                .contains("\"abiertas\":{\"duras\":5,\"blandas\":3}")
                .contains("\"sinEvaluar\":[\"RN-03\",\"RN-04\",\"RN-07\",\"RN-08\",\"RN-09\"");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "DOCENTE_SIN_CLASE|RN-17|No tiene horas de clase.|proponerle al menos un paralelo|OFERTA",
                "PARALELO_SIN_PROPUESTA|RF-24|Falta el docente de 4 h de clase.|proponer un docente|OFERTA",
                "COORDINADORES_DE_LA_CARRERA|RN-12|La carrera tiene 2 coordinadores.|un solo coordinador|CARGOS",
                "GESTORES_FUERA_DE_RANGO|RN-10|Hay 7 docentes con cargo de gestión; la normativa admite de 8 a 11.|revisar los gestores|CARGOS",
                "HORAS_SOBRE_LA_MALLA|RN-18|Programación · A|bajar sus horas|OFERTA",
                "SOBRECARGA|RN-02|Tiene 21,5 h asignadas y su límite es 20 h.|bajar sus horas|ACTIVIDADES",
                "PROPORCION|RN-05|Tiene 72 % de clase y 28 % de otras actividades; la meta es 60/40, y se admite de 55 a 65 % de clase.|ajustar sus horas|ACTIVIDADES",
                "ACTIVIDADES_OBLIGATORIAS|RN-05|Le falta en su carga: 140-b.|asignarle lo que falta|ACTIVIDADES",
                "EXCEPCION_DE_AFINIDAD|RN-19|PEREZ LUIS|Ya está justificada|OFERTA"
            })
    void cadaTipo_cuandoSeConsulta_entoncesDiceQueHacerYDonde(
            String tipo, String regla, String detalle, String indicacion, String pantalla)
            throws Exception {
        String cuerpo = pedir("2026-I", CONSOLIDA).getResponse().getContentAsString();

        String suya = cuerpo.substring(cuerpo.indexOf("{\"tipo\":\"" + tipo + "\""));
        suya = suya.substring(0, suya.indexOf("\"pantalla\""));
        String resto = cuerpo.substring(cuerpo.indexOf("{\"tipo\":\"" + tipo + "\""));
        assertThat(suya)
                .contains("\"regla\":\"" + regla + "\"")
                .contains(detalle)
                .contains(indicacion);
        assertThat(resto).contains("\"pantalla\":\"" + pantalla + "\"");
    }

    // #215: en revisión se consulta, y lo dice.
    @Test
    void dadoElDistributivoEnRevision_cuandoSeConsulta_entonces200ConEnRevision() throws Exception {
        MvcResult resultado = pedir("EN-REVISION", CONSOLIDA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"enRevision\":true");
        contrato.validar(resultado);
        assertThat(pedir("2026-I", CONSOLIDA).getResponse().getContentAsString())
                .contains("\"enRevision\":false");
    }

    // F13-H01: con la versión congelada se consulta, y lo dice.
    @Test
    void dadaLaVersionCongelada_cuandoSeConsulta_entonces200ConVersionCongelada() throws Exception {
        MvcResult resultado = pedir("CONGELADA", CONSOLIDA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"versionCongelada\":true");
        contrato.validar(resultado);
        assertThat(pedir("2026-I", CONSOLIDA).getResponse().getContentAsString())
                .contains("\"versionCongelada\":false");
    }

    @Test
    void unaBlanda_cuandoSeConsulta_entoncesDiceQueSePuedeJustificar() throws Exception {
        String cuerpo = pedir("2026-I", CONSOLIDA).getResponse().getContentAsString();

        assertThat(cuerpo).contains("Consolidación puede justificarla aquí.");
        String dura = cuerpo.substring(cuerpo.indexOf("{\"tipo\":\"DOCENTE_SIN_CLASE\""));
        dura = dura.substring(0, dura.indexOf("\"pantalla\""));
        assertThat(dura).doesNotContain("justificarla");
    }

    @Test
    void unaCarreraSinCoordinador_cuandoSeConsulta_entoncesLoDice() throws Exception {
        assertThat(pedir("SIN-COORDINADOR", CONSOLIDA).getResponse().getContentAsString())
                .contains("La carrera no tiene coordinador.");
    }

    @Test
    void unaJustificada_cuandoSeConsulta_entoncesTraeSuJustificacion() throws Exception {
        MvcResult resultado = pedir("2026-I", CONSOLIDA);

        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"estado\":\"JUSTIFICADA\"")
                .contains("\"texto\":\"Dictó esta materia\",\"por\":\"coordinacion.demo\"");
    }

    @Test
    void sinDistributivo_cuandoConsulta_entonces200SinErrorNiVersion() throws Exception {
        MvcResult resultado = pedir("SIN", CONSOLIDA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"creado\":false")
                .contains("\"inconsistencias\":[]")
                .doesNotContain("\"version\"");
        contrato.validar(resultado);
    }

    @Test
    void conElPeriodoInexistente_cuandoConsulta_entonces404DelCatalogo() throws Exception {
        assertError(pedir("NADA", CONSOLIDA), 404, "GENERACION_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void dadoElVicerrector_cuandoConsulta_entonces200() throws Exception {
        assertThat(pedir("2026-I", VICERRECTOR).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoUnCoordinador_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado =
                pedir(
                        "2026-I",
                        "OFERTA:LECTURA:PROPIO",
                        "OFERTA:ESCRITURA:PROPIO",
                        "DISTRIBUTIVO:LECTURA:PROPIO");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void dadoConsolidacion_cuandoJustifica_entonces200ConLaJustificada() throws Exception {
        MvcResult resultado = justificar("2026-I", CUERPO, CONSOLIDA_Y_ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"tipo\":\"PROPORCION\"")
                .contains("\"estado\":\"JUSTIFICADA\"")
                .contains("\"texto\":\"Tiene horas de tronco común\"")
                .contains("\"por\":\"consolidacion.demo\"");
        contrato.validar(resultado);
        assertThat(Dobles.PEDIDOS).contains("consolidacion.demo");
    }

    @Test
    void dadoElVicerrector_cuandoJustifica_entonces403() throws Exception {
        assertThat(justificar("2026-I", CUERPO, VICERRECTOR).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void conUnTextoCorto_cuandoJustifica_entonces400EnElCampo() throws Exception {
        MvcResult resultado = justificar("CORTO", CUERPO, CONSOLIDA_Y_ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"campo\":\"texto\"")
                .contains("en 10 a 500 caracteres");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "RESUELTA,409,GENERACION_INCONSISTENCIA_NO_ENCONTRADA",
        "DURA,409,GENERACION_INCONSISTENCIA_DURA",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "SIN,409,GENERACION_SIN_DISTRIBUTIVO",
        "EN-REVISION,409,GENERACION_CONSOLIDADO_EN_REVISION",
        "CONGELADA,409,GENERACION_VERSION_CONGELADA"
    })
    void dadoLoQueNoAdmite_cuandoJustifica_entoncesElCodigoDelCatalogo(
            String periodo, int estado, String codigo) throws Exception {
        assertError(justificar(periodo, CUERPO, CONSOLIDA_Y_ESCRIBE), estado, codigo);
    }

    @Test
    void conUnObjetoMalEscrito_cuandoJustifica_entonces400() throws Exception {
        MvcResult resultado =
                justificar(
                        "2026-I",
                        "{\"tipo\":\"PROPORCION\",\"objeto\":\"<x>\",\"texto\":\"Un texto largo\"}",
                        CONSOLIDA_Y_ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    private MvcResult pedir(String periodo, String... permisos) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo)).with(token(permisos))).andReturn();
    }

    private MvcResult justificar(String periodo, String cuerpo, String... permisos)
            throws Exception {
        return mvc.perform(
                        put(RUTA.formatted(periodo) + "/justificacion")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(token(permisos)))
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

        private static final List<String> PEDIDOS = new ArrayList<>();
        private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
        private static final ObjetoAfectado DOCENTE =
                new ObjetoAfectado(ObjetoAfectado.Clase.DOCENTE, "0109990010", "PEREZ LUIS");
        private static final ObjetoAfectado PARALELO =
                new ObjetoAfectado(ObjetoAfectado.Clase.PARALELO, "POO-204/A", "Programación · A");
        private static final ObjetoAfectado CARRERA =
                new ObjetoAfectado(ObjetoAfectado.Clase.CARRERA, "TDS", "TDS");
        private static final ObjetoAfectado PERIODO =
                new ObjetoAfectado(ObjetoAfectado.Clase.PERIODO, "2026-I", "2026-I");

        @Bean
        ConsultarPanel consultarPanel() {
            return periodo ->
                    switch (periodo) {
                        case "NADA" -> throw new PeriodoNoEncontrado();
                        case "SIN" -> PanelDeInconsistencias.sinDistributivo(periodo, true);
                        case "SIN-COORDINADOR" ->
                                panel(
                                        periodo,
                                        abierta(
                                                TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA,
                                                Severidad.DURA,
                                                CARRERA,
                                                DatosDeLaInconsistencia.cantidad(BigDecimal.ZERO)));
                        default -> panel(periodo, entradas());
                    };
        }

        // #215: el periodo EN-REVISION tiene el distributivo en revisión. Es el mismo doble que el
        // de ConsolidadoContratoTest; PermisosGeneracionTest importa los dos y usa este.
        @Bean
        @Primary
        EnRevision enRevisionDelPanel() {
            return new EnRevision() {
                @Override
                public boolean enRevision(String periodo) {
                    return "EN-REVISION".equals(periodo) || "CONGELADA".equals(periodo);
                }

                // F13-H01: CONGELADA tiene la versión congelada.
                @Override
                public java.util.OptionalInt versionCongelada(String periodo) {
                    return "CONGELADA".equals(periodo)
                            ? java.util.OptionalInt.of(2)
                            : java.util.OptionalInt.empty();
                }
            };
        }

        @Bean
        JustificarInconsistencia justificarInconsistencia() {
            return solicitud -> {
                switch (solicitud.periodo()) {
                    case "CORTO" -> throw new JustificacionObligatoria();
                    case "RESUELTA" -> throw new InconsistenciaNoEncontrada();
                    case "DURA" -> throw new InconsistenciaNoJustificable();
                    case "CERRADO" -> throw new PeriodoCerrado();
                    case "SIN" -> throw new SinDistributivo();
                    case "EN-REVISION" -> throw new ConsolidadoEnRevision();
                    case "CONGELADA" -> throw new VersionCongelada(1);
                    default -> PEDIDOS.add(solicitud.quien());
                }
                return new EntradaDelPanel(
                        new InconsistenciaGlobal(
                                solicitud.tipo(),
                                Severidad.BLANDA,
                                DOCENTE,
                                new DatosDeLaInconsistencia(
                                        new BigDecimal("72"),
                                        new BigDecimal("60"),
                                        new BigDecimal("55"),
                                        new BigDecimal("65"),
                                        List.of())),
                        Optional.of(
                                new JustificacionDeInconsistencia(
                                        solicitud.tipo(),
                                        solicitud.objeto(),
                                        solicitud.texto(),
                                        solicitud.quien(),
                                        MOMENTO)));
            };
        }

        private static PanelDeInconsistencias panel(String periodo, EntradaDelPanel... entradas) {
            return new PanelDeInconsistencias(
                    periodo, true, Optional.of(UUID.randomUUID()), 2, List.of(entradas));
        }

        private static EntradaDelPanel[] entradas() {
            return new EntradaDelPanel[] {
                abierta(
                        TipoDeInconsistencia.DOCENTE_SIN_CLASE,
                        Severidad.DURA,
                        DOCENTE,
                        DatosDeLaInconsistencia.ninguno()),
                abierta(
                        TipoDeInconsistencia.PARALELO_SIN_PROPUESTA,
                        Severidad.DURA,
                        PARALELO,
                        DatosDeLaInconsistencia.cantidad(new BigDecimal("4.00"))),
                abierta(
                        TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA,
                        Severidad.DURA,
                        CARRERA,
                        DatosDeLaInconsistencia.cantidad(BigDecimal.TWO)),
                abierta(
                        TipoDeInconsistencia.HORAS_SOBRE_LA_MALLA,
                        Severidad.DURA,
                        CARRERA,
                        DatosDeLaInconsistencia.lista(List.of("Programación · A"))),
                abierta(
                        TipoDeInconsistencia.SOBRECARGA,
                        Severidad.DURA,
                        DOCENTE,
                        DatosDeLaInconsistencia.rango(
                                new BigDecimal("21.5"), null, new BigDecimal("20"))),
                abierta(
                        TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO,
                        Severidad.BLANDA,
                        PERIODO,
                        DatosDeLaInconsistencia.rango(
                                new BigDecimal("7"), new BigDecimal("8"), new BigDecimal("11"))),
                abierta(
                        TipoDeInconsistencia.PROPORCION,
                        Severidad.BLANDA,
                        DOCENTE,
                        new DatosDeLaInconsistencia(
                                new BigDecimal("72"),
                                new BigDecimal("60"),
                                new BigDecimal("55"),
                                new BigDecimal("65"),
                                List.of())),
                abierta(
                        TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS,
                        Severidad.BLANDA,
                        DOCENTE,
                        DatosDeLaInconsistencia.lista(List.of("140-b"))),
                new EntradaDelPanel(
                        new InconsistenciaGlobal(
                                TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD,
                                Severidad.BLANDA,
                                PARALELO,
                                DatosDeLaInconsistencia.lista(List.of("PEREZ LUIS"))),
                        Optional.of(
                                new JustificacionDeInconsistencia(
                                        TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD,
                                        "POO-204/A",
                                        "Dictó esta materia",
                                        "coordinacion.demo",
                                        MOMENTO)))
            };
        }

        private static EntradaDelPanel abierta(
                TipoDeInconsistencia tipo,
                Severidad severidad,
                ObjetoAfectado objeto,
                DatosDeLaInconsistencia datos) {
            return new EntradaDelPanel(
                    new InconsistenciaGlobal(tipo, severidad, objeto, datos), Optional.empty());
        }
    }
}
