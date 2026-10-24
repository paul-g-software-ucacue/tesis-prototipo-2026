package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioDePrueba.carga;
import static ec.mdd.cargas.dominio.DiccionarioDePrueba.columna;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Los encabezados de un archivo frente a la plantilla de su carga, y su número de registros (RF-07,
 * escenario «Columna faltante» de F01-H02), sin dobles.
 */
class EncabezadosTest {

    private static final Carga CARRERAS =
            carga(2, List.of(), columna("codigo_carrera"), columna("nombre"), columna("siglas"));

    @Test
    void dadosLosEncabezadosDeLaPlantilla_cuandoSeComparan_entoncesNoHayDiferencias() {
        assertThat(CARRERAS.compararEncabezados(List.of("codigo_carrera", "nombre", "siglas")))
                .isEmpty();
        assertThat(CARRERAS.compararEncabezados(List.of(" codigo_carrera", "nombre ", " siglas ")))
                .isEmpty();
    }

    @Test
    void dadoUnArchivoSinUnaColumna_cuandoSeComparan_entoncesFaltaEsaColumna() {
        assertThat(CARRERAS.compararEncabezados(List.of("nombre", "siglas")))
                .containsExactly(
                        new DiferenciaDeEncabezado(
                                "codigo_carrera", MotivoDeDiferencia.FALTA, null));
    }

    @Test
    void dadoUnArchivoConUnaColumnaDeMasYOtraRepetida_cuandoSeComparan_entoncesLasNombra() {
        assertThat(
                        CARRERAS.compararEncabezados(
                                List.of("codigo_carrera", "nombre", "nombre", "siglas", "extra")))
                .containsExactly(
                        new DiferenciaDeEncabezado("nombre", MotivoDeDiferencia.REPETIDA, null),
                        new DiferenciaDeEncabezado("extra", MotivoDeDiferencia.SOBRA, null));
    }

    @Test
    void dadasLasMismasColumnasEnOtroOrden_cuandoSeComparan_entoncesLaPrimeraFueraDeLugar() {
        assertThat(CARRERAS.compararEncabezados(List.of("codigo_carrera", "siglas", "nombre")))
                .containsExactly(
                        new DiferenciaDeEncabezado("siglas", MotivoDeDiferencia.FUERA_DE_LUGAR, 3));
    }

    @Test
    void dadosEncabezadosDistintos_cuandoSeLee_entoncesEncabezadosDistintos() {
        TablaDeDatos tabla =
                new TablaDeDatos(List.of("nombre", "siglas"), List.of(List.of("Uno", "U")));

        assertThatThrownBy(() -> CARRERAS.leer(tabla))
                .isInstanceOfSatisfying(
                        EncabezadosDistintos.class,
                        error ->
                                assertThat(error.diferencias())
                                        .extracting(DiferenciaDeEncabezado::columna)
                                        .containsExactly("codigo_carrera"));
    }

    @Test
    void dadoUnArchivoSinRegistros_cuandoSeLee_entoncesArchivoSinRegistros() {
        TablaDeDatos tabla =
                new TablaDeDatos(List.of("codigo_carrera", "nombre", "siglas"), List.of());

        assertThatThrownBy(() -> CARRERAS.leer(tabla)).isInstanceOf(ArchivoSinRegistros.class);
    }

    @Test
    void dadoUnArchivoVacio_cuandoSeLee_entoncesArchivoSinRegistrosYNoEncabezadosDistintos() {
        assertThatThrownBy(() -> CARRERAS.leer(new TablaDeDatos(List.of(), List.of())))
                .isInstanceOf(ArchivoSinRegistros.class);
        assertThatThrownBy(() -> CARRERAS.leer(new TablaDeDatos(List.of(""), List.of())))
                .isInstanceOf(ArchivoSinRegistros.class);
    }

    @Test
    void dadoUnArchivoValido_cuandoSeLee_entoncesDevuelveSusRegistros() {
        TablaDeDatos tabla =
                new TablaDeDatos(
                        List.of("codigo_carrera", "nombre", "siglas"),
                        List.of(List.of("C1", "Uno", "U"), List.of("C2", "Dos", "D")));

        assertThat(CARRERAS.leer(tabla)).isEqualTo(2);
    }
}
