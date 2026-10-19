package ec.mdd.normativa.dominio;

import static ec.mdd.normativa.dominio.FilaCargaHorariaTest.fila;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** La tabla de carga horaria: una sola fila por perfil y tipo de IST (carga 7), sin dobles. */
class TablaCargaHorariaTest {

    private static final FilaCargaHoraria DOCENTE_TC =
            fila(null, "DOCENTE_TC", 22, 18, 24, 18, 0, null);
    private static final FilaCargaHoraria GESTOR_T1 =
            fila(TipoIst.T1, "GESTOR", 16, 16, 16, 16, 8, null);
    private static final FilaCargaHoraria GESTOR_T2 =
            fila(TipoIst.T2, "GESTOR", 14, 14, 14, 16, 10, null);

    @Test
    void dadoUnaFilaDelTipoYOtraParaTodos_cuandoSeBuscaUnPerfil_entoncesDaLaQueAplica() {
        TablaCargaHoraria tabla = new TablaCargaHoraria(List.of(DOCENTE_TC, GESTOR_T1, GESTOR_T2));

        assertThat(tabla.filaPara("GESTOR", TipoIst.T2)).contains(GESTOR_T2);
        assertThat(tabla.filaPara("DOCENTE_TC", TipoIst.T2)).contains(DOCENTE_TC);
        assertThat(tabla.filaPara("GESTOR", TipoIst.T5)).isEmpty();
        assertThat(tabla.filaPara("RECTOR_VICERRECTOR", TipoIst.T2)).isEmpty();
    }

    @Test
    void dadoLasFilas_cuandoSeLeen_entoncesConservanElOrdenDeLaPolitica() {
        assertThat(new TablaCargaHoraria(List.of(GESTOR_T2, DOCENTE_TC, GESTOR_T1)).filas())
                .containsExactly(GESTOR_T2, DOCENTE_TC, GESTOR_T1);
    }

    @Test
    void dadoUnPerfilYUnTipoRepetidos_cuandoSeCreaLaTabla_entoncesSeRechaza() {
        assertThatThrownBy(() -> new TablaCargaHoraria(List.of(GESTOR_T1, GESTOR_T1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GESTOR");
    }

    @Test
    void dadoUnPerfilQueAplicaATodosYAUnTipo_cuandoSeCreaLaTabla_entoncesSeRechaza() {
        FilaCargaHoraria docenteT1 = fila(TipoIst.T1, "DOCENTE_TC", 22, 18, 24, 18, 0, null);

        assertThatThrownBy(() -> new TablaCargaHoraria(List.of(DOCENTE_TC, docenteT1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DOCENTE_TC");
    }

    @Test
    void dadoUnaTablaVacia_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new TablaCargaHoraria(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
