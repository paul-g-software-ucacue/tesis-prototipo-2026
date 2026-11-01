package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.CargoNoVigente;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.HorasNoValidas;
import ec.mdd.generacion.dominio.ParcialNoEditable;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import ec.mdd.generacion.dominio.ProyectoNoValido;
import ec.mdd.generacion.puertos.entrada.ActividadFueraDelCatalogo;
import ec.mdd.generacion.puertos.entrada.ActividadGuardada;
import ec.mdd.generacion.puertos.entrada.ActividadNoEncontrada;
import ec.mdd.generacion.puertos.entrada.ActividadPosible;
import ec.mdd.generacion.puertos.entrada.ActividadQuitada;
import ec.mdd.generacion.puertos.entrada.ActividadesDeCarrera;
import ec.mdd.generacion.puertos.entrada.ActividadesDelDocente;
import ec.mdd.generacion.puertos.entrada.ActividadesDelPeriodo;
import ec.mdd.generacion.puertos.entrada.AsignarActividad;
import ec.mdd.generacion.puertos.entrada.ConsultarActividades;
import ec.mdd.generacion.puertos.entrada.ConsultarActividadesPosibles;
import ec.mdd.generacion.puertos.entrada.DocenteNoActivo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDeActividad;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProcesoSinRespuesta;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Las actividades no lectivas cumplen su contrato (RF-26, principio IV), con tokens con reclamos
 * reales: el alcance sale del permiso de escribir (D-64).
 */
@WebMvcTest(ActividadesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ActividadesContratoTest.Dobles.class, ErroresGeneracion.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ActividadesContratoTest {

    private static final String RUTA =
            "/api/generacion/periodos/%s/parciales/%s/docentes/0109990010/actividades/%s";
    private static final String CUERPO = "{\"horas\":4.25}";
    private static final String ESCRIBE = "OFERTA:ESCRITURA:PROPIO";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadoUnCoordinador_cuandoAsigna_entonces200ConElAviso() throws Exception {
        MvcResult resultado = asignar("2026-I", "TDS", "140-b", CUERPO, "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"actividad\":{\"actividad\":\"140-b\",\"nombre\":\"Planificar\","
                                + "\"componente\":\"OTRAS_DOCENCIA\",\"horas\":4.25},"
                                + "\"horasSuficientes\":false}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnProyecto_cuandoAsigna_entoncesLaRespuestaLoTrae() throws Exception {
        MvcResult resultado =
                asignar(
                        "2026-I",
                        "TDS",
                        "141-a",
                        "{\"horas\":6,\"proyecto\":\"INV-2025-01\"}",
                        "TDS",
                        ESCRIBE);

        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"proyecto\":\"INV-2025-01\"", "\"nombreProyecto\":\"Monitoreo\"");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"horas\":0}", "{\"horas\":-1}", "{\"horas\":40.01}"})
    void dadasHorasFueraDeRango_cuandoAsigna_entonces400(String cuerpo) throws Exception {
        MvcResult resultado = asignar("FALLA", "TDS", "140-b", cuerpo, "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        contrato.validar(resultado);
    }

    @Test
    void dadasHorasConTresDecimales_cuandoAsigna_entonces400ConElCampoHoras() throws Exception {
        MvcResult resultado =
                asignar("MALAS-HORAS", "TDS", "140-b", "{\"horas\":4.125}", "TDS", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"campo\":\"horas\"", "dos decimales");
        contrato.validar(resultado);
    }

    @Test
    void dadas40Horas_cuandoAsigna_entoncesLasAcepta() throws Exception {
        assertThat(
                        asignar("2026-I", "TDS", "140-b", "{\"horas\":40}", "TDS", ESCRIBE)
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({
        "2026 I,TDS,140-b",
        "2026-I,tds,140-b",
        "2026-I,TDS,139-a",
        "2026-I,TDS,140-q",
        "2026-I,TDS,140b"
    })
    void dadoUnCodigoMalEscrito_cuandoAsigna_entonces400(
            String periodo, String carrera, String actividad) throws Exception {
        assertThat(
                        asignar(periodo, carrera, actividad, CUERPO, "TDS", ESCRIBE)
                                .getResponse()
                                .getStatus())
                .isEqualTo(400);
    }

    @Test
    void dadoUnProyectoMalEscrito_cuandoAsigna_entonces400() throws Exception {
        assertThat(
                        asignar(
                                        "FALLA",
                                        "TDS",
                                        "141-a",
                                        "{\"horas\":6,\"proyecto\":\"inv 1\"}",
                                        "TDS",
                                        ESCRIBE)
                                .getResponse()
                                .getStatus())
                .isEqualTo(400);
    }

    @Test
    void dadoElParcialDeOtraCarrera_cuandoAsigna_entonces403SinDatos() throws Exception {
        MvcResult resultado = asignar("2026-I", "AGRO", "140-b", CUERPO, "TDS", ESCRIBE);

        assertError(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoAsigna_entonces403QueDiceQuePidaSuCarrera()
            throws Exception {
        assertError(
                asignar("2026-I", "TDS", "140-b", CUERPO, null, ESCRIBE),
                403,
                "COMUN_CARRERA_SIN_INDICAR");
    }

    @Test
    void dadoQuienEscribeSobreTodo_cuandoAsigna_entonces200SinCarrera() throws Exception {
        assertThat(
                        asignar("2026-I", "AGRO", "140-b", CUERPO, null, "OFERTA:ESCRITURA")
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoQuienSoloLee_cuandoAsigna_entonces403() throws Exception {
        assertError(
                asignar("FALLA", "TDS", "140-b", CUERPO, "TDS", "DISTRIBUTIVO:LECTURA"),
                403,
                "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @ParameterizedTest
    @CsvSource({
        "NADA,404,GENERACION_PERIODO_NO_ENCONTRADO",
        "FUERA,404,GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "SIN-ABRIR,409,GENERACION_PERIODO_SIN_ABRIR",
        "SIN,409,GENERACION_SIN_DISTRIBUTIVO",
        "NO-ACTIVO,409,GENERACION_DOCENTE_NO_ACTIVO",
        "ENVIADO,409,GENERACION_PARCIAL_NO_EDITABLE",
        "PROYECTO,409,GENERACION_PROYECTO_NO_VALIDO",
        "CARGO,409,GENERACION_CARGO_NO_VIGENTE",
        "FALLA,502,COMUN_PROCESO_CON_ERROR",
        "LENTO,504,COMUN_PROCESO_SIN_RESPUESTA"
    })
    void dadoUnFallo_cuandoAsigna_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        assertError(asignar(periodo, "TDS", "140-b", CUERPO, "TDS", ESCRIBE), estado, codigo);
    }

    @Test
    void dadaUnaActividad_cuandoQuita_entonces204() throws Exception {
        MvcResult resultado = quitar("2026-I", "TDS", "INV-2025-01");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "SIN-ACTIVIDAD,404,GENERACION_ACTIVIDAD_NO_ENCONTRADA",
        "CERRADO,409,GENERACION_PERIODO_CERRADO",
        "ENVIADO,409,GENERACION_PARCIAL_NO_EDITABLE"
    })
    void dadoUnFallo_cuandoQuita_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        assertError(quitar(periodo, "TDS", null), estado, codigo);
    }

    @Test
    void dadoElParcialDeOtraCarrera_cuandoQuita_entonces403() throws Exception {
        assertError(quitar("2026-I", "AGRO", null), 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoUnProceso_cuandoVeLasPosibles_entonces200ConDisponibilidadYMotivo() throws Exception {
        MvcResult resultado = posibles("2026-I", ESCRIBE);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"docente\":\"0109990010\",\"actividades\":["
                                + "{\"actividad\":\"140-b\",\"nombre\":\"Planificar\","
                                + "\"componente\":\"OTRAS_DOCENCIA\",\"disponible\":true,"
                                + "\"exigeProyecto\":false,\"proyectos\":[]},"
                                + "{\"actividad\":\"141-a\",\"nombre\":\"Investigar\","
                                + "\"componente\":\"INVESTIGACION\",\"disponible\":true,"
                                + "\"exigeProyecto\":true,\"proyectos\":[{"
                                + "\"codigo\":\"INV-2025-01\",\"nombre\":\"Monitoreo\"}]},"
                                + "{\"actividad\":\"143-d\",\"nombre\":\"Coordinar\","
                                + "\"componente\":\"GESTION\",\"disponible\":false,"
                                + "\"exigeProyecto\":false,\"proyectos\":[],"
                                + "\"motivo\":\"No tiene el cargo\"}]}");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @CsvSource({
        "NO-ACTIVO,409,GENERACION_DOCENTE_NO_ACTIVO",
        "SIN-ABRIR,409,GENERACION_PERIODO_SIN_ABRIR",
        "FALLA,502,COMUN_PROCESO_CON_ERROR"
    })
    void dadoUnFallo_cuandoVeLasPosibles_entoncesResponde(String periodo, int estado, String codigo)
            throws Exception {
        assertError(posibles(periodo, ESCRIBE), estado, codigo);
    }

    @Test
    void dadoQuienNoEscribe_cuandoVeLasPosibles_entonces403() throws Exception {
        assertError(
                posibles("FALLA", "OFERTA:LECTURA:PROPIO"), 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadasActividades_cuandoLasVe_entonces200ConSuSuma() throws Exception {
        MvcResult resultado = actividades("2026-I", null, "DISTRIBUTIVO:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .startsWith("{\"creado\":true,\"carreras\":[{\"carrera\":\"AGRO\"")
                .contains(
                        "\"totales\":{\"clase\":8,\"otrasDocencia\":4.25,\"investigacion\":0,"
                                + "\"vinculacion\":0,\"gestion\":0}");
        contrato.validar(resultado);
    }

    @Test
    void dadoQueNoHayDistributivo_cuandoLasVe_entoncesCreadoEnFalso() throws Exception {
        MvcResult resultado = actividades("SIN-CREAR", null, "DISTRIBUTIVO:LECTURA");

        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("{\"creado\":false,\"carreras\":[]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinador_cuandoLasVe_entoncesSoloVeSuCarrera() throws Exception {
        assertThat(
                        actividades("2026-I", "TDS", "OFERTA:LECTURA:PROPIO")
                                .getResponse()
                                .getContentAsString())
                .contains("\"TDS\"")
                .doesNotContain("AGRO");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoLasVe_entonces403() throws Exception {
        assertError(
                actividades("2026-I", null, "OFERTA:LECTURA:PROPIO"),
                403,
                "COMUN_CARRERA_SIN_INDICAR");
    }

    private MvcResult asignar(
            String periodo,
            String carrera,
            String actividad,
            String cuerpo,
            String suCarrera,
            String... permisos)
            throws Exception {
        return mvc.perform(
                        put(RUTA.formatted(periodo, carrera, actividad))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(token(suCarrera, permisos)))
                .andReturn();
    }

    private MvcResult quitar(String periodo, String carrera, String proyecto) throws Exception {
        var pedido = delete(RUTA.formatted(periodo, carrera, "141-a")).with(token("TDS", ESCRIBE));
        return mvc.perform(proyecto == null ? pedido : pedido.param("proyecto", proyecto))
                .andReturn();
    }

    private MvcResult posibles(String periodo, String permiso) throws Exception {
        return mvc.perform(
                        get("/api/generacion/periodos/%s/docentes/0109990010/actividades-posibles"
                                        .formatted(periodo))
                                .with(token("TDS", permiso)))
                .andReturn();
    }

    private MvcResult actividades(String periodo, String carrera, String permiso) throws Exception {
        return mvc.perform(
                        get("/api/generacion/periodos/%s/actividades".formatted(periodo))
                                .with(token(carrera, permiso)))
                .andReturn();
    }

    private static RequestPostProcessor token(String carrera, String... permisos) {
        return jwt().jwt(
                        token -> {
                            token.subject("coordinacion.demo");
                            token.claim("permisos", List.of(permisos));
                            if (carrera != null) {
                                token.claim("carrera", carrera);
                            }
                        })
                .authorities(new AutoridadesDelToken());
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"" + codigo + "\"");
        contrato.validar(resultado);
    }

    /** Los casos de uso de las actividades: el periodo de la ruta elige la respuesta. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        private static void fallar(String periodo) {
            switch (periodo) {
                case "NADA" -> throw new PeriodoNoEncontrado();
                case "FUERA" -> throw new ActividadFueraDelCatalogo();
                case "CERRADO" -> throw new PeriodoCerrado();
                case "SIN-ABRIR" -> throw new PeriodoSinAbrir();
                case "SIN" -> throw new SinDistributivo();
                case "NO-ACTIVO" -> throw new DocenteNoActivo();
                case "ENVIADO" -> throw new ParcialNoEditable();
                case "PROYECTO" -> throw new ProyectoNoValido();
                case "CARGO" -> throw new CargoNoVigente();
                case "MALAS-HORAS" -> throw new HorasNoValidas();
                case "SIN-ACTIVIDAD" -> throw new ActividadNoEncontrada();
                case "FALLA" -> throw new ProcesoConError("motivo-interno");
                case "LENTO" -> throw new ProcesoSinRespuesta();
                default -> {
                    // Los demás responden.
                }
            }
        }

        private static void exigirCarrera(String carrera, Optional<String> propia) {
            if (propia.isPresent() && !propia.get().equals(carrera)) {
                throw new OtraCarrera();
            }
        }

        private static ActividadNoLectiva actividad(String carrera, String horas, String proyecto) {
            return new ActividadNoLectiva(
                    carrera,
                    "0109990010",
                    "PEREZ LUIS",
                    proyecto == null ? "140-b" : "141-a",
                    proyecto == null ? "Planificar" : "Investigar",
                    proyecto == null ? Componente.OTRAS_DOCENCIA : Componente.INVESTIGACION,
                    proyecto,
                    proyecto == null ? null : "Monitoreo",
                    new BigDecimal(horas),
                    Instant.parse("2026-11-01T15:00:00Z"),
                    "coordinacion.demo");
        }

        @Bean
        AsignarActividad asignarActividad() {
            return new AsignarActividad() {
                @Override
                public ActividadGuardada asignar(SolicitudDeActividad solicitud) {
                    exigirCarrera(solicitud.carrera(), solicitud.carreraPropia());
                    fallar(solicitud.periodo());
                    return new ActividadGuardada(
                            actividad(
                                    solicitud.carrera(),
                                    solicitud.horas().toPlainString(),
                                    solicitud.proyecto()),
                            false);
                }

                @Override
                public void quitar(ActividadQuitada quitada) {
                    exigirCarrera(quitada.carrera(), quitada.carreraPropia());
                    fallar(quitada.periodo());
                }
            };
        }

        @Bean
        ConsultarActividadesPosibles consultarActividadesPosibles() {
            return (periodo, docente) -> {
                fallar(periodo);
                return List.of(
                        new ActividadPosible(
                                new ActividadDelCatalogo(
                                        "140-b",
                                        "Planificar",
                                        Componente.OTRAS_DOCENCIA,
                                        List.of(),
                                        List.of()),
                                List.of(),
                                Optional.empty()),
                        new ActividadPosible(
                                new ActividadDelCatalogo(
                                        "141-a",
                                        "Investigar",
                                        Componente.INVESTIGACION,
                                        List.of("INVESTIGACION"),
                                        List.of()),
                                List.of(
                                        new ProyectoDelDocente(
                                                "INV-2025-01", "Monitoreo", "INVESTIGACION")),
                                Optional.empty()),
                        new ActividadPosible(
                                new ActividadDelCatalogo(
                                        "143-d",
                                        "Coordinar",
                                        Componente.GESTION,
                                        List.of(),
                                        List.of("COORDINADOR_CARRERA")),
                                List.of(),
                                Optional.of("No tiene el cargo")));
            };
        }

        @Bean
        ConsultarActividades consultarActividades() {
            return (periodo, carrera) -> {
                if ("SIN-CREAR".equals(periodo)) {
                    return new ActividadesDelPeriodo(false, List.of());
                }
                ActividadNoLectiva una = actividad("TDS", "4.25", null);
                ActividadesDeCarrera tds =
                        new ActividadesDeCarrera(
                                "TDS",
                                List.of(
                                        new ActividadesDelDocente(
                                                "0109990010",
                                                "PEREZ LUIS",
                                                List.of(una),
                                                new CargaHoraria(
                                                        new BigDecimal("8"),
                                                        new BigDecimal("4.25"),
                                                        BigDecimal.ZERO,
                                                        BigDecimal.ZERO,
                                                        BigDecimal.ZERO))));
                ActividadesDeCarrera agro = new ActividadesDeCarrera("AGRO", List.of());
                return new ActividadesDelPeriodo(
                        true, carrera.isPresent() ? List.of(tds) : List.of(agro, tds));
            };
        }
    }
}
