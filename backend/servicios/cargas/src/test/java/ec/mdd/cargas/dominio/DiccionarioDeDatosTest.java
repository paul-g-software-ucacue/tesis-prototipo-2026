package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioDePrueba.carga;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.columna;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.columnaConCatalogo;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.convenciones;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.diccionario;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.diezCargas;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.gestiones;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El diccionario de datos de las 10 cargas y sus reglas (RF-05, ERS §8.3), sin dobles. */
class DiccionarioDeDatosTest {

    @Test
    void dadasLasDiezCargas_cuandoSeCrea_entoncesLasDevuelveEnOrdenYBuscaPorCodigo() {
        DiccionarioDeDatos diccionario = diccionario(diezCargas());

        assertThat(diccionario.cargas())
                .extracting(Carga::numero)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(diccionario.carga("carga_c")).map(Carga::numero).contains(3);
        assertThat(diccionario.carga("no_existe")).isEmpty();
        assertThat(diccionario.columnas()).isEqualTo(10);
        assertThat(diccionario.convenciones()).hasSize(1);
    }

    @Test
    void dadasNueveOOnceCargas_cuandoSeCrea_entoncesSeRechaza() {
        List<Carga> nueve = new ArrayList<>(diezCargas().subList(0, 9));
        assertThatThrownBy(() -> diccionario(nueve))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 cargas");
        List<Carga> once = new ArrayList<>(diezCargas());
        once.add(carga(10, List.of()));
        assertThatThrownBy(() -> diccionario(once)).hasMessageContaining("10 cargas");
    }

    @Test
    void dadoUnNumeroRepetidoOFueraDeOrden_cuandoSeCrea_entoncesSeRechaza() {
        List<Carga> cargas = new ArrayList<>(diezCargas());
        cargas.set(4, carga(4, List.of()));
        assertThatThrownBy(() -> diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posición 5");
    }

    @Test
    void dadoUnNumeroFueraDeUnoADiez_cuandoSeCreaLaCarga_entoncesSeRechaza() {
        assertThatThrownBy(() -> carga(0, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 a 10");
        assertThatThrownBy(() -> carga(11, List.of())).hasMessageContaining("1 a 10");
    }

    @Test
    void dadoUnCodigoOUnArchivoRepetido_cuandoSeCrea_entoncesSeRechaza() {
        List<Carga> cargas = new ArrayList<>(diezCargas());
        Carga segunda = cargas.get(1);
        cargas.set(
                1,
                new Carga(
                        2,
                        "carga_a",
                        "Carga 2",
                        segunda.archivo(),
                        List.of(),
                        Obligatoriedad.OBLIGATORIA,
                        segunda.columnas()));
        assertThatThrownBy(() -> diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código carga_a");

        List<Carga> otras = new ArrayList<>(diezCargas());
        otras.set(
                1,
                new Carga(
                        2,
                        "carga_b",
                        "Carga 2",
                        otras.get(0).archivo(),
                        List.of(),
                        Obligatoriedad.OBLIGATORIA,
                        segunda.columnas()));
        assertThatThrownBy(() -> diccionario(otras)).hasMessageContaining("archivo");
    }

    @Test
    void dadaUnaDependenciaHaciaUnaCargaPosteriorOInexistente_cuandoSeCrea_entoncesSeRechaza() {
        List<Carga> cargas = new ArrayList<>(diezCargas());
        cargas.set(2, carga(3, List.of(4)));
        assertThatThrownBy(() -> diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carga 3")
                .hasMessageContaining("anterior");
        List<Carga> otras = new ArrayList<>(diezCargas());
        otras.set(2, carga(3, List.of(0)));
        assertThatThrownBy(() -> diccionario(otras)).hasMessageContaining("anterior");
        List<Carga> validas = new ArrayList<>(diezCargas());
        validas.set(4, carga(5, List.of(3, 4)));
        assertThat(diccionario(validas).carga("carga_e").orElseThrow().dependeDe())
                .containsExactly(3, 4);
    }

    @Test
    void dadaUnaCargaSinColumnasOConUnCampoRepetido_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new Carga(
                                        1,
                                        "carga_a",
                                        "Carga 1",
                                        "01_carga.csv",
                                        List.of(),
                                        Obligatoriedad.OBLIGATORIA,
                                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carga 1")
                .hasMessageContaining("columna");
        assertThatThrownBy(() -> carga(1, List.of(), columna("codigo"), columna("codigo")))
                .hasMessageContaining("codigo")
                .hasMessageContaining("repite");
    }

    @Test
    void dadoUnCodigoDeCargaConOtroFormato_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new Carga(
                                        1,
                                        "Estructura-Organica",
                                        "Carga 1",
                                        "01_carga.csv",
                                        List.of(),
                                        Obligatoriedad.OBLIGATORIA,
                                        List.of(columna("codigo"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código");
    }

    @Test
    void dadaUnaColumnaConUnCatalogoQueNoExiste_cuandoSeCrea_entoncesSeRechaza() {
        List<Carga> cargas = new ArrayList<>(diezCargas());
        cargas.set(0, carga(1, List.of(), columnaConCatalogo("codigo_gestion", "CARGOS")));
        assertThatThrownBy(() -> diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carga 1")
                .hasMessageContaining("codigo_gestion")
                .hasMessageContaining("CARGOS");
    }

    @Test
    void dadaUnaColumnaConCatalogo_cuandoSePidenSusValores_entoncesSonLosDelCatalogo() {
        ColumnaDeCarga conCatalogo = columnaConCatalogo("codigo_gestion", "GESTIONES");
        List<Carga> cargas = new ArrayList<>(diezCargas());
        cargas.set(0, carga(1, List.of(), conCatalogo));
        DiccionarioDeDatos diccionario = diccionario(cargas);

        assertThat(diccionario.valoresDe(conCatalogo))
                .extracting(ValorDeCatalogo::codigo)
                .containsExactly("G01", "G02");
        assertThat(diccionario.valoresDe(columna("codigo"))).isEmpty();
    }

    @Test
    void dadaUnaCarga_cuandoSePideSuPlantilla_entoncesTraeSusEncabezadosEnOrden() {
        Carga carga = carga(1, List.of(), columna("codigo_unidad"), columna("nombre"));

        PlantillaCarga plantilla = carga.plantilla();

        assertThat(plantilla.archivo()).isEqualTo("01_carga.csv");
        assertThat(plantilla.encabezados()).containsExactly("codigo_unidad", "nombre");
        assertThat(carga.archivoDelDiccionario()).isEqualTo("01_carga_diccionario.csv");
    }

    @Test
    void dadoUnCatalogoVacioOConUnValorRepetido_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new Catalogo("VACIO", "Vacío", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VACIO");
        ValorDeCatalogo g01 = new ValorDeCatalogo("G01", "Rector");
        assertThatThrownBy(() -> new Catalogo("GESTIONES", "Gestiones", List.of(g01, g01)))
                .hasMessageContaining("G01");
        assertThatThrownBy(
                        () ->
                                new DiccionarioDeDatos(
                                        diezCargas(),
                                        convenciones(),
                                        List.of(gestiones(), gestiones())))
                .hasMessageContaining("GESTIONES");
    }

    @Test
    void dadaUnaConvencionOUnValorSinTexto_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new Convencion("Fechas", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValorDeCatalogo("", "Rector"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Convencion("Fechas", "=HOY()"))
                .hasMessageContaining("fórmula");
    }
}
