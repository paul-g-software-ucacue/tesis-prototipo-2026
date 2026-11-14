package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.usuarios.aplicacion.ConsultarBitacoraServicio;
import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.ConsultarBitacora;
import ec.mdd.usuarios.puertos.entrada.EntradaConsultada;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * La consulta de la bitácora cumple el contrato de Usuarios (F17-H02, {@code
 * contracts/usuarios.md}): cada parámetro en sus límites, los 400 con su detalle, y solo los roles
 * con {@code BITACORA:LECTURA} de la matriz D-16 la consultan (FR-007, FR-008, FR-010, SC-002).
 */
@WebMvcTest(BitacoraControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({BitacoraContratoTest.Dobles.class, ErroresUsuarios.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class BitacoraContratoTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T20:00:00Z");
    private static final String CONSULTA = "/api/usuarios/bitacora";
    private static final String ACCIONES = "/api/usuarios/bitacora/acciones";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map<String, List<String>> MATRIZ = matrizD16();

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("usuarios");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElVicerrector_cuandoConsulta_entonces200ConLaPaginaSegunElContrato() throws Exception {
        MvcResult resultado =
                mvc.perform(
                                conRol(
                                        get(CONSULTA)
                                                .param("usuario", "coordinacion.demo")
                                                .param("desde", "2026-11-07")
                                                .param("hasta", "2026-11-07"),
                                        "VICERRECTOR"))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = JSON.readTree(resultado.getResponse().getContentAsString());
        assertThat(cuerpo.get("total").asLong()).isEqualTo(1);
        assertThat(cuerpo.get("pagina").asInt()).isEqualTo(1);
        assertThat(cuerpo.get("tamano").asInt()).isEqualTo(50);
        assertThat(cuerpo.get("corte").asString()).isEqualTo("2026-11-07T20:00:00Z");
        JsonNode entrada = cuerpo.get("entradas").get(0);
        assertThat(entrada.get("servicio").asString()).isEqualTo("generacion");
        assertThat(entrada.get("accion").asString()).isEqualTo("ACTIVIDAD_GUARDADA");
        assertThat(entrada.get("anterior").get("horas").asString()).isEqualTo("4");
        assertThat(entrada.get("nuevo").get("horas").asString()).isEqualTo("6");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnaEntradaQueCrea_cuandoSeConsulta_entoncesNoTraeAnterior() throws Exception {
        MvcResult resultado =
                mvc.perform(conRol(get(CONSULTA).param("periodo", "CREA"), "ADMINISTRADOR"))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode entrada =
                JSON.readTree(resultado.getResponse().getContentAsString()).get("entradas").get(0);
        assertThat(entrada.has("anterior")).isFalse();
        assertThat(entrada.has("periodo")).isFalse();
        assertThat(entrada.get("nuevo").get("horas").asString()).isEqualTo("2");
        contrato.validar(resultado);
    }

    @Test
    void dadoElAdministrador_cuandoPideLasAcciones_entonces200SegunElContrato() throws Exception {
        MvcResult resultado = mvc.perform(conRol(get(ACCIONES), "ADMINISTRADOR")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo("[{\"servicio\":\"generacion\",\"accion\":\"ACTIVIDAD_GUARDADA\"}]");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "x' or '1'='1"})
    void dadoUnUsuarioEnSusLimites_cuandoSeConsulta_entonces200(String usuario) throws Exception {
        assertThat(estado(get(CONSULTA).param("usuario", usuario))).isEqualTo(200);
        assertThat(estado(get(CONSULTA).param("usuario", "u".repeat(64)))).isEqualTo(200);
    }

    @ParameterizedTest
    @MethodSource("invalidos")
    void dadoUnParametroInvalido_cuandoSeConsulta_entonces400ConElDetalleDeSuCampo(
            String parametro, String valor, String campo) throws Exception {
        MvcResult resultado = consultar(get(CONSULTA).param(parametro, valor));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        JsonNode cuerpo = JSON.readTree(resultado.getResponse().getContentAsString());
        assertThat(cuerpo.get("codigo").asString()).isEqualTo("COMUN_SOLICITUD_INVALIDA");
        assertThat(cuerpo.get("detalles").findValuesAsString("campo")).contains(campo);
        contrato.validar(resultado);
    }

    static List<org.junit.jupiter.params.provider.Arguments> invalidos() {
        return List.of(
                argumentos("usuario", "", "usuario"),
                argumentos("usuario", "u".repeat(65), "usuario"),
                argumentos("identificador", "tildé", "identificador"),
                argumentos("identificador", "con espacio", "identificador"),
                argumentos("identificador", "a".repeat(65), "identificador"),
                argumentos("periodo", "p".repeat(21), "periodo"),
                argumentos("periodo", "2026 I", "periodo"),
                argumentos("accion", "minusculas", "accion"),
                argumentos("tamano", "0", "tamano"),
                argumentos("tamano", "101", "tamano"),
                argumentos("pagina", "0", "pagina"));
    }

    // Un valor fuera del enumerado en la consulta lo responde la plataforma, sin detalle: solo los
    // cuerpos llevan el detalle de ValorNoPermitido. La SPA elige el servicio de una lista.
    @Test
    void dadoUnServicioFueraDeLaLista_cuandoSeConsulta_entonces400SolicitudInvalida()
            throws Exception {
        MvcResult resultado = consultar(get(CONSULTA).param("servicio", "usuarios"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(
                        JSON.readTree(resultado.getResponse().getContentAsString())
                                .get("codigo")
                                .asString())
                .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        contrato.validar(resultado);
    }

    // El valor del contrato, en minúsculas, no el nombre del enumerado generado (GENERACION).
    @ParameterizedTest
    @ValueSource(
            strings = {
                "generacion",
                "academico",
                "docentes",
                "normativa",
                "roles",
                "validacionocs",
                "firma"
            })
    void dadoUnServicioDeLaLista_cuandoSeConsultaConSuAccion_entonces200(String servicio)
            throws Exception {
        assertThat(estado(get(CONSULTA).param("servicio", servicio).param("accion", "ALGO_HECHO")))
                .isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOC/0102030405", "140-b"})
    void dadoUnIdentificadorConBarraOGuion_cuandoSeConsulta_entonces200(String identificador)
            throws Exception {
        assertThat(estado(get(CONSULTA).param("identificador", identificador))).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void dadoUnTamanoEnSusLimites_cuandoSeConsulta_entonces200(int tamano) throws Exception {
        assertThat(estado(get(CONSULTA).param("tamano", String.valueOf(tamano)))).isEqualTo(200);
    }

    @Test
    void dadoUnaAccionSinServicio_cuandoSeConsulta_entonces400EnAccion() throws Exception {
        MvcResult resultado = consultar(get(CONSULTA).param("accion", "ACTIVIDAD_GUARDADA"));

        assertThat(detalle(resultado)).isEqualTo("accion");
        contrato.validar(resultado);
    }

    @Test
    void dadoHastaAntesDeDesde_cuandoSeConsulta_entonces400EnHastaConSuMotivo() throws Exception {
        MvcResult resultado =
                consultar(get(CONSULTA).param("desde", "2026-11-07").param("hasta", "2026-11-06"));

        assertThat(detalle(resultado)).isEqualTo("hasta");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("La fecha hasta no puede ser anterior a la fecha desde.");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCorteFuturo_cuandoSeConsulta_entonces400EnCorte() throws Exception {
        MvcResult resultado = consultar(get(CONSULTA).param("corte", "2026-11-07T20:00:01Z"));

        assertThat(detalle(resultado)).isEqualTo("corte");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @ValueSource(strings = {CONSULTA, ACCIONES})
    void dadoLosRolesConLecturaDeLaBitacora_cuandoConsultan_entonces200(String ruta)
            throws Exception {
        assertThat(MATRIZ.entrySet())
                .filteredOn(rol -> rol.getValue().contains("PERMISO_BITACORA:LECTURA"))
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("ADMINISTRADOR", "VICERRECTOR");
        for (String rol : List.of("ADMINISTRADOR", "VICERRECTOR")) {
            assertThat(mvc.perform(conRol(get(ruta), rol)).andReturn().getResponse().getStatus())
                    .as(rol)
                    .isEqualTo(200);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "OPERADOR_DATOS",
                "COORDINADOR_CARRERA",
                "GESTOR_UNIDAD",
                "RESPONSABLE_CONSOLIDACION",
                "RECTOR",
                "MIEMBRO_OCS",
                "SECRETARIA_GENERAL",
                "DOCENTE"
            })
    void dadoUnRolSinLecturaDeLaBitacora_cuandoConsulta_entonces403SinDecirQuePermisoFalta(
            String rol) throws Exception {
        for (String ruta : List.of(CONSULTA, ACCIONES)) {
            MvcResult resultado = mvc.perform(conRol(get(ruta), rol)).andReturn();

            assertThat(resultado.getResponse().getStatus()).as(rol + " " + ruta).isEqualTo(403);
            String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(JSON.readTree(cuerpo).get("codigo").asString())
                    .isEqualTo("SEGURIDAD_ACCESO_NO_AUTORIZADO");
            assertThat(cuerpo).doesNotContain("BITACORA").doesNotContain("LECTURA");
            contrato.validar(resultado);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {CONSULTA, ACCIONES})
    void dadoSinSesion_cuandoConsulta_entonces401(String ruta) throws Exception {
        MvcResult resultado = mvc.perform(get(ruta)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        contrato.validar(resultado);
    }

    private int estado(MockHttpServletRequestBuilder peticion) throws Exception {
        return consultar(peticion).getResponse().getStatus();
    }

    private MvcResult consultar(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(conRol(peticion, "VICERRECTOR")).andReturn();
    }

    private static String detalle(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        JsonNode cuerpo = JSON.readTree(resultado.getResponse().getContentAsString());
        assertThat(cuerpo.get("codigo").asString()).isEqualTo("COMUN_SOLICITUD_INVALIDA");
        return cuerpo.get("detalles").get(0).get("campo").asString();
    }

    private static MockHttpServletRequestBuilder conRol(
            MockHttpServletRequestBuilder peticion, String rol) {
        SimpleGrantedAuthority[] permisos =
                MATRIZ.get(rol).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toArray(SimpleGrantedAuthority[]::new);
        return peticion.with(jwt().authorities(permisos));
    }

    private static org.junit.jupiter.params.provider.Arguments argumentos(Object... valores) {
        return org.junit.jupiter.params.provider.Arguments.of(valores);
    }

    // Los permisos reales de cada rol, leídos de la migración de Roles, no escritos a mano (T016).
    private static Map<String, List<String>> matrizD16() {
        String sql;
        try (InputStream entrada =
                BitacoraContratoTest.class
                        .getClassLoader()
                        .getResourceAsStream("db/migration/roles/V2__matriz_d16.sql")) {
            String todo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            sql = todo.substring(todo.indexOf("insert into rol_permiso"));
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("No se encontró la matriz D-16 de Roles", e);
        }
        Matcher fila =
                Pattern.compile("\\('([A-Z_]+)', '([A-Z_]+)', '([A-Z]+)', '([A-Z]+)'\\)")
                        .matcher(sql);
        Map<String, TreeSet<String>> permisos = new TreeMap<>();
        while (fila.find()) {
            permisos.computeIfAbsent(fila.group(1), rol -> new TreeSet<>())
                    .add("PERMISO_" + fila.group(2) + ":" + fila.group(3));
        }
        Map<String, List<String>> matriz = new TreeMap<>();
        permisos.forEach((rol, suyos) -> matriz.put(rol, List.copyOf(suyos)));
        if (matriz.size() != 10) {
            throw new IllegalStateException("La matriz D-16 tiene 10 roles: " + matriz.keySet());
        }
        return matriz;
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        ConsultarBitacora consultarBitacora() {
            return new ConsultarBitacoraServicio(
                    new BitacoraFija(), Clock.fixed(AHORA, ZoneOffset.UTC));
        }
    }

    /** Una bitácora con una entrada de 4 a 6 horas, o una que crea si se filtra por «CREA». */
    private static final class BitacoraFija implements RepositorioBitacora {

        @Override
        public void registrar(EntradaBitacora entrada) {
            throw new UnsupportedOperationException("La consulta no registra");
        }

        @Override
        public PaginaDeEntradas consultar(FiltroDeBitacora filtro, PaginaPedida pagina) {
            boolean crea = filtro.periodo().filter("CREA"::equals).isPresent();
            EntradaConsultada entrada =
                    new EntradaConsultada(
                            UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f"),
                            "generacion",
                            "coordinacion.demo",
                            "ACTIVIDAD_GUARDADA",
                            Instant.parse("2026-11-07T15:00:00Z"),
                            crea ? Optional.empty() : Optional.of("2026-I"),
                            Map.of("actividad", "140-b", "docente", "0102030405"),
                            crea ? Optional.empty() : Optional.of(Map.of("horas", "4")),
                            Optional.of(Map.of("horas", crea ? "2" : "6")));
            return new PaginaDeEntradas(
                    List.of(entrada), 1, pagina.pagina(), pagina.tamano(), pagina.corte());
        }

        @Override
        public List<AccionConocida> acciones() {
            return List.of(new AccionConocida("generacion", "ACTIVIDAD_GUARDADA"));
        }
    }
}
