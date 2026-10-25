package ec.mdd.cargas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La tabla que entrega una fuente de datos y su número de registros (RF-07, RF-57), sin dobles. */
class TablaDeDatosTest {

    private static final List<String> ENCABEZADOS = List.of("codigo", "nombre");

    @Test
    void dadasFilasVaciasAlFinal_cuandoSeCuentan_entoncesNoSonRegistros() {
        TablaDeDatos tabla =
                new TablaDeDatos(
                        ENCABEZADOS,
                        List.of(
                                List.of("A1", "Uno"),
                                List.of("A2", "Dos"),
                                List.of("", ""),
                                List.of(" ", "")));

        assertThat(tabla.registros()).isEqualTo(2);
    }

    @Test
    void dadaUnaFilaVaciaEntreRegistros_cuandoSeCuentan_entoncesNoEsUnRegistro() {
        TablaDeDatos tabla =
                new TablaDeDatos(
                        ENCABEZADOS,
                        List.of(List.of("A1", "Uno"), List.of("", ""), List.of("A3", "Tres")));

        assertThat(tabla.registros()).isEqualTo(2);
    }

    @Test
    void dadaSoloLaFilaDeEncabezados_cuandoSeCuentan_entoncesCero() {
        assertThat(new TablaDeDatos(ENCABEZADOS, List.of()).registros()).isZero();
        assertThat(new TablaDeDatos(ENCABEZADOS, List.of(List.of("", ""))).registros()).isZero();
    }

    @Test
    void dadasListasMutables_cuandoSeCreaLaTabla_entoncesLasCopia() {
        List<List<String>> filas = new ArrayList<>(List.of(List.of("A1", "Uno")));
        TablaDeDatos tabla = new TablaDeDatos(ENCABEZADOS, filas);
        filas.add(List.of("A2", "Dos"));

        assertThat(tabla.filas()).hasSize(1);
    }
}
