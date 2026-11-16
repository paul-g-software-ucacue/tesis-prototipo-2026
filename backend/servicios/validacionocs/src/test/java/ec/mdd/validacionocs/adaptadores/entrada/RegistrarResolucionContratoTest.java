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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteResolucion;
import ec.mdd.validacionocs.dominio.NumeroDeResolucionInvalido;
import ec.mdd.validacionocs.dominio.ResolucionDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ResolucionFueraDeRango;
import ec.mdd.validacionocs.dominio.ResolucionNoEsPdf;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.RegistrarResolucion;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registrar la resolución del OCS cumple {@code contratos/validacionocs/openapi.yaml} (F15-H01,
 * contracts/validacionocs.md): el 200 con el flujo en APROBADO, cada error con su código y el 403
 * sin el permiso o sin el rol de la etapa. El caso de uso, simulado: sus reglas las prueban {@code
 * RegistrarResolucionServicioTest} y {@code ProcesoAprobacionTest}.
 */
@WebMvcTest(FlujoControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    RegistrarResolucionContratoTest.Dobles.class,
    ErroresValidacionocs.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class RegistrarResolucionContratoTest {

    private static final String RUTA = "/api/validacionocs/periodos/2026-I/resolucion";
    private static final String NUMERO = "RPC-SO-12-No.123-2026";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 14);
    private static final byte[] PDF =
            "%PDF-1.7\nresolucion\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final RegistrarResolucion REGISTRAR = mock(RegistrarResolucion.class);

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("validacionocs");

    @Autowired private MockMvc mvc;

    @BeforeEach
    void reiniciar() {
        reset(REGISTRAR);
    }

    private static MockMultipartHttpServletRequestBuilder completa() {
        return multipart(RUTA)
                .file(new MockMultipartFile("archivo", "resolucion.pdf", "application/pdf", PDF))
                .param("numero", NUMERO)
                .param("fecha", FECHA.toString());
    }

    private MvcResult pedir(MockMultipartHttpServletRequestBuilder peticion, String... permisos)
            throws Exception {
        return mvc.perform(
                        peticion.with(
                                jwt().jwt(una -> una.subject("secretaria.demo"))
                                        .authorities(
                                                java.util.Arrays.stream(permisos)
                                                        .map(
                                                                permiso ->
                                                                        new SimpleGrantedAuthority(
                                                                                "PERMISO_"
                                                                                        + permiso))
                                                        .toArray(SimpleGrantedAuthority[]::new))))
                .andReturn();
    }

    // Con doAnswer: un when(...) volvería a llamar al doble, que todavía lanza el error anterior.
    private static void falla(Class<? extends RuntimeException> error) {
        org.mockito.Mockito.doAnswer(
                        invocacion -> {
                            throw error.getDeclaredConstructor().newInstance();
                        })
                .when(REGISTRAR)
                .registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void dadaLaSecretaria_cuandoRegistraLaResolucion_entonces200ConElFlujoEnAprobado()
            throws Exception {
        when(REGISTRAR.registrar(
                        eq("2026-I"), eq(NUMERO), eq(FECHA), eq("resolucion.pdf"), any(), any()))
                .thenReturn(
                        new FlujoDelDistributivo(
                                "2026-I",
                                TipoDeEtapa.APROBADO,
                                "Aprobado",
                                1,
                                List.of(),
                                List.of(),
                                List.of()));

        MvcResult resultado = pedir(completa(), "RESOLUCION_OCS:ESCRITURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(resultado).get("etapa").asString()).isEqualTo("APROBADO");
        contrato.validar(resultado);
    }

    @Test
    void dadoCadaRechazoDelCasoDeUso_cuandoSeRegistra_entoncesSuCodigoSegunElContrato()
            throws Exception {
        record Caso(Class<? extends RuntimeException> error, int estado, String codigo) {}
        for (Caso caso :
                List.of(
                        new Caso(
                                NumeroDeResolucionInvalido.class,
                                400,
                                "VALIDACIONOCS_NUMERO_DE_RESOLUCION_INVALIDO"),
                        new Caso(
                                ResolucionFueraDeRango.class,
                                400,
                                "VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO"),
                        new Caso(
                                ResolucionNoEsPdf.class, 400, "VALIDACIONOCS_RESOLUCION_NO_ES_PDF"),
                        new Caso(
                                ResolucionDemasiadoGrande.class,
                                413,
                                "VALIDACIONOCS_RESOLUCION_DEMASIADO_GRANDE"),
                        new Caso(
                                EtapaNoAdmiteResolucion.class,
                                409,
                                "VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION"),
                        new Caso(PeriodoCerrado.class, 409, "VALIDACIONOCS_PERIODO_CERRADO"),
                        new Caso(RolDeOtraEtapa.class, 403, "SEGURIDAD_ACCESO_NO_AUTORIZADO"))) {
            falla(caso.error());

            MvcResult resultado = pedir(completa(), "RESOLUCION_OCS:ESCRITURA");

            assertThat(resultado.getResponse().getStatus())
                    .as(caso.codigo())
                    .isEqualTo(caso.estado());
            assertThat(cuerpo(resultado).get("codigo").asString()).isEqualTo(caso.codigo());
            contrato.validar(resultado);
        }
    }

    @Test
    void dadaUnaParteQueFaltaOUnaFechaMalFormada_cuandoSeRegistra_entonces400Comun()
            throws Exception {
        for (MockMultipartHttpServletRequestBuilder peticion :
                List.of(
                        multipart(RUTA)
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "r.pdf", "application/pdf", PDF))
                                .param("fecha", FECHA.toString()),
                        multipart(RUTA).param("numero", NUMERO).param("fecha", FECHA.toString()),
                        multipart(RUTA)
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "r.pdf", "application/pdf", PDF))
                                .param("numero", NUMERO)
                                .param("fecha", "15/11/2026"))) {
            MvcResult resultado = pedir(peticion, "RESOLUCION_OCS:ESCRITURA");

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(cuerpo(resultado).get("codigo").asString())
                    .isEqualTo("COMUN_SOLICITUD_INVALIDA");
        }
        verify(REGISTRAR, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void dadoQuienNoTieneElPermiso_cuandoRegistra_entonces403SinLlamarAlCasoDeUso()
            throws Exception {
        MvcResult resultado = pedir(completa(), "DISTRIBUTIVO:LECTURA", "RESOLUCION_OCS:LECTURA");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        verify(REGISTRAR, never()).registrar(any(), any(), any(), any(), any(), any());
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
        RegistrarResolucion registrarResolucion() {
            return REGISTRAR;
        }

        @Bean
        ec.mdd.validacionocs.puertos.entrada.DescargarResolucion descargarResolucion() {
            return org.mockito.Mockito.mock(
                    ec.mdd.validacionocs.puertos.entrada.DescargarResolucion.class);
        }
    }
}
