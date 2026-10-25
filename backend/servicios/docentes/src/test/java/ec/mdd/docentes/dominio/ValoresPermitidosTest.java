package ec.mdd.docentes.dominio;

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
 * Cada lista de valores permitidos del docente vive en una enumeración, y es la misma de la carga 6
 * del diccionario de datos (FR-012, research §9). El diccionario se lee como archivo, sin depender
 * del módulo Cargas (principio V): si cambia, esta prueba falla.
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
    void dadasLasEnumeracionesDelDocente_cuandoSeComparanConLaCarga6_entoncesSonIguales() {
        assertThat(nombres(TipoDeIdentificacion.values()))
                .containsExactlyElementsOf(formato("tipo_identificacion"));
        assertThat(nombres(TipoPersonal.values()))
                .containsExactlyElementsOf(formato("tipo_personal"));
        assertThat(nombres(Dedicacion.values())).containsExactlyElementsOf(formato("dedicacion"));
        assertThat(nombres(CondicionEspecial.values()))
                .containsExactlyElementsOf(formato("condicion_especial"));
        assertThat(nombres(EstadoLaboral.values())).containsExactlyElementsOf(formato("estado"));
    }

    @Test
    void dadaLaCategoria_cuandoSeComparaConLaCarga6_entoncesExpandeSusNiveles() {
        // El diccionario abrevia «AGREGADO_1..3, PRINCIPAL_1..3».
        List<String> expandida =
                formato("categoria_escalafon").stream()
                        .flatMap(
                                valor ->
                                        valor.endsWith("_1..3")
                                                ? Stream.of(1, 2, 3)
                                                        .map(n -> valor.replace("1..3", "" + n))
                                                : Stream.of(valor))
                        .toList();

        assertThat(nombres(CategoriaDeEscalafon.values())).containsExactlyElementsOf(expandida);
    }

    @Test
    void dadoElTipoDeTituloDeCuartoNivel_cuandoSeComparaConLaCarga6_entoncesEsElMismo() {
        String formato = String.valueOf(columna("titulos_cuarto_nivel").get("formato"));

        assertThat(formato.substring(formato.indexOf(':') + 1).split(","))
                .extracting(String::strip)
                .containsExactlyElementsOf(nombres(TipoDeTituloDeCuartoNivel.values()));
    }

    @Test
    void dadasLasEnumeracionesDelCargo_cuandoSeComparanConLaCarga8_entoncesSonIguales() {
        // Los 22 del catálogo CARGOS: los 16 de gestor son los de D-73.
        List<String> cargos =
                lista(raiz, "catalogos").stream()
                        .filter(catalogo -> "CARGOS".equals(catalogo.get("codigo")))
                        .flatMap(catalogo -> lista(catalogo, "valores").stream())
                        .map(valor -> String.valueOf(valor.get("codigo")))
                        .toList();

        assertThat(nombres(TipoDeCargo.values())).containsExactlyElementsOf(cargos);
        assertThat(nombres(EstadoDeCargo.values()))
                .containsExactlyElementsOf(formato("cargos_autoridades", "estado"));
    }

    @Test
    void dadasLasEnumeracionesDelProyecto_cuandoSeComparanConLaCarga9_entoncesSonIguales() {
        assertThat(nombres(TipoDeProyecto.values()))
                .containsExactlyElementsOf(formato("proyectos_practicas", "tipo_proyecto"));
        assertThat(nombres(EstadoDeProyecto.values()))
                .containsExactlyElementsOf(formato("proyectos_practicas", "estado"));
        String roles =
                String.valueOf(columna("proyectos_practicas", "participantes").get("formato"));
        assertThat(roles.substring(roles.indexOf(':') + 1).split(","))
                .extracting(String::strip)
                .containsExactlyElementsOf(nombres(RolDeParticipante.values()));
    }

    private static List<String> formato(String carga, String campo) {
        return Arrays.stream(String.valueOf(columna(carga, campo).get("formato")).split("[,;]"))
                .map(String::strip)
                .toList();
    }

    private static Map<?, ?> columna(String carga, String campo) {
        Map<?, ?> una =
                lista(raiz, "cargas").stream()
                        .filter(otra -> carga.equals(otra.get("codigo")))
                        .findFirst()
                        .orElseThrow();
        return lista(una, "columnas").stream()
                .filter(otra -> campo.equals(otra.get("campo")))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> formato(String campo) {
        return Arrays.stream(String.valueOf(columna(campo).get("formato")).split("[,;]"))
                .map(String::strip)
                .toList();
    }

    private static Map<?, ?> columna(String campo) {
        Map<?, ?> carga =
                lista(raiz, "cargas").stream()
                        .filter(una -> "docentes".equals(una.get("codigo")))
                        .findFirst()
                        .orElseThrow();
        return lista(carga, "columnas").stream()
                .filter(una -> campo.equals(una.get("campo")))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> nombres(Enum<?>[] valores) {
        return Stream.of(valores).map(Enum::name).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> lista(Map<?, ?> padre, String clave) {
        return (List<Map<?, ?>>) padre.get(clave);
    }
}
