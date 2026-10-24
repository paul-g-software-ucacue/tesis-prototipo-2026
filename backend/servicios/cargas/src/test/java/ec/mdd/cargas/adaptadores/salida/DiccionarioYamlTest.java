package ec.mdd.cargas.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.ColumnaDeCarga;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.Obligatoriedad;
import ec.mdd.cargas.dominio.ValorDeCatalogo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * El diccionario de datos versionado, leído como en producción y comparado con los encabezados de
 * los anexos de la ERS (research §2 y §4): una columna mal transcrita no pasa.
 */
class DiccionarioYamlTest {

    private static DiccionarioDeDatos diccionario;

    @BeforeAll
    static void leer() {
        diccionario = new DiccionarioYaml().leer();
    }

    @Test
    void dadoElYamlVersionado_cuandoSeLee_entoncesTieneLasDiezCargasDeLaErsEnOrden() {
        assertThat(diccionario.cargas())
                .extracting(Carga::nombre)
                .containsExactly(
                        "Estructura orgánica",
                        "Carreras",
                        "Ciclos académicos aprobados",
                        "Asignaturas y malla",
                        "Oferta del periodo",
                        "Docentes contratados (con ficha académica)",
                        "Horas según la política de contratación",
                        "Cargos y autoridades",
                        "Proyectos y prácticas aprobados",
                        "Matriculados por carrera");
        assertThat(diccionario.cargas())
                .extracting(carga -> carga.columnas().size())
                .containsExactly(7, 12, 8, 14, 9, 26, 14, 8, 12, 5);
        assertThat(diccionario.columnas()).isEqualTo(115);
    }

    @Test
    void dadoElYamlVersionado_cuandoSeComparaConLosAnexosDeLaErs_entoncesLosEncabezadosSonIguales()
            throws IOException {
        Map<String, List<String>> anexo = anexo();

        assertThat(diccionario.cargas())
                .extracting(Carga::archivo)
                .containsExactlyElementsOf(anexo.keySet());
        for (Carga carga : diccionario.cargas()) {
            assertThat(carga.plantilla().encabezados())
                    .as(carga.archivo())
                    .containsExactlyElementsOf(anexo.get(carga.archivo()));
            assertThat(carga.codigo() + ".csv").isEqualTo(carga.archivo().substring(3));
        }
    }

    @Test
    void dadoElYamlVersionado_cuandoSeLee_entoncesLaObligatoriedadYLasDependenciasSonLasDeLaErs() {
        assertThat(diccionario.cargas())
                .extracting(Carga::obligatoriedad)
                .containsExactly(
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA,
                        Obligatoriedad.OBLIGATORIA_SALVO_SIN_PROYECTOS,
                        Obligatoriedad.RECOMENDADA);
        assertThat(diccionario.cargas())
                .extracting(Carga::dependeDe)
                .containsExactly(
                        List.of(),
                        List.of(1),
                        List.of(),
                        List.of(2),
                        List.of(3, 4),
                        List.of(1, 2),
                        List.of(),
                        List.of(1, 2, 6),
                        List.of(2, 3, 6),
                        List.of(2, 3));
    }

    @Test
    void dadoElYamlVersionado_cuandoSeLee_entoncesTraeLasConvencionesYLosCatalogos() {
        assertThat(diccionario.convenciones()).hasSize(13);
        assertThat(diccionario.catalogos())
                .extracting(catalogo -> catalogo.codigo())
                .containsExactly(
                        "GESTIONES_AUTORIZADAS",
                        "CARGOS",
                        "PERFILES_DE_CARGA",
                        "CAMPOS_AMPLIOS_CINE",
                        "TIPOS_DE_IST");
        ColumnaDeCarga gestion = columna(1, "codigo_gestion_autorizada");
        assertThat(gestion.obligatorio()).isEqualTo("Sí si tipo_unidad = GESTION");
        assertThat(diccionario.valoresDe(gestion))
                .hasSize(21)
                .first()
                .isEqualTo(new ValorDeCatalogo("G01", "Rector"));
        assertThat(diccionario.valoresDe(columna(7, "perfil"))).hasSize(11);
    }

    @Test
    void dadoElYamlVersionado_cuandoSeLee_entoncesNingunTextoRemiteAHojasQueNoViajan() {
        for (Carga carga : diccionario.cargas()) {
            for (ColumnaDeCarga columna : carga.columnas()) {
                assertThat(columna.toString()).doesNotContainIgnoringCase("hoja Catálogos");
            }
        }
        assertThat(diccionario.convenciones().toString())
                .doesNotContainIgnoringCase("hoja Catálogos")
                .doesNotContain("del índice");
    }

    @Test
    void dadasLasColumnasSinEjemploEnLaErs_cuandoSeLee_entoncesTienenElEjemploEscritoAMano() {
        assertThat(columna(3, "fecha_limite_envio_senescyt").ejemplo()).isEqualTo("2026-03-07");
        assertThat(columna(4, "creditos").ejemplo()).isEqualTo("3.5");
        assertThat(columna(5, "codigo_unidad_sede").ejemplo()).isEqualTo("MATRIZ");
        assertThat(columna(5, "horas_semanales").ejemplo()).isEqualTo("4");
        assertThat(columna(6, "telefono").ejemplo()).isEqualTo("0999999999");
        assertThat(columna(6, "registro_senescyt_3n").ejemplo()).isEqualTo("1031-2020-0000001");
    }

    @Test
    void dadoUnYamlConUnError_cuandoSeLee_entoncesFallaConElMotivo() {
        String sinCargas = "convenciones: []\ncatalogos: []\ncargas: []\n";
        assertThatThrownBy(() -> DiccionarioYaml.leer(entrada(sinCargas)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 cargas");
        String columnaSinTipo =
                """
                convenciones: []
                catalogos: []
                cargas:
                  - numero: 1
                    codigo: estructura_organica
                    nombre: Estructura orgánica
                    archivo: 01_estructura_organica.csv
                    dependeDe: []
                    obligatoriedad: OBLIGATORIA
                    columnas:
                      - campo: codigo_unidad
                        descripcion: Código
                        obligatorio: Sí
                        ejemplo: MATRIZ
                """;
        assertThatThrownBy(() -> DiccionarioYaml.leer(entrada(columnaSinTipo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carga 1")
                .hasMessageContaining("codigo_unidad")
                .hasMessageContaining("tipo");
    }

    private static ColumnaDeCarga columna(int numero, String campo) {
        return diccionario.cargas().get(numero - 1).columnas().stream()
                .filter(columna -> columna.campo().equals(campo))
                .findFirst()
                .orElseThrow();
    }

    private static InputStream entrada(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }

    /** Los encabezados de {@code cargas/plantillas/} de la ERS, por archivo, en orden. */
    private static Map<String, List<String>> anexo() throws IOException {
        Map<String, List<String>> anexo = new LinkedHashMap<>();
        try (InputStream archivo =
                DiccionarioYamlTest.class.getResourceAsStream(
                        "/cargas/anexo-ers-encabezados.csv")) {
            String texto = new String(archivo.readAllBytes(), StandardCharsets.UTF_8);
            for (String linea : texto.strip().split("\n")) {
                List<String> partes = Arrays.asList(linea.strip().split(","));
                anexo.put(partes.getFirst(), partes.subList(1, partes.size()));
            }
        }
        return anexo;
    }
}
