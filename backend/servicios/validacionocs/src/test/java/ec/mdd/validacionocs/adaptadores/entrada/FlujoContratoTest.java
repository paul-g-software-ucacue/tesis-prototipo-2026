package ec.mdd.validacionocs.adaptadores.entrada;

import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.CERRADO;
import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.CON_UNIDADES;
import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.DS;
import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.PERIODO;
import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.SIN_DISTRIBUTIVO;
import static ec.mdd.validacionocs.adaptadores.entrada.FlujoEnMemoria.TDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * El flujo del distributivo cumple {@code contratos/validacionocs/openapi.yaml} (F12-H02,
 * contracts/validacionocs.md): las tres operaciones con el servicio real y sus puertos en memoria,
 * sus errores, y el 403 de los roles de la matriz D-16 que no son el de la etapa (SC-002).
 */
@WebMvcTest(FlujoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({FlujoContratoTest.Dobles.class, ErroresValidacionocs.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class FlujoContratoTest {

    private static final String FLUJO = "/api/validacionocs/periodos/%s/flujo";
    private static final String ENVIO = "/api/validacionocs/periodos/%s/parciales/%s/envio";
    private static final String AVANCE = "/api/validacionocs/periodos/%s/avance";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map<String, List<String>> MATRIZ = matrizD16();
    private static final FlujoEnMemoria MEMORIA = new FlujoEnMemoria();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;
    @Autowired private JwtAuthenticationConverter conversor;

    @BeforeEach
    void reiniciar() {
        MEMORIA.reiniciar();
    }

    @Test
    void dadoElVicerrector_cuandoConsulta_entonces200EnBorradorCarreraSegunElContrato()
            throws Exception {
        MvcResult resultado = pedir(get(FLUJO.formatted(PERIODO)), "VICERRECTOR", null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = cuerpo(resultado);
        assertThat(cuerpo.get("etapa").asString()).isEqualTo("BORRADOR_CARRERA");
        assertThat(cuerpo.get("nombreDeEtapa").asString()).isEqualTo("Propuesta de carrera");
        assertThat(cuerpo.get("parciales")).hasSize(2);
        assertThat(cuerpo.get("acciones")).isEmpty();
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "PERMISO_DISTRIBUTIVO:LECTURA",
                "PERMISO_OFERTA:LECTURA",
                "PERMISO_PROYECTOS:LECTURA"
            })
    void dadoCadaPermisoDeLectura_cuandoConsulta_entonces200(String permiso) throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get(FLUJO.formatted(PERIODO))
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        permiso))))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void dadoSinNingunoDeLosTresPermisos_cuandoConsulta_entonces403() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                get(FLUJO.formatted(PERIODO))
                                        .with(
                                                jwt().authorities(
                                                                new SimpleGrantedAuthority(
                                                                        "PERMISO_CARGAS:LECTURA"))))
                        .andReturn();

        assertCodigo(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
    }

    @Test
    void dadoElCoordinador_cuandoEnviaSuParcial_entonces200EnviadoConSuTransicion()
            throws Exception {
        MvcResult resultado = enviar(PERIODO, DS, DS);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = cuerpo(resultado);
        assertThat(cuerpo.get("parciales").get(0).get("carrera").asString()).isEqualTo(DS);
        assertThat(cuerpo.get("parciales").get(0).get("enviado").asBoolean()).isTrue();
        assertThat(cuerpo.get("parciales").get(0).get("enviadoPor").asString())
                .isEqualTo("coordinacion.demo");
        JsonNode transicion = cuerpo.get("transiciones").get(0);
        assertThat(transicion.get("tipo").asString()).isEqualTo("ENVIO_PARCIAL");
        assertThat(transicion.get("hash").asString()).matches("^[0-9a-f]{64}$");
        contrato.validar(resultado);
    }

    @Test
    void dadoElUltimoParcial_cuandoSeEnvia_entoncesLaRespuestaYaEstaEnLaSiguienteEtapa()
            throws Exception {
        enviar(PERIODO, DS, DS);

        MvcResult resultado = enviar(PERIODO, TDS, TDS);

        assertThat(cuerpo(resultado).get("etapa").asString()).isEqualTo("EN_CONSOLIDACION");
        assertThat(cuerpo(resultado).get("transiciones")).hasSize(3);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnParcialYaEnviado_cuandoSeEnviaOtraVez_entonces409() throws Exception {
        enviar(PERIODO, DS, DS);

        assertCodigo(enviar(PERIODO, DS, DS), 409, "VALIDACIONOCS_PARCIAL_YA_ENVIADO");
    }

    @Test
    void dadoElParcialDeOtraCarrera_cuandoSeEnvia_entonces403ComoSinPermiso() throws Exception {
        MvcResult resultado = enviar(PERIODO, TDS, DS);

        assertCodigo(resultado, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain(TDS)
                .doesNotContain("COORDINADOR");
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoEnvia_entonces403CarreraSinIndicar() throws Exception {
        assertCodigo(enviar(PERIODO, DS, null), 403, "COMUN_CARRERA_SIN_INDICAR");
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeEnviaOConsulta_entonces404() throws Exception {
        assertCodigo(enviar(SIN_DISTRIBUTIVO, DS, DS), 404, "VALIDACIONOCS_SIN_DISTRIBUTIVO");
        assertCodigo(
                pedir(get(FLUJO.formatted(SIN_DISTRIBUTIVO)), "VICERRECTOR", null),
                404,
                "VALIDACIONOCS_SIN_DISTRIBUTIVO");
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeEnvia_entonces404ParcialNoEncontrado() throws Exception {
        assertCodigo(
                enviar(PERIODO, "550732E01-D-0301", "550732E01-D-0301"),
                404,
                "VALIDACIONOCS_PARCIAL_NO_ENCONTRADO");
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeEnvia_entonces409PeriodoCerrado() throws Exception {
        assertCodigo(enviar(CERRADO, DS, DS), 409, "VALIDACIONOCS_PERIODO_CERRADO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ds-minusculas", "CON ESPACIO", "123456789012345678901"})
    void dadaUnaCarreraInvalida_cuandoSeEnvia_entonces400(String carrera) throws Exception {
        assertCodigo(enviar(PERIODO, carrera, DS), 400, "COMUN_SOLICITUD_INVALIDA");
    }

    @Test
    void dadoBorradorCarrera_cuandoSeUsaElAvanceGenerico_entonces409OperacionPropia()
            throws Exception {
        assertCodigo(
                pedir(post(AVANCE.formatted(PERIODO)), "COORDINADOR_CARRERA", DS),
                409,
                "VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA");
    }

    @Test
    void dadoBorradorUnidades_cuandoElGestorAvanza_entonces200EnConsolidacion() throws Exception {
        enviar(CON_UNIDADES, DS, DS);

        MvcResult resultado = pedir(post(AVANCE.formatted(CON_UNIDADES)), "GESTOR_UNIDAD", null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(resultado).get("etapa").asString()).isEqualTo("EN_CONSOLIDACION");
        contrato.validar(resultado);
    }

    // SC-002 y T016: los 9 roles que no son el de la etapa reciben 403, en las dos operaciones.
    @Test
    void dadosLosRolesQueNoSonElDeLaEtapa_cuandoEnvianOAvanzan_entonces403YElDeLaEtapa200()
            throws Exception {
        assertThat(MATRIZ).hasSize(10);
        List<String> fallas = new ArrayList<>();
        for (String rol : MATRIZ.keySet()) {
            MEMORIA.reiniciar();
            int envio = enviarComo(rol).getResponse().getStatus();
            if (envio != (rol.equals("COORDINADOR_CARRERA") ? 200 : 403)) {
                fallas.add(rol + " al enviar: " + envio);
            }
            MEMORIA.reiniciar();
            enviar(CON_UNIDADES, DS, DS);
            MvcResult avance = pedir(post(AVANCE.formatted(CON_UNIDADES)), rol, null);
            int esperado = rol.equals("GESTOR_UNIDAD") ? 200 : 403;
            if (avance.getResponse().getStatus() != esperado) {
                fallas.add(rol + " al avanzar: " + avance.getResponse().getStatus());
            } else if (esperado == 403) {
                assertThat(cuerpo(avance).get("codigo").asString())
                        .isEqualTo("SEGURIDAD_ACCESO_NO_AUTORIZADO");
            }
        }
        assertThat(fallas).isEmpty();
    }

    private MvcResult enviarComo(String rol) throws Exception {
        return pedir(
                post(ENVIO.formatted(PERIODO, DS)),
                rol,
                rol.equals("COORDINADOR_CARRERA") ? DS : null);
    }

    private MvcResult enviar(String periodo, String carrera, String carreraDelCoordinador)
            throws Exception {
        return pedir(
                post(ENVIO.formatted(periodo, carrera)),
                "COORDINADOR_CARRERA",
                carreraDelCoordinador);
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion, String rol, String carrera)
            throws Exception {
        return mvc.perform(peticion.with(token(rol, carrera))).andReturn();
    }

    // El token como lo arma Usuarios: sus roles, sus permisos de la matriz y su carrera.
    private RequestPostProcessor token(String rol, String carrera) {
        return jwt().jwt(
                        jwt -> {
                            jwt.subject(
                                            rol.equals("COORDINADOR_CARRERA")
                                                    ? "coordinacion.demo"
                                                    : rol.toLowerCase() + ".demo")
                                    .claim("roles", List.of(rol))
                                    .claim("permisos", MATRIZ.get(rol));
                            if (carrera != null) {
                                jwt.claim("carrera", carrera);
                            }
                        })
                .authorities(
                        (Jwt jwt) ->
                                java.util.Objects.requireNonNull(conversor.convert(jwt))
                                        .getAuthorities());
    }

    private static void assertCodigo(MvcResult resultado, int estado, String codigo)
            throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
        assertThat(cuerpo(resultado).get("codigo").asString()).isEqualTo(codigo);
    }

    private static JsonNode cuerpo(MvcResult resultado) throws Exception {
        return JSON.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    // Los permisos reales de cada rol, con su alcance, de la migración de Roles (V2).
    private static Map<String, List<String>> matrizD16() {
        // Validación OCS no depende de Roles: se lee el archivo del repositorio, desde el módulo.
        String sql;
        try {
            String todo =
                    Files.readString(
                            Path.of(
                                    "../roles/src/main/resources/db/migration/roles/"
                                            + "V2__matriz_d16.sql"));
            sql = todo.substring(todo.indexOf("insert into rol_permiso"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la matriz D-16 de Roles", e);
        }
        Matcher fila =
                Pattern.compile("\\('([A-Z_]+)', '([A-Z_]+)', '([A-Z]+)', '([A-Z]+)'\\)")
                        .matcher(sql);
        Map<String, List<String>> matriz = new TreeMap<>();
        while (fila.find()) {
            String permiso =
                    fila.group(2)
                            + ":"
                            + fila.group(3)
                            + ("PROPIO".equals(fila.group(4)) ? ":PROPIO" : "");
            matriz.computeIfAbsent(fila.group(1), rol -> new ArrayList<>()).add(permiso);
        }
        return matriz;
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        FlujoDelDistributivoServicio flujoDelDistributivo() {
            return MEMORIA.servicio();
        }
    }
}
