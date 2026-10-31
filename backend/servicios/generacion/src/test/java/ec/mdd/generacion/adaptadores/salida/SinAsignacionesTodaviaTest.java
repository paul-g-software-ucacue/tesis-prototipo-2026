package ec.mdd.generacion.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Sin asignaciones, nadie tiene horas (FR-009). */
class SinAsignacionesTodaviaTest {

    @Test
    void dadoUnPeriodo_cuandoSePidenLasHoras_entoncesNadieTieneAsignaciones() {
        assertThat(new SinAsignacionesTodavia().delPeriodo("2026-I", "TDS")).isEmpty();
    }
}
