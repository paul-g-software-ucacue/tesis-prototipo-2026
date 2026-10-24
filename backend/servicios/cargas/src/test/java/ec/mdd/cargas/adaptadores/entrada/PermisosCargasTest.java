package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
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
 * La prueba de autorización por operación de Cargas (RNF-SEG-03): sin «Cargas: lectura» ni
 * «Distributivo del periodo: lectura», cada operación de las plantillas responde 403, y con
 * cualquiera de los dos, no (D-70). Subir un archivo exige «Cargas: escritura» (D-71). La sesión no
 * exige permiso.
 */
@WebMvcTest({SesionControlador.class, PlantillasControlador.class, SubidaControlador.class})
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class PermisosCargasTest {

    private final VerificadorDePermisos verificador = VerificadorDePermisos.para("cargas");

    @Autowired private MockMvc mvc;

    @Autowired private JwtAuthenticationConverter conversor;

    @Test
    void dadoElContratoDeCargas_cuandoSeVerificaCadaOperacion_entoncesExigeUnoDeSusPermisos() {
        assertThat(verificador.operaciones())
                .extracting(VerificadorDePermisos.OperacionProtegida::toString)
                .containsExactlyInAnyOrder(
                        "GET /api/cargas/plantillas ([CARGAS:LECTURA, DISTRIBUTIVO:LECTURA])",
                        "GET /api/cargas/plantillas/estructura_organica/plantilla"
                                + " ([CARGAS:LECTURA, DISTRIBUTIVO:LECTURA])",
                        "GET /api/cargas/plantillas/estructura_organica/diccionario"
                                + " ([CARGAS:LECTURA, DISTRIBUTIVO:LECTURA])",
                        "POST /api/cargas/plantillas/estructura_organica/archivo"
                                + " (CARGAS:ESCRITURA)");

        verificador.verificar(mvc, conversor);
    }
}
