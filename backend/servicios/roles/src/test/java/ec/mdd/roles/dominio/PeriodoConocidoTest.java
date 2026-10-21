package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** La copia de un periodo que avisa Académico nunca retrocede (research §1), sin dobles. */
class PeriodoConocidoTest {

    @Test
    void dadoUnEventoMasNuevo_cuandoSeAplica_entoncesLaCopiaAvanza() {
        PeriodoConocido periodo = new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION);

        assertThat(periodo.conEstado(EstadoConocido.EN_APROBACION))
                .isEqualTo(new PeriodoConocido("2026-2", EstadoConocido.EN_APROBACION));
    }

    @Test
    void dadoUnEventoViejoQueLlegaTarde_cuandoSeAplica_entoncesLaCopiaNoRetrocede() {
        PeriodoConocido cerrado = new PeriodoConocido("2026-2", EstadoConocido.CERRADO);

        assertThat(cerrado.conEstado(EstadoConocido.PLANIFICACION)).isEqualTo(cerrado);
    }
}
