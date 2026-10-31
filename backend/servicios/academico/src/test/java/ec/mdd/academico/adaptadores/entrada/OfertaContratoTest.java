package ec.mdd.academico.adaptadores.entrada;

import static ec.mdd.academico.adaptadores.entrada.AcademicoContratoTest.json;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.cuerpo;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.escritura;
import static ec.mdd.academico.adaptadores.entrada.CarrerasContratoTest.lectura;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.AutoridadesDelToken;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * La oferta del periodo cumple {@code contratos/academico/openapi.yaml} (principio IV, F03-H03),
 * con los servicios reales sobre dobles en memoria de los repositorios.
 */
@WebMvcTest(OfertaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    OfertaContratoTest.Dobles.class,
    CarrerasContratoTest.Dobles.class,
    ErroresAcademico.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class OfertaContratoTest {

    static final String RUTA = "/api/academico/periodos/2026-I/oferta";
    static final String POO = "DS-P-II-POO-204";

    static final String NOCTURNA =
            "{\"asignatura\":\""
                    + POO
                    + "\",\"paralelo\":\"A_NOCTURNA\",\"jornada\":\"NOCTURNA\","
                    + "\"modalidad\":\"PRESENCIAL\",\"estudiantes\":28}";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("academico");

    @Autowired private MockMvc mvc;

    @Autowired private CarrerasEnMemoria carreras;

    @Autowired private OfertaEnMemoria oferta;

    /** DS vigente y nocturna, con POO de 4 horas y CONT, de otra carrera, inactiva. */
    @BeforeEach
    void reiniciar() {
        carreras.reiniciar();
        oferta.reiniciar();
        Instant ahora = Instant.now();
        carreras.registrar(
                Carrera.nueva(
                        "550613A01-P-0301",
                        "DESARROLLO DE SOFTWARE",
                        "DS",
                        "MATRIZ",
                        NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                        Modalidad.PRESENCIAL,
                        List.of(Jornada.NOCTURNA),
                        new CampoConocimiento("06", "061"),
                        null,
                        5),
                "x",
                ahora);
        carreras.registrar(Malla.nueva("DS-2019", "550613A01-P-0301"), "x", ahora);
        carreras.registrar(asignatura(POO, true), "x", ahora);
        carreras.registrar(asignatura("DS-INACTIVA", false), "x", ahora);
    }

    static Asignatura asignatura(String codigo, boolean activa) {
        return Asignatura.nueva(
                        codigo,
                        "550613A01-P-0301",
                        "DS-2019",
                        "PROGRAMACIÓN ORIENTADA A OBJETOS",
                        2,
                        UnidadDeOrganizacionCurricular.PROFESIONAL,
                        new BigDecimal("4"),
                        null,
                        null,
                        false,
                        new CampoConocimiento("06", "061"),
                        List.of("programación", "objetos"))
                .conEstado(activa);
    }

    @Test
    void
            dadaUnaAsignaturaActiva_cuandoSeOferta_entonces201ConLasHorasDeLaAsignaturaYLaSedeDeLaCarrera()
                    throws Exception {
        MvcResult resultado = pedir(json(post(RUTA), NOCTURNA), escritura());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("estado", "OFERTADA")
                .containsEntry("sede", "MATRIZ")
                .containsEntry("sedeIndicada", false)
                .containsEntry("horasIndicadas", false)
                .containsEntry("ciclo", 2);
        assertThat(JsonPath.<Number>read(cuerpo(resultado), "$.horasSemanales").doubleValue())
                .isEqualTo(4.0);
        contrato.validar(resultado);
    }

    @Test
    void dadosDatosQueNoCumplenSusReglas_cuandoSeOferta_entonces400ConSuCampo() throws Exception {
        Map<String, String> casos =
                Map.of(
                        "jornada",
                                NOCTURNA.replace(
                                        "\"jornada\":\"NOCTURNA\"", "\"jornada\":\"MATUTINA\""),
                        "horasSemanales",
                                NOCTURNA.replace(
                                        "\"estudiantes\":28",
                                        "\"estudiantes\":28,\"horasSemanales\":5"),
                        "asignatura", NOCTURNA.replace(POO, "DS-INACTIVA"),
                        "sede",
                                NOCTURNA.replace(
                                        "\"estudiantes\":28",
                                        "\"estudiantes\":28,\"sede\":\"RECT\""));
        for (Map.Entry<String, String> caso : casos.entrySet()) {
            MvcResult resultado = pedir(json(post(RUTA), caso.getValue()), escritura());

            assertThat(resultado.getResponse().getStatus()).as(caso.getKey()).isEqualTo(400);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.detalles[0].campo"))
                    .isEqualTo(caso.getKey());
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaUnaRepetidaOUnPeriodoFueraDePlanificacion_cuandoSeOferta_entonces409ConSuCodigo()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult repetida = pedir(json(post(RUTA), NOCTURNA), escritura());
        MvcResult cerrada =
                pedir(json(post("/api/academico/periodos/2025-II/oferta"), NOCTURNA), escritura());

        assertThat(repetida.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(repetida)).contains("ACADEMICO_ASIGNATURA_PARALELO_REPETIDA");
        assertThat(cuerpo(cerrada)).contains("ACADEMICO_OFERTA_CERRADA");
        contrato.validar(repetida);
        contrato.validar(cerrada);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeOfertaOSeConsulta_entonces404() throws Exception {
        for (MvcResult resultado :
                List.of(
                        pedir(
                                json(post("/api/academico/periodos/NADA/oferta"), NOCTURNA),
                                escritura()),
                        pedir(get("/api/academico/periodos/NADA/oferta"), lectura()))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            contrato.validar(resultado);
        }
    }

    @Test
    void
            dadaUnaAsignaturaDe4HorasEn3Paralelos_cuandoSeConsultaLaOferta_entoncesSuma12YConUnaRetirada8()
                    throws Exception {
        for (String paralelo : List.of("A_NOCTURNA", "B_NOCTURNA", "C_NOCTURNA")) {
            pedir(json(post(RUTA), NOCTURNA.replace("A_NOCTURNA", paralelo)), escritura());
        }

        MvcResult tres = pedir(get(RUTA), oferta());
        pedir(
                json(put(RUTA + "/" + POO + "/C_NOCTURNA/estado"), "{\"estado\":\"RETIRADA\"}"),
                escritura());
        MvcResult dos = pedir(get(RUTA), lectura());

        assertThat(tres.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(tres), "$"))
                .containsEntry("periodo", "2026-I")
                .containsEntry("estadoDelPeriodo", "PLANIFICACION")
                .containsEntry("modificable", true);
        assertThat(JsonPath.<Number>read(cuerpo(tres), "$.horasACubrir").doubleValue())
                .isEqualTo(12.0);
        assertThat(
                        JsonPath.<Number>read(
                                        cuerpo(tres), "$.carreras[0].asignaturas[0].horasACubrir")
                                .doubleValue())
                .isEqualTo(12.0);
        assertThat(JsonPath.<Integer>read(cuerpo(tres), "$.carreras[0].asignaturas[0].paralelos"))
                .isEqualTo(3);
        assertThat(JsonPath.<String>read(cuerpo(tres), "$.carreras[0].nombre"))
                .isEqualTo("DESARROLLO DE SOFTWARE");
        assertThat(JsonPath.<Number>read(cuerpo(dos), "$.horasACubrir").doubleValue())
                .isEqualTo(8.0);
        assertThat(JsonPath.<List<String>>read(cuerpo(dos), "$.asignaturasParalelo[*].estado"))
                .containsExactly("OFERTADA", "OFERTADA", "RETIRADA");
        contrato.validar(tres);
        contrato.validar(dos);
    }

    @Test
    void dadaUnaAsignaturaParalelo_cuandoSeConsulta_entoncesTraeLosDatosParaLaAfinidadYSusHoras()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult resultado = pedir(get(RUTA + "/" + POO + "/A_NOCTURNA"), oferta());

        String cuerpo = cuerpo(resultado);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$"))
                .containsEntry("ciclo", 2)
                .containsEntry("troncoComun", false)
                .containsEntry("campoEspecifico", "061")
                .containsEntry("horasIndicadas", false)
                .containsEntry("porEncimaDeLaAsignatura", false);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo, "$.campoAmplio"))
                .containsEntry("codigo", "06")
                .containsEntry("nombre", "Tecnologías de la información y la comunicación");
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.palabrasClave"))
                .containsExactly("programación", "objetos");
        contrato.validar(resultado);
        MvcResult noEsta = pedir(get(RUTA + "/" + POO + "/Z_NOCTURNA"), oferta());
        assertThat(noEsta.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(noEsta);
    }

    @Test
    void
            dadasHorasIndicadasYSuAsignaturaCorregidaDespues_cuandoSeConsulta_entoncesSeMarcaPorEncima()
                    throws Exception {
        pedir(
                json(
                        post(RUTA),
                        NOCTURNA.replace(
                                "\"estudiantes\":28", "\"estudiantes\":28,\"horasSemanales\":4")),
                escritura());
        Asignatura poo =
                carreras.asignaturas().stream()
                        .filter(una -> una.codigo().equals(POO))
                        .findFirst()
                        .orElseThrow();
        carreras.guardar(
                poo.corregida(
                                poo.nombre(),
                                poo.ciclo(),
                                poo.unidadOrganizacionCurricular(),
                                new BigDecimal("3"),
                                poo.horasPracticasSemanales(),
                                poo.creditos(),
                                poo.troncoComun(),
                                poo.campo(),
                                poo.palabrasClave())
                        .asignatura());

        MvcResult resultado = pedir(get(RUTA + "/" + POO + "/A_NOCTURNA"), lectura());

        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("horasIndicadas", true)
                .containsEntry("porEncimaDeLaAsignatura", true);
        assertThat(JsonPath.<Number>read(cuerpo(resultado), "$.horasDeLaAsignatura").doubleValue())
                .isEqualTo(3.0);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaOfertada_cuandoSeCorrigeYSeRetiraYSeVuelveAOfertar_entoncesCumpleElContrato()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());
        String datos =
                "{\"jornada\":\"NOCTURNA\",\"modalidad\":\"DUAL\",\"estudiantes\":31,"
                        + "\"sede\":\"MATRIZ\",\"horasSemanales\":3.5}";

        MvcResult corregida =
                pedir(json(put(RUTA + "/" + POO + "/A_NOCTURNA"), datos), escritura());
        MvcResult retirada =
                pedir(
                        json(
                                put(RUTA + "/" + POO + "/A_NOCTURNA/estado"),
                                "{\"estado\":\"RETIRADA\"}"),
                        escritura());
        MvcResult otraVez =
                pedir(
                        json(
                                put(RUTA + "/" + POO + "/A_NOCTURNA/estado"),
                                "{\"estado\":\"OFERTADA\"}"),
                        escritura());
        MvcResult invalida =
                pedir(
                        json(
                                put(RUTA + "/" + POO + "/A_NOCTURNA"),
                                datos.replace("NOCTURNA", "MATUTINA")),
                        escritura());

        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(corregida), "$"))
                .containsEntry("modalidad", "DUAL")
                .containsEntry("estudiantes", 31)
                .containsEntry("sedeIndicada", true);
        assertThat(JsonPath.<String>read(cuerpo(retirada), "$.estado")).isEqualTo("RETIRADA");
        assertThat(JsonPath.<String>read(cuerpo(otraVez), "$.estado")).isEqualTo("OFERTADA");
        assertThat(JsonPath.<String>read(cuerpo(invalida), "$.detalles[0].campo"))
                .isEqualTo("jornada");
        for (MvcResult resultado : List.of(corregida, retirada, otraVez, invalida)) {
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaUnaOfertaDeUnPeriodoVigente_cuandoSeCorrigeOSeRetira_entonces409OfertaCerrada()
            throws Exception {
        oferta.registrar(
                ec.mdd.academico.dominio.AsignaturaParalelo.nueva(
                        "2025-II",
                        POO,
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        28,
                        null),
                "x",
                Instant.now());
        String vigente = "/api/academico/periodos/2025-II/oferta/" + POO + "/A_NOCTURNA";

        MvcResult corregir =
                pedir(
                        json(
                                put(vigente),
                                "{\"jornada\":\"NOCTURNA\",\"modalidad\":\"DUAL\",\"estudiantes\":1}"),
                        escritura());
        MvcResult retirar =
                pedir(json(put(vigente + "/estado"), "{\"estado\":\"RETIRADA\"}"), escritura());
        MvcResult consultar = pedir(get("/api/academico/periodos/2025-II/oferta"), lectura());

        assertThat(cuerpo(corregir)).contains("ACADEMICO_OFERTA_CERRADA");
        assertThat(cuerpo(retirar)).contains("ACADEMICO_OFERTA_CERRADA");
        assertThat(JsonPath.<Boolean>read(cuerpo(consultar), "$.modificable")).isFalse();
        contrato.validar(corregir);
        contrato.validar(retirar);
        contrato.validar(consultar);
    }

    @Test
    void dadoUnCoordinadorDeLaCarrera_cuandoConsulta_entoncesVeSuOferta() throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult resultado = pedir(get(RUTA), coordinador("550613A01-P-0301"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.horasACubrir")).isEqualTo(4);
        assertThat(cuerpo(resultado)).contains(POO);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinadorDeOtraCarrera_cuandoConsulta_entoncesNoVeNadaDeEsta() throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult resultado = pedir(get(RUTA), coordinador("OTRA-01"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.horasACubrir")).isZero();
        assertThat(cuerpo(resultado)).doesNotContain(POO);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinadorDeOtraCarrera_cuandoConsultaUnParalelo_entonces403SinDatos()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult resultado = pedir(get(RUTA + "/" + POO + "/A_NOCTURNA"), coordinador("OTRA-01"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(cuerpo(resultado))
                .contains("SEGURIDAD_ACCESO_NO_AUTORIZADO")
                .doesNotContain(POO);
        contrato.validar(resultado);
    }

    @Test
    void dadoUnCoordinadorDeLaCarrera_cuandoConsultaUnParalelo_entonces200() throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        assertThat(
                        pedir(
                                        get(RUTA + "/" + POO + "/A_NOCTURNA"),
                                        coordinador("550613A01-P-0301"))
                                .getResponse()
                                .getStatus())
                .isEqualTo(200);
    }

    @Test
    void dadoUnCoordinadorSinCarrera_cuandoConsulta_entonces403QueDiceQuePidaSuCarrera()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        for (MvcResult resultado :
                List.of(
                        pedir(get(RUTA), coordinador(null)),
                        pedir(get(RUTA + "/" + POO + "/A_NOCTURNA"), coordinador(null)))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(cuerpo(resultado))
                    .contains("COMUN_CARRERA_SIN_INDICAR", "Roles de usuarios")
                    .doesNotContain(POO);
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoQuienTieneUnPermisoConAlcanceTotal_cuandoConsulta_entoncesVeTodoSinCarrera()
            throws Exception {
        pedir(json(post(RUTA), NOCTURNA), escritura());

        MvcResult resultado =
                pedir(
                        get(RUTA),
                        conPermisos(null, "OFERTA:LECTURA:PROPIO", "DISTRIBUTIVO:LECTURA"));

        assertThat(cuerpo(resultado)).contains(POO);
    }

    /** Un Coordinador de carrera, con el token real: «Oferta» solo sobre lo propio (D-65). */
    private static JwtRequestPostProcessor coordinador(String carrera) {
        return conPermisos(
                carrera,
                "OFERTA:LECTURA:PROPIO",
                "DISTRIBUTIVO:LECTURA:PROPIO",
                "DOCENTES:LECTURA");
    }

    private static JwtRequestPostProcessor conPermisos(String carrera, String... permisos) {
        return jwt().jwt(
                        token -> {
                            token.claim("permisos", List.of(permisos));
                            if (carrera != null) {
                                token.claim("carrera", carrera);
                            }
                        })
                .authorities(new AutoridadesDelToken());
    }

    MvcResult pedir(MockHttpServletRequestBuilder peticion, JwtRequestPostProcessor token)
            throws Exception {
        return mvc.perform(peticion.with(token)).andReturn();
    }

    /** El Responsable de consolidación: lee la oferta con «Oferta: lectura» (D-65). */
    static JwtRequestPostProcessor oferta() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(new SimpleGrantedAuthority("PERMISO_OFERTA:LECTURA"));
    }

    /** La oferta en memoria; las carreras son las de {@link CarrerasContratoTest.Dobles}. */
    @TestConfiguration
    static class Dobles {

        @Bean
        OfertaEnMemoria ofertaEnMemoria() {
            return new OfertaEnMemoria();
        }

        @Bean
        ConsultarOferta consultarOferta(OfertaEnMemoria oferta, CarrerasEnMemoria carreras) {
            return oferta.consultar(carreras);
        }

        @Bean
        MantenerOferta mantenerOferta(OfertaEnMemoria oferta, CarrerasEnMemoria carreras) {
            return oferta.mantener(carreras);
        }
    }
}
