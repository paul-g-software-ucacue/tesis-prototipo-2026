package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioDePrueba.columna;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.columnaConCatalogo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Una columna del diccionario de datos y sus reglas (RF-05, research §3), sin dobles. */
class ColumnaDeCargaTest {

    @Test
    void dadosDatosValidos_cuandoSeCrea_entoncesLosOpcionalesVaciosQuedanEnBlanco() {
        ColumnaDeCarga columna =
                new ColumnaDeCarga(
                        "telefono",
                        "Teléfono",
                        "Texto",
                        null,
                        "No",
                        null,
                        null,
                        null,
                        "0999",
                        null);

        assertThat(columna.longitud()).isNull();
        assertThat(columna.formato()).isEmpty();
        assertThat(columna.clave()).isEmpty();
        assertThat(columna.validacion()).isEmpty();
        assertThat(columna.catalogo()).isEmpty();
        assertThat(columnaConCatalogo("codigo_gestion", "GESTIONES").catalogo())
                .contains("GESTIONES");
    }

    @Test
    void dadoUnCampoConMayusculasEspaciosOVacio_cuandoSeCrea_entoncesSeRechaza() {
        for (String campo :
                new String[] {"Codigo", "codigo unidad", "código", "", "codigo-unidad"}) {
            assertThatThrownBy(() -> columna(campo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minúsculas");
        }
        assertThat(columna("campo_amplio_3n").campo()).isEqualTo("campo_amplio_3n");
    }

    @Test
    void dadaUnaColumnaSinTipoObligatoriedadEjemploODescripcion_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () -> new ColumnaDeCarga("campo", "D", " ", 1, "Sí", "", "", "", "A", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campo")
                .hasMessageContaining("tipo");
        assertThatThrownBy(
                        () ->
                                new ColumnaDeCarga(
                                        "campo", "D", "Texto", 1, "", "", "", "", "A", null))
                .hasMessageContaining("obligatoriedad");
        assertThatThrownBy(
                        () ->
                                new ColumnaDeCarga(
                                        "campo", "D", "Texto", 1, "Sí", "", "", "", null, null))
                .hasMessageContaining("ejemplo");
        assertThatThrownBy(
                        () ->
                                new ColumnaDeCarga(
                                        "campo", "", "Texto", 1, "Sí", "", "", "", "A", null))
                .hasMessageContaining("descripción");
    }

    @Test
    void dadaUnaLongitudMenorQueUno_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new ColumnaDeCarga(
                                        "campo", "D", "Texto", 0, "Sí", "", "", "", "A", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitud");
    }

    @Test
    void dadoUnTextoQueEmpiezaComoFormula_cuandoSeCrea_entoncesSeRechaza() {
        for (String formula : new String[] {"=1+1", "+57", "-3", "@SUMA", "\tA", "\rA"}) {
            assertThatThrownBy(
                            () ->
                                    new ColumnaDeCarga(
                                            "campo", "D", "Texto", 1, "Sí", "", "", "", formula,
                                            null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fórmula");
            assertThatThrownBy(
                            () ->
                                    new ColumnaDeCarga(
                                            "campo", formula, "Texto", 1, "Sí", "", "", "", "A",
                                            null))
                    .hasMessageContaining("fórmula");
        }
    }
}
