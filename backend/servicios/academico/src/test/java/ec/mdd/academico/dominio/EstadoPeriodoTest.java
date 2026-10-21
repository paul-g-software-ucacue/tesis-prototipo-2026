package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El orden de los estados del periodo (RF-13, research §1), sin dobles. */
class EstadoPeriodoTest {

    @Test
    void dadoCadaEstado_cuandoSePideElSiguiente_entoncesEsElDeLaLista() {
        assertThat(EstadoPeriodo.PLANIFICACION.siguiente()).contains(EstadoPeriodo.EN_APROBACION);
        assertThat(EstadoPeriodo.EN_APROBACION.siguiente()).contains(EstadoPeriodo.VIGENTE);
        assertThat(EstadoPeriodo.VIGENTE.siguiente()).contains(EstadoPeriodo.CERRADO);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSePideElSiguiente_entoncesNoHay() {
        assertThat(EstadoPeriodo.CERRADO.siguiente()).isEmpty();
    }
}
