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
import ec.mdd.validacionocs.dominio.EtapaNoAdmitePublicacion;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
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
 * Publicar cumple {@code contratos/validacionocs/openapi.yaml} (F18-H01,
 * contracts/validacionocs.md): el 200 con el flujo en PUBLICADO y la constancia, cada error con su
 * código y el 403 sin el permiso o sin el rol de la etapa. El caso de uso, simulado: sus reglas las
 * prueban {@code PublicarDistributivoServicioTest} y {@code ProcesoAprobacionTest}.
 */
@WebMvcTest(FlujoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({PublicarContratoTest.Dobles.class, ErroresValidacionocs.class, ErroresEntreProcesos.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PublicarContratoTest {

    private static final String RUTA = "/api/validacionocs/periodos/%s/publicacion";
    private static final Instant PUBLICADA = Instant.parse("2026-11-18T15:00:00Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final PublicarDistributivo PUBLICAR = mock(PublicarDistributivo.class);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void reiniciar() {
        reset(PUBLICAR);
    }

    private MvcResult pedir(String periodo, String... permisos) throws Exception {
        return mvc.perform(
                        post(RUTA.formatted(periodo))
                                .with(
                                        jwt().jwt(una -> una.subject("secretaria.demo"))
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
                .when(PUBLICAR)
                .publicar(any(), any());
    }

    @Test
    void dadaLaSecretaria_cuandoPublica_entonces200ConElFlujoEnPublicadoYLaConstancia()
            throws Exception {
        when(PUBLICAR.publicar(eq("2026-I"), any()))
                .thenReturn(
                        new FlujoDelDistributivo(
                                "2026-I",
                                TipoDeEtapa.PUBLICADO,
                                "Publicado",
                                1,
                                List.of(),
                                List.of(),
                                List.of(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(
                                        new FlujoDelDistributivo.Publicacion(
                                                1, "secretaria.demo", PUBLICADA))));

        MvcResult resultado = pedir("2026-I", "PUBLICACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = cuerpo(resultado);
        assertThat(cuerpo.get("etapa").asString()).isEqualTo("PUBLICADO");
        assertThat(cuerpo.get("acciones")).isEmpty();
        assertThat(cuerpo.get("publicacion").get("version").asInt()).isEqualTo(1);
        assertThat(cuerpo.get("publicacion").get("publicadaPor").asString())
                .isEqualTo("secretaria.demo");
        assertThat(cuerpo.get("publicacion").get("publicadaEn").asString())
                .isEqualTo("2026-11-18T15:00:00Z");
        contrato.validar(resultado);
    }

    @Test
    void dadoCadaRechazoDelCasoDeUso_cuandoSePublica_entoncesSuCodigoSegunElContrato()
            throws Exception {
        record Caso(Class<? extends RuntimeException> error, int estado, String codigo) {}
        for (Caso caso :
                List.of(
                        new Caso(
                                EtapaNoAdmitePublicacion.class,
                                409,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION"),
                        new Caso(PeriodoCerrado.class, 409, "VALIDACIONOCS_PERIODO_CERRADO"),
                        new Caso(RolDeOtraEtapa.class, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO"))) {
            falla(caso.error());

            MvcResult resultado = pedir("2026-I", "PUBLICACION:APROBACION");

            assertThat(resultado.getResponse().getStatus())
                    .as(caso.codigo())
                    .isEqualTo(caso.estado());
            assertThat(cuerpo(resultado).get("codigo").asString()).isEqualTo(caso.codigo());
            contrato.validar(resultado);
        }
    }

    @Test
    void dadoQuienNoTieneElPermiso_cuandoPublica_entonces403SinLlamarAlCasoDeUso()
            throws Exception {
        MvcResult resultado =
                pedir("2026-I", "DISTRIBUTIVO:APROBACION", "RESOLUCION_OCS:ESCRITURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        verify(PUBLICAR, never()).publicar(any(), any());
    }

    @Test
    void dadoUnPeriodoMalFormado_cuandoSePublica_entonces400() throws Exception {
        MvcResult resultado = pedir("2026 I!", "PUBLICACION:APROBACION");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(resultado).get("codigo").asString())
                .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        verify(PUBLICAR, never()).publicar(any(), any());
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
        PublicarDistributivo publicarDistributivo() {
            return PUBLICAR;
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
