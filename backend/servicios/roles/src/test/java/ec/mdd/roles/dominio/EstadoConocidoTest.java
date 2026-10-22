package ec.mdd.roles.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El estado de un periodo en la copia de Roles: solo avanza (research §1), sin dobles. */
class EstadoConocidoTest {

    @Test
    void dadoCadaParDeEstados_cuandoSeCombinan_entoncesGanaElMasAvanzado() {
        EstadoConocido[] estados = EstadoConocido.values();
        for (EstadoConocido uno : estados) {
            for (EstadoConocido otro : estados) {
                EstadoConocido esperado = uno.ordinal() >= otro.ordinal() ? uno : otro;
                assertThat(uno.masAvanzado(otro)).isEqualTo(esperado);
            }
        }
    }

    @Test
    void dadoCadaEstado_cuandoSePreguntaSiEstaCerrado_entoncesSoloCerrado() {
        assertThat(EstadoConocido.CERRADO.cerrado()).isTrue();
        assertThat(EstadoConocido.PLANIFICACION.cerrado()).isFalse();
        assertThat(EstadoConocido.EN_APROBACION.cerrado()).isFalse();
        assertThat(EstadoConocido.VIGENTE.cerrado()).isFalse();
    }
}
