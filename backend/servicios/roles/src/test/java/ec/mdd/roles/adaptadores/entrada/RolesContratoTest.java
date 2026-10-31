package ec.mdd.roles.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.Funcionalidad;
import ec.mdd.roles.dominio.NombreRepetido;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.PermisosInvalidos;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.RolAsignado;
import ec.mdd.roles.dominio.RolPredefinido;
import ec.mdd.roles.dominio.SinAdministrador;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.AsignarRoles;
import ec.mdd.roles.puertos.entrada.CarreraSinRol;
import ec.mdd.roles.puertos.entrada.DatosDeRol;
import ec.mdd.roles.puertos.entrada.GestionarRoles;
import ec.mdd.roles.puertos.entrada.RolConUsuarios;
import ec.mdd.roles.puertos.entrada.RolDesconocido;
import ec.mdd.roles.puertos.entrada.RolNoEncontrado;
import ec.mdd.roles.puertos.entrada.UsuarioSinCarrera;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las operaciones de {@code /api/roles} cumplen {@code contratos/roles/openapi.yaml} (principio
 * IV), con un administrador que tiene lectura y escritura de «Usuarios y roles» y dobles de los
 * casos de uso escritos a mano (principio VIII: Mockito solo en la aplicación).
 */
@WebMvcTest(RolesControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({RolesContratoTest.Dobles.class, ErroresRoles.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class RolesContratoTest {

    private static final Permiso LEER_CARGAS =
            new Permiso("CARGAS", TipoDePermiso.LECTURA, Alcance.TODO);
    private static final Rol ADMINISTRADOR =
            new Rol(
                    "ADMINISTRADOR",
                    "Administrador del sistema",
                    "Administra.",
                    true,
                    List.of(new Permiso("USUARIOS_ROLES", TipoDePermiso.ESCRITURA, Alcance.TODO)));
    private static final Rol AUDITOR =
            Rol.nuevo("Auditor de cargas", "Revisa cargas.", List.of(LEER_CARGAS));

    private static final AtomicReference<String> QUIEN = new AtomicReference<>();

    private static final Map<String, List<String>> USUARIOS =
            Map.of(
                    "docente.demo",
                    List.of("DOCENTE"),
                    "coordinacion.demo",
                    List.of("COORDINADOR_CARRERA"));

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("roles");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElAdministrador_cuandoListaLasFuncionalidades_entonces200ConSusTiposEnOrden()
            throws Exception {
        MvcResult resultado = pedir(get("/api/roles/funcionalidades"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "{\"codigo\":\"DISTRIBUTIVO\",\"nombre\":\"Distributivo del periodo\","
                                + "\"tipos\":[\"LECTURA\",\"ESCRITURA\",\"APROBACION\",\"FIRMA\"]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoElAdministrador_cuandoListaLosRoles_entonces200ConPermisosYUsuarios()
            throws Exception {
        MvcResult resultado = pedir(get("/api/roles"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"identificador\":\"ADMINISTRADOR\"")
                .contains("\"predefinido\":true")
                .contains("\"usuariosAsignados\":1")
                .contains(
                        "{\"funcionalidad\":\"CARGAS\",\"tipo\":\"LECTURA\",\"alcance\":\"TODO\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnRolQueExisteYOtroQueNo_cuandoSeConsultan_entonces200Y404() throws Exception {
        MvcResult existe = pedir(get("/api/roles/AUDITOR_DE_CARGAS"));
        MvcResult noExiste = pedir(get("/api/roles/NO_EXISTE"));

        assertThat(existe.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(existe);
        assertThat(noExiste.getResponse().getStatus()).isEqualTo(404);
        assertThat(noExiste.getResponse().getContentAsString())
                .contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(noExiste);
    }

    @Test
    void dadoUnRolValido_cuandoSeCrea_entonces201ConSuIdentificadorYQuienLoCreo() throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        rol(
                                                "Auditor de roles",
                                                "{\"funcionalidad\":\"USUARIOS_ROLES\","
                                                        + "\"tipo\":\"LECTURA\",\"alcance\":\"TODO\"}")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"identificador\":\"AUDITOR_DE_ROLES\"")
                .contains("\"usuariosAsignados\":0");
        assertThat(QUIEN.get()).isEqualTo("admin.demo");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnRolSinPermisos_cuandoSeCrea_entonces400ConElCampoPermisos() throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rol("Vacío", "")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("COMUN_SOLICITUD_INVALIDA")
                .contains("\"campo\":\"permisos\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnTipoQueLaFuncionalidadNoAdmite_cuandoSeCrea_entonces400ConElDetalle()
            throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        rol(
                                                "Firmante",
                                                "{\"funcionalidad\":\"CARGAS\",\"tipo\":\"FIRMA\","
                                                        + "\"alcance\":\"TODO\"}")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString())
                .contains(
                        "{\"campo\":\"permisos\",\"descripcion\":\"«Cargas de datos» no admite el"
                                + " permiso de firma.\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnNombreQueDaUnIdentificadorDeD16_cuandoSeCrea_entonces409NombreRepetido()
            throws Exception {
        MvcResult resultado =
                pedir(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        rol(
                                                "Administrador",
                                                "{\"funcionalidad\":\"CARGAS\",\"tipo\":\"LECTURA\","
                                                        + "\"alcance\":\"TODO\"}")));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString()).contains("ROLES_NOMBRE_REPETIDO");
        contrato.validar(resultado);
    }

    @Test
    void dadoLasReglasDeEdicion_cuandoSeEditan_entoncesPredefinidoSinAdministradorY200()
            throws Exception {
        String cuerpo =
                rol(
                        "Auditor de cargas",
                        "{\"funcionalidad\":\"CARGAS\",\"tipo\":\"LECTURA\",\"alcance\":\"TODO\"}");

        MvcResult predefinido = pedir(editar("ADMINISTRADOR", cuerpo));
        MvcResult sinAdministrador = pedir(editar("GESTOR_DE_ROLES", cuerpo));
        MvcResult editado = pedir(editar("AUDITOR_DE_CARGAS", cuerpo));

        assertThat(predefinido.getResponse().getContentAsString())
                .contains("ROLES_ROL_PREDEFINIDO");
        assertThat(sinAdministrador.getResponse().getContentAsString())
                .contains("ROLES_SIN_ADMINISTRADOR");
        assertThat(editado.getResponse().getStatus()).isEqualTo(200);
        for (MvcResult resultado : List.of(predefinido, sinAdministrador, editado)) {
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoUnRolAsignadoYOtroLibre_cuandoSeBorran_entonces409Y204() throws Exception {
        MvcResult asignado = pedir(delete("/api/roles/AUDITOR_ASIGNADO"));
        MvcResult libre = pedir(delete("/api/roles/AUDITOR_DE_CARGAS"));

        assertThat(asignado.getResponse().getStatus()).isEqualTo(409);
        assertThat(asignado.getResponse().getContentAsString()).contains("ROLES_ROL_ASIGNADO");
        assertThat(libre.getResponse().getStatus()).isEqualTo(204);
        contrato.validar(asignado);
        contrato.validar(libre);
    }

    @Test
    void dadoLasAsignaciones_cuandoSeListanYSeCambian_entoncesCumplenElContrato() throws Exception {
        MvcResult lista = pedir(get("/api/roles/usuarios"));
        MvcResult asignado =
                pedir(
                        put("/api/roles/usuarios/docente.demo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[\"DOCENTE\",\"AUDITOR_DE_ROLES\"]}"));

        assertThat(lista.getResponse().getContentAsString())
                .isEqualTo(
                        "[{\"usuario\":\"coordinacion.demo\","
                                + "\"roles\":[\"COORDINADOR_CARRERA\"],\"carrera\":\"DS-01\"},"
                                + "{\"usuario\":\"docente.demo\",\"roles\":[\"DOCENTE\"]}]");
        assertThat(asignado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"usuario\":\"docente.demo\","
                                + "\"roles\":[\"AUDITOR_DE_ROLES\",\"DOCENTE\"]}");
        contrato.validar(lista);
        contrato.validar(asignado);
    }

    @Test
    void dadoUnCoordinador_cuandoSeIndicaSuCarrera_entonces200ConLaCarrera() throws Exception {
        MvcResult resultado = pedir(carrera("coordinacion.demo", "DS-01"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"usuario\":\"coordinacion.demo\","
                                + "\"roles\":[\"COORDINADOR_CARRERA\"],\"carrera\":\"DS-01\"}");
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoEsCoordinador_cuandoSeIndicaSuCarrera_entonces409DelCatalogo()
            throws Exception {
        MvcResult resultado = pedir(carrera("docente.demo", "DS-01"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"ROLES_CARRERA_SIN_ROL\"");
        contrato.validar(resultado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ds-01", "DS 01", "CODIGO-DEMASIADO-LARGO-PARA-LA-CARRERA", ""})
    void dadoUnCodigoInvalido_cuandoSeIndicaLaCarrera_entonces400(String codigo) throws Exception {
        assertThat(pedir(carrera("coordinacion.demo", codigo)).getResponse().getStatus())
                .isEqualTo(400);
    }

    @Test
    void dadoElCodigoMasLargo_cuandoSeIndicaLaCarrera_entonces200() throws Exception {
        assertThat(
                        pedir(carrera("coordinacion.demo", "ABCDEFGHIJ-123456789"))
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoUnCoordinadorConCarrera_cuandoSeQuita_entonces200() throws Exception {
        MvcResult resultado = pedir(delete("/api/roles/usuarios/coordinacion.demo/carrera"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        contrato.validar(resultado);
    }

    @Test
    void dadoQuienNoTieneCarrera_cuandoSeQuita_entonces404DelCatalogo() throws Exception {
        MvcResult resultado = pedir(delete("/api/roles/usuarios/docente.demo/carrera"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"codigo\":\"ROLES_USUARIO_SIN_CARRERA\"");
        contrato.validar(resultado);
    }

    @Test
    void dadoLasAsignaciones_cuandoSeListan_entoncesTraenLaCarreraDelCoordinador()
            throws Exception {
        MvcResult lista = pedir(get("/api/roles/usuarios"));

        assertThat(lista.getResponse().getContentAsString()).doesNotContain("\"carrera\":null");
        contrato.validar(lista);
    }

    private static MockHttpServletRequestBuilder carrera(String usuario, String carrera) {
        return put("/api/roles/usuarios/" + usuario + "/carrera")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"carrera\":\"" + carrera + "\"}");
    }

    @Test
    void dadoUnRolQueNoExisteOQuitarseElPropio_cuandoSeAsigna_entonces400Y409() throws Exception {
        MvcResult desconocido =
                pedir(
                        put("/api/roles/usuarios/docente.demo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[\"BRUJO\"]}"));
        MvcResult propio =
                pedir(
                        put("/api/roles/usuarios/admin.demo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[]}"));

        assertThat(desconocido.getResponse().getStatus()).isEqualTo(400);
        assertThat(desconocido.getResponse().getContentAsString())
                .contains(
                        "{\"campo\":\"roles\",\"descripcion\":\"No existen estos roles: BRUJO.\"}");
        assertThat(propio.getResponse().getStatus()).isEqualTo(409);
        assertThat(propio.getResponse().getContentAsString()).contains("ROLES_SIN_ADMINISTRADOR");
        contrato.validar(desconocido);
        contrato.validar(propio);
    }

    private MvcResult pedir(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.with(administrador())).andReturn();
    }

    private static MockHttpServletRequestBuilder editar(String identificador, String cuerpo) {
        return put("/api/roles/" + identificador)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private static String rol(String nombre, String permisos) {
        return "{\"nombre\":\""
                + nombre
                + "\",\"descripcion\":\"Descripción.\",\"permisos\":["
                + permisos
                + "]}";
    }

    private static JwtRequestPostProcessor administrador() {
        return jwt().jwt(token -> token.subject("admin.demo"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"),
                        new SimpleGrantedAuthority("PERMISO_USUARIOS_ROLES:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_USUARIOS_ROLES:ESCRITURA"));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        GestionarRoles gestionarRoles() {
            return new GestionarRoles() {

                @Override
                public List<Funcionalidad> funcionalidades() {
                    return List.of(
                            new Funcionalidad(
                                    "CARGAS",
                                    "Cargas de datos",
                                    EnumSet.of(TipoDePermiso.LECTURA, TipoDePermiso.ESCRITURA)),
                            new Funcionalidad(
                                    "DISTRIBUTIVO",
                                    "Distributivo del periodo",
                                    EnumSet.of(
                                            TipoDePermiso.FIRMA,
                                            TipoDePermiso.LECTURA,
                                            TipoDePermiso.APROBACION,
                                            TipoDePermiso.ESCRITURA)));
                }

                @Override
                public List<RolConUsuarios> roles() {
                    return List.of(
                            new RolConUsuarios(ADMINISTRADOR, 1), new RolConUsuarios(AUDITOR, 0));
                }

                @Override
                public RolConUsuarios consultar(String identificador) {
                    if (!identificador.equals(AUDITOR.identificador())) {
                        throw new RolNoEncontrado(identificador);
                    }
                    return new RolConUsuarios(AUDITOR, 0);
                }

                @Override
                public RolConUsuarios crear(DatosDeRol datos, String quien) {
                    QUIEN.set(quien);
                    if (Rol.identificadorDe(datos.nombre()).equals("ADMINISTRADOR")) {
                        throw new NombreRepetido("igual que ADMINISTRADOR");
                    }
                    if (datos.permisos().stream().anyMatch(p -> p.tipo() == TipoDePermiso.FIRMA)) {
                        throw new PermisosInvalidos(
                                "«Cargas de datos» no admite el permiso de firma.");
                    }
                    return new RolConUsuarios(
                            Rol.nuevo(datos.nombre(), datos.descripcion(), datos.permisos()), 0);
                }

                @Override
                public RolConUsuarios editar(String identificador, DatosDeRol datos, String quien) {
                    switch (identificador) {
                        case "ADMINISTRADOR" -> throw new RolPredefinido(identificador);
                        case "GESTOR_DE_ROLES" -> throw new SinAdministrador(quien);
                        default -> {
                            return new RolConUsuarios(
                                    AUDITOR.editado(
                                            datos.nombre(), datos.descripcion(), datos.permisos()),
                                    0);
                        }
                    }
                }

                @Override
                public void borrar(String identificador, String quien) {
                    if (identificador.equals("AUDITOR_ASIGNADO")) {
                        throw new RolAsignado(identificador, 2);
                    }
                }
            };
        }

        @Bean
        AsignarCarrera asignarCarrera() {
            return new AsignarCarrera() {

                @Override
                public Map<String, String> carreras() {
                    return Map.of("coordinacion.demo", "DS-01");
                }

                @Override
                public void indicar(String usuario, String carrera, String quien) {
                    if ("docente.demo".equals(usuario)) {
                        throw new CarreraSinRol();
                    }
                }

                @Override
                public void quitar(String usuario, String quien) {
                    if ("docente.demo".equals(usuario)) {
                        throw new UsuarioSinCarrera();
                    }
                }
            };
        }

        @Bean
        AsignarRoles asignarRoles() {
            return new AsignarRoles() {

                @Override
                public Map<String, List<String>> rolesDeUsuarios() {
                    return USUARIOS;
                }

                @Override
                public List<String> asignar(
                        String usuario, Collection<String> roles, String quien) {
                    if (roles.contains("BRUJO")) {
                        throw new RolDesconocido(List.of("BRUJO"));
                    }
                    if (usuario.equals(quien)) {
                        throw new SinAdministrador(quien);
                    }
                    return roles.stream().sorted().toList();
                }
            };
        }
    }
}
