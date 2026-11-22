package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.mdd.generacion.dominio.ActividadCongelada;
import ec.mdd.generacion.dominio.AsignaturaCongelada;
import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import ec.mdd.generacion.dominio.TipoDeIdentificacion;
import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

/** Las consultas internas del contenido congelado cumplen su contrato (F20-H01, ADR-023). */
@WebMvcTest(InternoGeneracionControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import(InternoGeneracionContratoTest.Dobles.class)
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class InternoGeneracionContratoTest {

    private static final String CARGAS = "/interno/generacion/periodos/%s/versiones/%s/cargas";
    private static final String CONTENIDO =
            "/interno/generacion/periodos/%s/versiones/%s/contenido";
    private static final String HASH = "a".repeat(64);
    private static final String ANA = "0109990010";

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("generacion");

    @Autowired private MockMvc mvc;

    @Test
    void dadaUnaVersionCongelada_cuandoUnProcesoPideSusCargas_entonces200ConLosDocentesYSusHoras()
            throws Exception {
        MvcResult resultado = pedir(CARGAS.formatted("2026-I", 1), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"hash\":\""
                                + HASH
                                + "\",\"docentes\":[{\"identificacion\":\"0109990010\","
                                + "\"nombre\":\"PEREZ ANA\",\"horas\":"
                                + "{\"clase\":8.00,\"otrasDocencia\":0,\"investigacion\":2.50,"
                                + "\"vinculacion\":0,\"gestion\":0},\"dedicacion\":\"TC\"},"
                                + "{\"identificacion\":\"0109990020\",\"nombre\":\"VEGA LUIS\","
                                + "\"horas\":{\"clase\":0,\"otrasDocencia\":0,"
                                + "\"investigacion\":0,\"vinculacion\":0,\"gestion\":3}}]}");
        contrato.validar(resultado);
    }

    @Test
    void dadoUnDocente_cuandoUnProcesoPideSuCarga_entonces200ConSusAsignacionesYSuCarga()
            throws Exception {
        MvcResult resultado = pedir(CARGAS.formatted("2026-I", 1) + "/" + ANA, deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString())
                .isEqualTo(
                        "{\"hash\":\""
                                + HASH
                                + "\",\"docente\":{\"identificacion\":\"0109990010\","
                                + "\"nombre\":\"PEREZ ANA\",\"dedicacion\":\"TC\","
                                + "\"horasContratadas\":40,\"tipoIdentificacion\":\"CEDULA\"},\"asignaturas\":[{\"codigo\":"
                                + "\"BD-301\",\"nombre\":\"Bases de datos\",\"paralelo\":\"A\","
                                + "\"carrera\":\"TDS\",\"nombreCarrera\":\"Software\","
                                + "\"horas\":8.00}],\"actividades\":[{\"codigo\":\"141-a\","
                                + "\"nombre\":\"Proyecto\",\"componente\":\"INVESTIGACION\","
                                + "\"carrera\":\"TDS\",\"horas\":2.50,"
                                + "\"nombreProyecto\":\"Suelos\",\"proyecto\":\"INV-01\"}],"
                                + "\"horas\":"
                                + "{\"clase\":8.00,\"otrasDocencia\":0,\"investigacion\":2.50,"
                                + "\"vinculacion\":0,\"gestion\":0}}");
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaVersionCongelada_cuandoUnProcesoPideSuContenido_entonces200ConCadaCargaCompleta()
            throws Exception {
        MvcResult resultado = pedir(CONTENIDO.formatted("2026-I", 1), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo)
                .startsWith("{\"hash\":\"" + HASH + "\",\"cargas\":[{\"hash\":\"" + HASH + "\"")
                .contains("\"identificacion\":\"0109990010\"", "\"codigo\":\"BD-301\"")
                .contains("\"identificacion\":\"0109990020\",\"nombre\":\"VEGA LUIS\"}");
        // Sin tipo de identificación: no estaba activo al congelar (D-119).
        assertThat(cuerpo.split("tipoIdentificacion", -1)).hasSize(2);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaVersionSinCongelar_cuandoUnProcesoPideSuContenido_entonces404() throws Exception {
        MvcResult resultado = pedir(CONTENIDO.formatted("2026-I", 2), deProceso());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(resultado);
    }

    @Test
    void dadaUnaVersionSinCongelarOUnDocenteAusente_cuandoUnProcesoPide_entonces404()
            throws Exception {
        MvcResult sinVersion = pedir(CARGAS.formatted("2026-I", 2), deProceso());
        MvcResult sinDocente = pedir(CARGAS.formatted("2026-I", 1) + "/0109990099", deProceso());

        assertThat(sinVersion.getResponse().getStatus()).isEqualTo(404);
        assertThat(sinDocente.getResponse().getStatus()).isEqualTo(404);
        contrato.validar(sinVersion);
        contrato.validar(sinDocente);
    }

    @Test
    void dadosUnaRutaInvalidaOUnTokenDeUsuario_cuandoSePide_entonces400O403() throws Exception {
        MvcResult invalida = pedir(CARGAS.formatted("2026-I", 0), deProceso());
        MvcResult deUsuario = pedir(CARGAS.formatted("2026-I", 1), jwt());

        assertThat(invalida.getResponse().getStatus()).isEqualTo(400);
        assertThat(deUsuario.getResponse().getStatus()).isEqualTo(403);
        contrato.validar(invalida);
        contrato.validar(deUsuario);
    }

    private MvcResult pedir(String ruta, JwtRequestPostProcessor token) throws Exception {
        return mvc.perform(get(ruta).with(token)).andReturn();
    }

    private static JwtRequestPostProcessor deProceso() {
        return jwt().jwt(token -> token.subject("proceso:salida"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROCESO"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dobles {

        @Bean
        ConsultarContenidoCongelado consultarContenidoCongelado() {
            CargaCongelada ana =
                    new CargaCongelada(
                            ANA,
                            "PEREZ ANA",
                            TipoDeIdentificacion.CEDULA,
                            Dedicacion.TC,
                            40,
                            List.of(
                                    new AsignaturaCongelada(
                                            "BD-301",
                                            "Bases de datos",
                                            "A",
                                            "TDS",
                                            "Software",
                                            new BigDecimal("8.00"))),
                            List.of(
                                    new ActividadCongelada(
                                            "141-a",
                                            "Proyecto",
                                            Componente.INVESTIGACION,
                                            "TDS",
                                            "INV-01",
                                            "Suelos",
                                            new BigDecimal("2.50"))),
                            new CargaHoraria(
                                    new BigDecimal("8.00"),
                                    BigDecimal.ZERO,
                                    new BigDecimal("2.50"),
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO));
            return new ConsultarContenidoCongelado() {
                @Override
                public Optional<CargasCongeladas> cargas(String periodo, int version) {
                    return version == 1
                            ? Optional.of(
                                    new CargasCongeladas(
                                            HASH,
                                            List.of(
                                                    DocenteDeLaVersion.de(ana),
                                                    // Sin dedicación: no estaba activo al congelar.
                                                    new DocenteDeLaVersion(
                                                            "0109990020",
                                                            "VEGA LUIS",
                                                            null,
                                                            new CargaHoraria(
                                                                    BigDecimal.ZERO,
                                                                    BigDecimal.ZERO,
                                                                    BigDecimal.ZERO,
                                                                    BigDecimal.ZERO,
                                                                    new BigDecimal("3"))))))
                            : Optional.empty();
                }

                @Override
                public Optional<ContenidoCompleto> contenido(String periodo, int version) {
                    CargaCongelada luis =
                            new CargaCongelada(
                                    "0109990020",
                                    "VEGA LUIS",
                                    null,
                                    null,
                                    null,
                                    List.of(),
                                    List.of(),
                                    new CargaHoraria(
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO));
                    return version == 1
                            ? Optional.of(new ContenidoCompleto(HASH, List.of(ana, luis)))
                            : Optional.empty();
                }

                @Override
                public Optional<CargaDeLaVersion> carga(
                        String periodo, int version, String docente) {
                    return version == 1 && ANA.equals(docente)
                            ? Optional.of(new CargaDeLaVersion(HASH, ana))
                            : Optional.empty();
                }
            };
        }
    }
}
