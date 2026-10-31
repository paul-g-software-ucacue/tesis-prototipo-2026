package ec.mdd.generacion.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.eventos.ErroresEntreProcesos;
import ec.mdd.plataforma.pruebas.VerificadorDePermisos;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La prueba de autorización por operación de Generación (RNF-SEG-03, FR-014): sin uno de los tres
 * permisos de leer un docente, la afinidad y la sugerencia responden 403, y con cualquiera de ellos
 * pasan.
 */
@WebMvcTest({AfinidadControlador.class, SugerenciaControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({
    AfinidadContratoTest.Dobles.class,
    SugerenciaContratoTest.Dobles.class,
    ErroresGeneracion.class,
    ErroresEntreProcesos.class
})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosGeneracionTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("generacion");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeGeneracion_cuandoSeVerificaCadaOperacion_entoncesExigeSuPermiso() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .hasSize(2)
                .allSatisfy(
                        operacion ->
                                assertThat(operacion)
                                        .contains(
                                                "DOCENTES:LECTURA",
                                                "DISTRIBUTIVO:LECTURA",
                                                "CATALOGOS:LECTURA"))
                .anySatisfy(operacion -> assertThat(operacion).contains("/afinidad"))
                .anySatisfy(operacion -> assertThat(operacion).contains("/sugerencia"));

        verificador.verificar(mvc, conversor);
    }
}
