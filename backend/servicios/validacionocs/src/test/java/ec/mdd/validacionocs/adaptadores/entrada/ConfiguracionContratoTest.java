package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.puertos.entrada.CargarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Las tres operaciones del flujo cumplen {@code contratos/validacionocs/openapi.yaml}. */
@WebMvcTest(ConfiguracionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    ConfiguracionContratoTest.Dobles.class,
    ErroresValidacionocs.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ConfiguracionContratoTest {

    private static final String RUTA = "/api/validacionocs/periodos/%s/configuracion-flujo";
    private static final String PLANTILLA_RUTA = "/api/validacionocs/plantilla-flujo";
    private static final String LEER = "PERMISO_FLUJO_FIRMAS:LECTURA";
    private static final String ESCRIBIR = "PERMISO_FLUJO_FIRMAS:ESCRITURA";
    private static final String CODIGO = "$.codigo";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;

    private static RequestPostProcessor con(String permiso) {
        return jwt().authorities(new SimpleGrantedAuthority(permiso));
    }

    private MvcResult consultar(String periodo) throws Exception {
        return mvc.perform(get(RUTA.formatted(periodo)).with(con(LEER))).andReturn();
    }

    private MvcResult cargar(String periodo) throws Exception {
        return mvc.perform(
                        multipart(RUTA.formatted(periodo))
                                .file(
                                        new MockMultipartFile(
                                                "archivo",
                                                "flujo.yaml",
                                                "text/yaml",
                                                "contenido"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)))
                                .with(
                                        peticion -> {
                                            peticion.setMethod("PUT");
                                            return peticion;
                                        })
                                .with(con(ESCRIBIR)))
                .andReturn();
    }

    private void assertError(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentAsString()).contains(codigo);
        contrato.validar(resultado);
    }

    @Test
    void sinArchivo_cuandoConsulta_entonces200ConLaDePorDefecto() throws Exception {
        MvcResult resultado = consultar(Dobles.POR_DEFECTO);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo)
                .contains("\"porDefecto\":true")
                .contains("\"modificable\":true")
                .contains("\"tipo\":\"EN_OCS\",\"esqueleto\":true")
                .contains("\"tipo\":\"EN_REVISION_VRA\",\"esqueleto\":false")
                .contains("\"orden\":1,\"etiqueta\":\"Elaborado\"")
                .doesNotContain("\"version\"")
                .doesNotContain("\"cargada\"");
        contrato.validar(resultado);
    }

    @Test
    void conArchivo_cuandoConsulta_entonces200ConSuVersionQuienYCuando() throws Exception {
        MvcResult resultado = consultar(Dobles.CARGADO);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"porDefecto\":false")
                .contains("\"version\":2")
                .contains("\"por\":\"admin.demo\"")
                .contains("\"modificable\":false");
        contrato.validar(resultado);
    }

    @Test
    void dadaLaEtapaPublicado_cuandoConsulta_entoncesNoLlevaPlazo() throws Exception {
        String cuerpo = consultar(Dobles.POR_DEFECTO).getResponse().getContentAsString();

        assertThat(cuerpo.substring(cuerpo.indexOf("\"tipo\":\"PUBLICADO\"")))
                .startsWith("\"tipo\":\"PUBLICADO\"")
                .doesNotContain("\"plazoDias\":");
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoConsulta_entonces404() throws Exception {
        assertError(consultar(Dobles.NADA), 404, "VALIDACIONOCS_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void dadoUnCodigoMalEscrito_cuandoConsulta_entonces400() throws Exception {
        assertThat(consultar("un periodo!").getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void dadoUnArchivoValido_cuandoCarga_entonces200ConLaVigente() throws Exception {
        MvcResult resultado = cargar(Dobles.CARGADO);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"version\":2")
                .contains("\"por\":\"admin.demo\"");
        contrato.validar(resultado);
    }

    @Test
    void dadosVariosProblemas_cuandoCarga_entonces400ConUnDetallePorProblema() throws Exception {
        MvcResult resultado = cargar(Dobles.INVALIDO);

        assertError(resultado, 400, "VALIDACIONOCS_CONFIGURACION_INVALIDA");
        assertThat(resultado.getResponse().getContentAsString())
                .contains("Falta la etapa EN_OCS.")
                .contains("El rol RECTOR no tiene usuarios.");
    }

    @Test
    void dadoUnArchivoIlegible_cuandoCarga_entonces400() throws Exception {
        assertError(cargar(Dobles.ILEGIBLE), 400, "VALIDACIONOCS_ARCHIVO_ILEGIBLE");
    }

    @Test
    void dadoUnArchivoGrande_cuandoCarga_entonces413() throws Exception {
        assertError(cargar(Dobles.GRANDE), 413, "VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE");
    }

    @Test
    void dadoUnPeriodoEnCurso_cuandoCarga_entonces409() throws Exception {
        assertError(cargar(Dobles.EN_CURSO), 409, "VALIDACIONOCS_PERIODO_SIN_PLANIFICACION");
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoCarga_entonces404() throws Exception {
        assertError(cargar(Dobles.NADA), 404, "VALIDACIONOCS_PERIODO_NO_ENCONTRADO");
    }

    @Test
    void conPermisoDeLectura_cuandoDescargaLaPlantilla_entoncesEsElArchivoComentado()
            throws Exception {
        MvcResult resultado = mvc.perform(get(PLANTILLA_RUTA).with(con(LEER))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).startsWith("text/yaml");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("flujo-por-defecto.yaml");
        assertThat(
                        resultado
                                .getResponse()
                                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("# Configuración del flujo");
    }

    @Test
    void conSoloLectura_cuandoCarga_entonces403() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                multipart(RUTA.formatted(Dobles.CARGADO))
                                        .file(new MockMultipartFile("archivo", new byte[] {1}))
                                        .with(
                                                peticion -> {
                                                    peticion.setMethod("PUT");
                                                    return peticion;
                                                })
                                        .with(con(LEER)))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void sinPermiso_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get(RUTA.formatted(Dobles.CARGADO))
                                        .with(con("PERMISO_DISTRIBUTIVO:LECTURA")))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void sinToken_cuandoConsulta_entonces401() throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA.formatted(Dobles.CARGADO))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        static final String POR_DEFECTO = "2026-I";
        static final String CARGADO = "2026-II";
        static final String NADA = "NADA";
        static final String INVALIDO = "INVALIDO";
        static final String ILEGIBLE = "ILEGIBLE";
        static final String GRANDE = "GRANDE";
        static final String EN_CURSO = "EN-CURSO";

        private static final String PLANTILLA = new PlantillaEnRecursos().texto();
        private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

        private static ConfiguracionFlujo porDefecto() {
            return ConfiguracionFlujo.revisar(new LectorYaml().leer(PLANTILLA))
                    .configuracion()
                    .orElseThrow();
        }

        private static ConfiguracionVigente cargada(boolean modificable) {
            return new ConfiguracionVigente(
                    CARGADO,
                    porDefecto(),
                    Optional.of(new CargaGuardada(2, PLANTILLA, "admin.demo", AHORA)),
                    modificable);
        }

        @Bean
        CargarConfiguracion cargarConfiguracion() {
            return (periodo, archivo, quien) ->
                    switch (periodo) {
                        case NADA -> throw new PeriodoNoEncontrado();
                        case EN_CURSO -> throw new PeriodoSinPlanificacion();
                        case ILEGIBLE -> throw new ArchivoIlegible(3);
                        case GRANDE -> throw new ArchivoDemasiadoGrande();
                        case INVALIDO ->
                                throw new ConfiguracionInvalida(
                                        List.of(
                                                "Falta la etapa EN_OCS.",
                                                "El rol RECTOR no tiene usuarios."));
                        default -> cargada(true);
                    };
        }

        @Bean
        ConsultarConfiguracion consultarConfiguracion() {
            return new ConsultarConfiguracion() {
                @Override
                public ConfiguracionVigente dePeriodo(String periodo) {
                    return switch (periodo) {
                        case NADA -> throw new PeriodoNoEncontrado();
                        case CARGADO -> cargada(false);
                        default ->
                                new ConfiguracionVigente(
                                        periodo, porDefecto(), Optional.empty(), true);
                    };
                }

                @Override
                public String plantilla() {
                    return PLANTILLA;
                }
            };
        }
    }
}
