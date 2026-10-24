package ec.mdd.cargas.adaptadores.entrada;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.jayway.jsonpath.JsonPath;
import ec.mdd.cargas.adaptadores.salida.DatosDePrueba;
import ec.mdd.cargas.adaptadores.salida.LibroDePrueba;
import ec.mdd.cargas.infraestructura.ConfiguracionCargas;
import ec.mdd.plataforma.errores.ErroresAutoConfiguracion;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import ec.mdd.plataforma.seguridad.SeguridadProcesoAutoConfiguracion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Subir el archivo de una carga cumple {@code contratos/cargas/openapi.yaml} (principio IV,
 * F01-H02), con los lectores, el diccionario y los datos de prueba de la ERS reales.
 */
@WebMvcTest(SubidaControlador.class)
@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})
@Import({ConfiguracionCargas.class, ErroresCargas.class})
@TestPropertySource(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks")
class SubidaContratoTest {

    private final ValidadorDeContrato contrato = ValidadorDeContrato.para("cargas");

    @Autowired private MockMvc mvc;

    @Test
    void dadoElCsvDeDocentes_cuandoSeSube_entonces200ConSus47Registros() throws Exception {
        MvcResult resultado =
                subir("docentes", "06_docentes.csv", DatosDePrueba.contenido("06_docentes.csv"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("carga", "docentes")
                .containsEntry("archivo", "06_docentes.csv")
                .containsEntry("formato", "CSV")
                .containsEntry("registros", 47);
        contrato.validar(resultado);
    }

    @Test
    void dadoElMismoArchivoEnXlsx_cuandoSeSube_entonces200ConLosMismosRegistros() throws Exception {
        // Los matriculados no tienen comillas: cada línea se parte por las comas.
        String csv =
                new String(DatosDePrueba.contenido("10_matriculados_carrera.csv"), UTF_8).strip();
        List<String> lineas = List.of(csv.split("\r\n"));
        byte[] xlsx =
                LibroDePrueba.deTexto(
                        List.of(lineas.getFirst().split(",", -1)),
                        lineas.subList(1, lineas.size()).stream()
                                .map(linea -> List.of(linea.split(",", -1)))
                                .toList());

        MvcResult resultado = subir("matriculados_carrera", "10_matriculados_carrera.xlsx", xlsx);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$"))
                .containsEntry("formato", "XLSX")
                .containsEntry("registros", 7);
        contrato.validar(resultado);
    }

    @Test
    void dadoElArchivoMasGrandeDePrueba_cuandoSeSube_entoncesSeLeeEnMenosDeUnSegundo()
            throws Exception {
        long inicio = System.nanoTime();
        MvcResult resultado =
                subir(
                        "asignaturas_malla",
                        "04_asignaturas_malla.csv",
                        DatosDePrueba.contenido("04_asignaturas_malla.csv"));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.registros")).isEqualTo(300);
        assertThat(milisegundos).isLessThan(1000);
    }

    @Test
    void dadaUnaPeticionSinLaParteArchivo_cuandoSeSube_entonces400Comun() throws Exception {
        MvcResult resultado =
                mvc.perform(multipart("/api/cargas/plantillas/docentes/archivo").with(escritura()))
                        .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(resultado)).contains("COMUN_SOLICITUD_INVALIDA");
    }

    @Test
    void dadaUnaCargaQueNoExiste_cuandoSeSube_entonces404() throws Exception {
        MvcResult resultado = subir("comisiones", "c.csv", "a\n1\n".getBytes(UTF_8));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado)).contains("COMUN_RECURSO_NO_ENCONTRADO");
        contrato.validar(resultado);
    }

    MvcResult subir(String carga, String nombre, byte[] contenido) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion =
                multipart("/api/cargas/plantillas/" + carga + "/archivo");
        peticion.file(
                new MockMultipartFile("archivo", nombre, "application/octet-stream", contenido));
        return mvc.perform(peticion.with(escritura())).andReturn();
    }

    static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(UTF_8);
    }

    /** El Responsable de consolidación, con «Cargas: escritura» desde D-71. */
    static JwtRequestPostProcessor escritura() {
        return jwt().jwt(token -> token.subject("consolidacion.demo"))
                .authorities(
                        new SimpleGrantedAuthority("PERMISO_CARGAS:LECTURA"),
                        new SimpleGrantedAuthority("PERMISO_CARGAS:ESCRITURA"),
                        new SimpleGrantedAuthority("PERMISO_DISTRIBUTIVO:LECTURA"));
    }
}
