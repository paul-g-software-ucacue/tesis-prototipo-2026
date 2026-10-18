package ec.mdd.plataforma.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.pruebas.ServidorJwks;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import ec.mdd.plataforma.pruebas.VerificadorDeCatalogo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * La seguridad de los procesos con un servidor real y un JWKS servido por la prueba: cada causa de
 * rechazo da el mismo 401, y un token válido llega con sus roles (escenarios «Petición sin token» y
 * «Token alterado o vencido», ADR-014). Desde F05-H02, los permisos del token son autoridades y
 * cada acceso denegado responde el mismo 403 con cuerpo (ADR-021).
 */
@SpringBootTest(
        classes = AplicacionDePrueba.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.endpoints.web.exposure.include=health",
            "mdd.seguridad.rutas-publicas[0]=POST /api/prueba/sesion"
        })
class SeguridadProcesoTest {

    private static final String CUERPO_401 =
            "{\"titulo\":\"Sesión no válida\",\"descripcion\":\"Inicia sesión de nuevo para"
                    + " continuar.\",\"codigo\":\"SEGURIDAD_NO_AUTENTICADO\"}";

    private static final String CUERPO_403 =
            "{\"titulo\":\"Acceso no autorizado\",\"descripcion\":\"No tienes permiso para esta"
                    + " acción. Si crees que deberías tenerlo, pide al administrador que revise tus"
                    + " roles.\",\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"}";

    private static final TokensDePrueba TOKENS = new TokensDePrueba();
    private static final ServidorJwks JWKS = arrancarJwks();

    private final HttpClient cliente = HttpClient.newHttpClient();

    @LocalServerPort private int puerto;

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registro) {
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::uri);
    }

    @AfterAll
    static void cerrar() {
        JWKS.close();
    }

    static List<String> autorizacionesInvalidas() {
        return List.of(
                "",
                "Basic dXN1YXJpbzpjbGF2ZQ==",
                "Bearer %%%",
                "Bearer no.es.un-token",
                "Bearer " + TOKENS.alterado(),
                "Bearer " + TOKENS.vencido(),
                "Bearer " + TOKENS.deOtraLlave(),
                "Bearer " + TOKENS.deOtroEmisor(),
                "Bearer " + TOKENS.hs256(),
                "Bearer " + TOKENS.sinAlgoritmo());
    }

    @ParameterizedTest
    @MethodSource("autorizacionesInvalidas")
    void dadoUnTokenAusenteOInvalido_cuandoLlegaAlProceso_entoncesResponde401SiempreIgual(
            String autorizacion) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sesion", autorizacion);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(CUERPO_401);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(respuesta.headers().allValues("WWW-Authenticate")).containsExactly("Bearer");
    }

    @Test
    void dadoUnTokenValido_cuandoLlegaAlProceso_entoncesObtieneElUsuarioYSusRoles()
            throws Exception {
        String token = TOKENS.valido("autor.prueba", List.of("DOCENTE", "COORDINADOR_CARRERA"));

        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sesion", "Bearer " + token);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body())
                .isEqualTo("autor.prueba|ROLE_COORDINADOR_CARRERA,ROLE_DOCENTE");
    }

    @Test
    void dadoUnaRutaPublicaConMetodo_cuandoSeLlamaSinToken_entoncesSoloEseMetodoPasa()
            throws Exception {
        assertThat(pedir("POST", "/api/prueba/sesion", "").statusCode()).isEqualTo(200);
        assertThat(pedir("GET", "/api/prueba/sesion", "").statusCode()).isEqualTo(401);
    }

    @Test
    void dadoUnaPeticionDirectaAError_cuandoNoTraeToken_entoncesResponde401() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/error", "");

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(CUERPO_401);
    }

    @Test
    void dadoElCuerpoDel401_cuandoSeComparaConElCatalogo_entoncesEsElDeSeguridadNoAutenticado() {
        DefinicionDeCodigo codigo = CodigosComunes.SEGURIDAD_NO_AUTENTICADO;

        assertThat(CUERPO_401)
                .isEqualTo(
                        "{\"titulo\":\""
                                + codigo.titulo()
                                + "\",\"descripcion\":\""
                                + codigo.descripcion()
                                + "\",\"codigo\":\""
                                + codigo.codigo()
                                + "\"}");
        VerificadorDeCatalogo.verificar(java.util.List.of(codigo));
    }

    @Test
    void dadoUnTokenDeProceso_cuandoLlegaAUnaRutaInterna_entoncesPasa() throws Exception {
        String token = TOKENS.valido("proceso:distributivo", List.of("PROCESO"));

        HttpResponse<String> respuesta =
                pedir("POST", "/interno/prueba/eventos", "Bearer " + token);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("interno|proceso:distributivo");
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoLlegaAUnaRutaInterna_entoncesResponde403ConCuerpo()
            throws Exception {
        String token = TOKENS.valido("autor.prueba", List.of("DOCENTE"));

        assertEs403(pedir("POST", "/interno/prueba/eventos", "Bearer " + token));
    }

    @Test
    void dadoUnTokenDeProceso_cuandoLlegaAUnaRutaDeLaApi_entoncesResponde403ConCuerpo()
            throws Exception {
        String token = TOKENS.valido("proceso:distributivo", List.of("PROCESO"));

        assertEs403(pedir("GET", "/api/prueba/recurso", "Bearer " + token));
        assertEs403(pedir("GET", "/api/prueba/sesion", "Bearer " + token));
    }

    @Test
    void dadoUnTokenConPermisos_cuandoLlegaAlProceso_entoncesSonAutoridadesSinElAlcance()
            throws Exception {
        String token =
                TOKENS.conPermisos(
                        "docente.demo",
                        List.of("DOCENTE"),
                        List.of("USUARIOS_ROLES:LECTURA", "OFERTA:LECTURA:PROPIO"));

        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sesion", "Bearer " + token);

        assertThat(respuesta.body())
                .isEqualTo(
                        "docente.demo|PERMISO_OFERTA:LECTURA,PERMISO_USUARIOS_ROLES:LECTURA,"
                                + "ROLE_DOCENTE");
    }

    @Test
    void dadoUnTokenSinElPermiso_cuandoLlamaAUnaOperacionProtegida_entoncesResponde403ConCuerpo()
            throws Exception {
        String token =
                TOKENS.conPermisos("docente.demo", List.of("DOCENTE"), List.of("OFERTA:LECTURA"));

        assertEs403(pedir("GET", "/api/prueba/protegido", "Bearer " + token));
    }

    @Test
    void dadoUnTokenConElPermiso_cuandoLlamaAUnaOperacionProtegida_entoncesPasa() throws Exception {
        String token =
                TOKENS.conPermisos(
                        "admin.demo", List.of("ADMINISTRADOR"), List.of("USUARIOS_ROLES:LECTURA"));

        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/protegido", "Bearer " + token);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("protegido");
    }

    @Test
    void dadoUnTokenSinElPermisoYUnCuerpoIlegible_cuandoEscribe_entoncesEl403LlegaAntesQueEl400()
            throws Exception {
        String sinPermiso =
                TOKENS.conPermisos(
                        "docente.demo", List.of("DOCENTE"), List.of("USUARIOS_ROLES:LECTURA"));
        String conPermiso =
                TOKENS.conPermisos(
                        "admin.demo",
                        List.of("ADMINISTRADOR"),
                        List.of("USUARIOS_ROLES:ESCRITURA"));

        assertEs403(pedirConCuerpo("/api/prueba/protegido", "Bearer " + sinPermiso, "no es json"));
        assertThat(
                        pedirConCuerpo(
                                        "/api/prueba/protegido",
                                        "Bearer " + conPermiso,
                                        "no es json")
                                .statusCode())
                .isEqualTo(400);
        assertThat(
                        pedirConCuerpo(
                                        "/api/prueba/protegido",
                                        "Bearer " + conPermiso,
                                        "{\"nombre\":\"rol\"}")
                                .body())
                .isEqualTo("escrito|1");
    }

    @Test
    void dadoElCuerpoDel403_cuandoSeComparaConElCatalogo_entoncesEsElDeAccesoNoAutorizado() {
        DefinicionDeCodigo codigo = CodigosComunes.ACCESO_NO_AUTORIZADO;

        assertThat(CUERPO_403)
                .isEqualTo(
                        "{\"titulo\":\""
                                + codigo.titulo()
                                + "\",\"descripcion\":\""
                                + codigo.descripcion()
                                + "\",\"codigo\":\""
                                + codigo.codigo()
                                + "\"}");
        VerificadorDeCatalogo.verificar(List.of(codigo));
    }

    @Test
    void dadoUnTokenDeUsuario_cuandoLlegaAUnaRutaDeLaApi_entoncesPasa() throws Exception {
        String token = TOKENS.valido("autor.prueba", List.of("DOCENTE"));

        assertThat(pedir("GET", "/api/prueba/recurso", "Bearer " + token).statusCode())
                .isEqualTo(200);
    }

    @ParameterizedTest
    @MethodSource("autorizacionesInvalidas")
    void dadoUnTokenAusenteOInvalido_cuandoLlegaAUnaRutaInterna_entoncesResponde401SiempreIgual(
            String autorizacion) throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/interno/prueba/eventos", autorizacion);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(CUERPO_401);
    }

    @Test
    void dadoLaSaludDelProceso_cuandoSeLlamaSinToken_entoncesResponde() throws Exception {
        assertThat(pedir("GET", "/actuator/health", "").statusCode()).isEqualTo(200);
    }

    private static void assertEs403(HttpResponse<String> respuesta) {
        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(respuesta.body()).isEqualTo(CUERPO_403);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
    }

    private HttpResponse<String> pedirConCuerpo(String ruta, String autorizacion, String cuerpo)
            throws IOException, InterruptedException {
        HttpRequest peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                        .header("Authorization", autorizacion)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String autorizacion)
            throws IOException, InterruptedException {
        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                        .method(metodo, HttpRequest.BodyPublishers.noBody());
        if (!autorizacion.isEmpty()) {
            peticion.header("Authorization", autorizacion);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static ServidorJwks arrancarJwks() {
        try {
            return new ServidorJwks(TOKENS.jwks());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
