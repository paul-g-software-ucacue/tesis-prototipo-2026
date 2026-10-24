package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Cada lista de valores permitidos de las carreras y las asignaturas vive en una enumeración, y es
 * la misma del diccionario de datos de las cargas 2 y 4 (research §2). El diccionario se lee como
 * archivo, sin depender del módulo Cargas (principio V): si cambia, esta prueba falla.
 */
class ValoresPermitidosTest {

    private static final Path DICCIONARIO =
            Path.of("../cargas/src/main/resources/cargas/diccionario-de-datos.yaml");

    private static Map<?, ?> raiz;

    @BeforeAll
    static void leer() throws IOException {
        try (Reader lector = Files.newBufferedReader(DICCIONARIO, StandardCharsets.UTF_8)) {
            raiz = new Yaml(new SafeConstructor(new LoaderOptions())).load(lector);
        }
    }

    @Test
    void dadasLasEnumeracionesDeLaCarrera_cuandoSeComparanConLaCarga2_entoncesSonIguales() {
        assertThat(nombres(NivelDeFormacion.values()))
                .containsExactlyElementsOf(formato("carreras", "nivel_formacion"));
        assertThat(nombres(Modalidad.values()))
                .containsExactlyElementsOf(formato("carreras", "modalidad"));
        assertThat(nombres(Jornada.values()))
                .containsExactlyElementsOf(formato("carreras", "jornadas"));
        assertThat(nombres(EstadoDeCarrera.values()))
                .containsExactlyElementsOf(formato("carreras", "estado"));
    }

    @Test
    void dadasLasEnumeracionesDeLaAsignatura_cuandoSeComparanConLaCarga4_entoncesSonIguales() {
        assertThat(nombres(UnidadDeOrganizacionCurricular.values()))
                .containsExactlyElementsOf(
                        formato("asignaturas_malla", "unidad_organizacion_curricular"));
        // El estado de la asignatura se guarda como «activa»: ACTIVA es true e INACTIVA, false.
        assertThat(formato("asignaturas_malla", "estado")).containsExactly("ACTIVA", "INACTIVA");
    }

    @Test
    void dadosLosCamposAmplios_cuandoSeComparanConSuCatalogo_entoncesSonLos11DeCine() {
        assertThat(camposAmplios()).hasSize(11).startsWith("00").endsWith("10");
    }

    /** Los códigos del catálogo CAMPOS_AMPLIOS_CINE del diccionario, para la migración V4. */
    static List<String> camposAmplios() {
        return catalogo("CAMPOS_AMPLIOS_CINE").stream()
                .map(valor -> String.valueOf(valor.get("codigo")))
                .toList();
    }

    /** Los valores del catálogo, con su código y su descripción. */
    static List<Map<?, ?>> catalogo(String codigo) {
        return lista(raiz, "catalogos").stream()
                .filter(catalogo -> codigo.equals(catalogo.get("codigo")))
                .findFirst()
                .map(catalogo -> lista(catalogo, "valores"))
                .orElseThrow();
    }

    private static List<String> formato(String carga, String campo) {
        Map<?, ?> laCarga =
                lista(raiz, "cargas").stream()
                        .filter(una -> carga.equals(una.get("codigo")))
                        .findFirst()
                        .orElseThrow();
        Map<?, ?> columna =
                lista(laCarga, "columnas").stream()
                        .filter(una -> campo.equals(una.get("campo")))
                        .findFirst()
                        .orElseThrow();
        return Arrays.stream(String.valueOf(columna.get("formato")).split("[,;]"))
                .map(String::strip)
                .toList();
    }

    private static List<String> nombres(Enum<?>[] valores) {
        return Stream.of(valores).map(Enum::name).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> lista(Map<?, ?> padre, String clave) {
        return (List<Map<?, ?>>) padre.get(clave);
    }
}
