package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteReforma;
import ec.mdd.validacionocs.dominio.MotivoDeReformaInvalido;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.IniciarReforma;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PublicarDistributivo;
import ec.mdd.validacionocs.puertos.entrada.RegistrarResolucion;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Iniciar una reforma cumple {@code contratos/validacionocs/openapi.yaml} (F18-H02,
 * contracts/validacionocs.md): el 200 con la versión nueva, la reforma en curso y la publicación
 * anterior, cada error con su código y el 403 sin el permiso. El caso de uso, simulado: sus reglas
 * las prueban {@code IniciarReformaServicioTest} y {@code ProcesoAprobacionTest}.
 */
@WebMvcTest(FlujoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ReformaContratoTest.Dobles.class, ErroresValidacionocs.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class ReformaContratoTest {

    private static final String RUTA = "/api/validacionocs/periodos/%s/reforma";
    private static final String MOTIVO = "Cambia el docente de Software por una licencia.";
    private static final Instant PUBLICADA = Instant.parse("2026-11-18T15:00:00Z");
    private static final Instant INICIADA = Instant.parse("2026-11-20T15:00:00Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final IniciarReforma REFORMAR = mock(IniciarReforma.class);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void reiniciar() {
        reset(REFORMAR);
    }

    private MvcResult pedir(String periodo, String... permisos) throws Exception {
        return pedirCon(periodo, "{\"motivo\":\"" + MOTIVO + "\"}", permisos);
    }

    private MvcResult pedirCon(String periodo, String cuerpo, String... permisos) throws Exception {
        return mvc.perform(
                        post(RUTA.formatted(periodo))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(cuerpo)
                                .with(
                                        jwt().jwt(una -> una.subject("vicerrectorado.demo"))
                                                .authorities(
                                                        java.util.Arrays.stream(permisos)
                                                                .map(
                                                                        permiso ->
                                                                                new SimpleGrantedAuthority(
                                                                                        "PERMISO_"
                                                                                                + permiso))
                                                                .toArray(
                                                                        SimpleGrantedAuthority[]
                                                                                ::new))))
                .andReturn();
    }

    // Con doAnswer: un when(...) volvería a llamar al doble, que todavía lanza el error anterior.
    private static void falla(Class<? extends RuntimeException> error) {
        org.mockito.Mockito.doAnswer(
                        invocacion -> {
                            throw error.getDeclaredConstructor().newInstance();
                        })
                .when(REFORMAR)
                .iniciar(any(), any(), any());
    }

    @Test
    void
            dadoElVicerrector_cuandoIniciaLaReforma_entonces200ConLaVersionNuevaYLaPublicacionAnterior()
                    throws Exception {
        when(REFORMAR.iniciar(eq("2026-I"), eq(MOTIVO), any()))
                .thenReturn(
                        new FlujoDelDistributivo(
                                "2026-I",
                                TipoDeEtapa.EN_CONSOLIDACION,
                                "Consolidación",
                                2,
                                List.of(),
                                List.of(),
                                List.of(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(
                                        new FlujoDelDistributivo.Publicacion(
                                                1, "secretaria.demo", PUBLICADA)),
                                Optional.of(
                                        new FlujoDelDistributivo.ReformaEnCurso(
                                                1, 2, MOTIVO, "vicerrectorado.demo", INICIADA))));

        MvcResult resultado = pedir("2026-I", "DISTRIBUTIVO:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = cuerpo(resultado);
        assertThat(cuerpo.get("etapa").asString()).isEqualTo("EN_CONSOLIDACION");
        assertThat(cuerpo.get("version").asInt()).isEqualTo(2);
        assertThat(cuerpo.get("publicacion").get("version").asInt()).isEqualTo(1);
        JsonNode reforma = cuerpo.get("reforma");
        assertThat(reforma.get("versionPublicada").asInt()).isEqualTo(1);
        assertThat(reforma.get("version").asInt()).isEqualTo(2);
        assertThat(reforma.get("motivo").asString()).isEqualTo(MOTIVO);
        assertThat(reforma.get("iniciadaPor").asString()).isEqualTo("vicerrectorado.demo");
        assertThat(reforma.get("iniciadaEn").asString()).isEqualTo("2026-11-20T15:00:00Z");
        contrato.validar(resultado);
    }

    @Test
    void dadoCadaRechazoDelCasoDeUso_cuandoSeIniciaLaReforma_entoncesSuCodigoSegunElContrato()
            throws Exception {
        record Caso(Class<? extends RuntimeException> error, int estado, String codigo) {}
        for (Caso caso :
                List.of(
                        new Caso(
                                EtapaNoAdmiteReforma.class,
                                409,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA"),
                        new Caso(
                                MotivoDeReformaInvalido.class,
                                400,
                                "VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO"),
                        new Caso(PeriodoCerrado.class, 409, "VALIDACIONOCS_PERIODO_CERRADO"),
                        new Caso(RolDeOtraEtapa.class, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO"))) {
            falla(caso.error());

            MvcResult resultado = pedir("2026-I", "DISTRIBUTIVO:APROBACION");

            assertThat(resultado.getResponse().getStatus())
                    .as(caso.codigo())
                    .isEqualTo(caso.estado());
            assertThat(cuerpo(resultado).get("codigo").asString()).isEqualTo(caso.codigo());
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoQuienNoTieneElPermiso_cuandoIniciaLaReforma_entonces403SinLlamarAlCasoDeUso()
            throws Exception {
        MvcResult resultado = pedir("2026-I", "DISTRIBUTIVO:LECTURA", "PUBLICACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        verify(REFORMAR, never()).iniciar(any(), any(), any());
    }

    @Test
    void dadoUnPeriodoOUnCuerpoMalFormado_cuandoSeIniciaLaReforma_entonces400Comun()
            throws Exception {
        for (MvcResult resultado :
                List.of(
                        pedir("2026 I!", "DISTRIBUTIVO:APROBACION"),
                        pedirCon("2026-I", "{}", "DISTRIBUTIVO:APROBACION"),
                        pedirCon(
                                "2026-I",
                                "{\"motivo\":\"" + "x".repeat(501) + "\"}",
                                "DISTRIBUTIVO:APROBACION"))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(cuerpo(resultado).get("codigo").asString())
                    .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        }
        verify(REFORMAR, never()).iniciar(any(), any(), any());
    }

    private static JsonNode cuerpo(MvcResult resultado) throws Exception {
        return JSON.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class Dobles {

        @Bean
        FlujoDelDistributivoServicio flujoDelDistributivo() {
            return mock(FlujoDelDistributivoServicio.class);
        }

        @Bean
        IniciarReforma iniciarReforma() {
            return REFORMAR;
        }

        @Bean
        PublicarDistributivo publicarDistributivo() {
            return mock(PublicarDistributivo.class);
        }

        @Bean
        RegistrarResolucion registrarResolucion() {
            return mock(RegistrarResolucion.class);
        }

        @Bean
        ec.mdd.validacionocs.puertos.entrada.DescargarResolucion descargarResolucion() {
            return mock(ec.mdd.validacionocs.puertos.entrada.DescargarResolucion.class);
        }
    }
}
