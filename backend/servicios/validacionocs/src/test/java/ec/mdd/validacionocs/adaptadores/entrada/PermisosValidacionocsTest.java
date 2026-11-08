package ec.mdd.validacionocs.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.VerificadorDePermisos;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La prueba de autorización por operación (RNF-SEG-03): sin el permiso que declara el contrato,
 * cada operación responde 403, y con él pasa (FR-021, FR-022).
 */
@WebMvcTest({ConfiguracionControlador.class, FlujoControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    ConfiguracionContratoTest.Dobles.class,
    PermisosValidacionocsTest.FlujoFijo.class,
    ErroresValidacionocs.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosValidacionocsTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("validacionocs");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContrato_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .hasSize(7)
                .anySatisfy(operacion -> assertThat(operacion).contains("FLUJO_FIRMAS:LECTURA"))
                .anySatisfy(operacion -> assertThat(operacion).contains("FLUJO_FIRMAS:ESCRITURA"))
                .anySatisfy(operacion -> assertThat(operacion).contains("PROYECTOS:LECTURA"))
                .anySatisfy(operacion -> assertThat(operacion).contains("OFERTA:APROBACION"))
                .anySatisfy(
                        operacion -> assertThat(operacion).contains("CONSOLIDACION:APROBACION"));

        verificador.verificar(mvc, conversor);
    }

    /**
     * El flujo con una respuesta fija (F12-H02): esta prueba mira solo el permiso del contrato; el
     * rol de la etapa y la carrera los prueba {@code FlujoContratoTest}.
     */
    @TestConfiguration
    static class FlujoFijo {

        @Bean
        FlujoDelDistributivoServicio flujoDelDistributivo() {
            FlujoDelDistributivoServicio servicio = mock(FlujoDelDistributivoServicio.class);
            FlujoDelDistributivo fijo =
                    new FlujoDelDistributivo(
                            "2026-I",
                            TipoDeEtapa.BORRADOR_CARRERA,
                            "Propuesta de carrera",
                            1,
                            List.of(),
                            List.of(),
                            List.of());
            when(servicio.flujo(anyString(), any())).thenReturn(fijo);
            when(servicio.enviar(anyString(), anyString(), any())).thenReturn(fijo);
            when(servicio.avanzar(anyString(), any())).thenReturn(fijo);
            when(servicio.enviarARevision(anyString(), any())).thenReturn(fijo);
            when(servicio.devolver(anyString(), any(), any())).thenReturn(fijo);
            return servicio;
        }
    }
}
