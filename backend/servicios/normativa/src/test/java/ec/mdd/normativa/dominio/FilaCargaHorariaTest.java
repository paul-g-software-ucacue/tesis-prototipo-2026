package ec.mdd.normativa.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Una fila de la tabla de carga horaria y sus invariantes (RN-03 a RN-15), sin dobles. */
class FilaCargaHorariaTest {

    static FilaCargaHoraria fila(
            TipoIst aplicaA,
            String perfil,
            int clase,
            int minimo,
            int maximo,
            Integer otras,
            Integer gestion,
            Integer investigacion) {
        return new FilaCargaHoraria(
                aplicaA,
                perfil,
                40,
                clase,
                minimo,
                maximo,
                otras,
                gestion,
                investigacion,
                Severidad.DURA,
                "POL 6",
                null);
    }

    @Test
    void dadoLosTiposYLasSeveridades_cuandoSeListan_entoncesSonLosDeLaPolitica() {
        assertThat(TipoIst.values())
                .containsExactly(TipoIst.T1, TipoIst.T2, TipoIst.T3, TipoIst.T4, TipoIst.T5);
        assertThat(Severidad.values()).containsExactly(Severidad.DURA, Severidad.BLANDA);
    }

    @Test
    void dadoElDocenteDeTiempoCompleto_cuandoSeCreaSuFila_entoncesEsValidaYAplicaATodos() {
        FilaCargaHoraria docente = fila(null, "DOCENTE_TC", 22, 18, 24, 18, 0, null);

        assertThat(docente.aplicaATodos()).isTrue();
        assertThat(docente.horasInvestigacionVinculacionMaximo()).isNull();
    }

    @Test
    void dadoUnCuidadorSustituto_cuandoSuSumaEsMenorQueLaDedicacion_entoncesEsValida() {
        FilaCargaHoraria cuidador = fila(null, "CUIDADOR_SUSTITUTO", 18, 18, 18, 12, 0, null);

        assertThat(cuidador.horasClase()).isEqualTo(18);
    }

    @Test
    void dadoUnaFilaSinValorDeGestion_cuandoSeCrea_entoncesNoEsCero() {
        FilaCargaHoraria investigacion =
                fila(null, "DOCENTE_INVESTIGACION_VINCULACION", 16, 0, 16, null, null, 24);

        assertThat(investigacion.horasGestion()).isNull();
        assertThat(investigacion.horasOtrasDocencia()).isNull();
        assertThat(investigacion.horasInvestigacionVinculacionMaximo()).isEqualTo(24);
    }

    @Test
    void dadoHorasDeClaseFueraDeSuRango_cuandoSeCreaLaFila_entoncesSeRechaza() {
        assertThatThrownBy(() -> fila(null, "DOCENTE_TC", 25, 18, 24, 15, 0, null))
                .isInstanceOf(FilaInvalida.class)
                .hasMessageContaining("DOCENTE_TC");
        assertThatThrownBy(() -> fila(null, "DOCENTE_TC", 17, 18, 24, 18, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnaSumaMayorQueLaDedicacion_cuandoSeCreaLaFila_entoncesSeRechaza() {
        assertThatThrownBy(() -> fila(TipoIst.T1, "GESTOR", 16, 16, 16, 16, 9, null))
                .isInstanceOfSatisfying(
                        FilaInvalida.class,
                        error -> {
                            assertThat(error.aplicaA()).isEqualTo(TipoIst.T1);
                            assertThat(error.motivo())
                                    .isEqualTo("Las horas (41) superan la dedicación de 40");
                        })
                .hasMessageContaining("GESTOR");
    }

    @Test
    void dadoHorasNegativasOSinPerfil_cuandoSeCreaLaFila_entoncesSeRechaza() {
        assertThatThrownBy(() -> fila(null, "DOCENTE_TC", 22, 18, 24, -1, 0, null))
                .isInstanceOf(FilaInvalida.class)
                .hasMessageContaining("negativas");
        assertThatThrownBy(() -> fila(null, " ", 22, 18, 24, 18, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
