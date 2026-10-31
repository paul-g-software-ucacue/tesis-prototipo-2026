package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las horas disponibles de un candidato (D-75, RF-25). */
class CandidatoTest {

    @Test
    void dadasHorasAsignadas_cuandoSePreguntan_entoncesSonContratadasMenosAsignadas() {
        assertThat(candidato(20, "12").horasDisponibles()).isEqualByComparingTo("8");
    }

    @Test
    void dadasHorasConDecimales_cuandoSePreguntan_entoncesSonExactas() {
        assertThat(candidato(20, "12.25").horasDisponibles()).isEqualByComparingTo("7.75");
    }

    @Test
    void dadasMasAsignadasQueContratadas_cuandoSePreguntan_entoncesSonCeroNuncaNegativas() {
        assertThat(candidato(20, "25").horasDisponibles()).isEqualByComparingTo("0");
    }

    @Test
    void dadasLasHorasDelParalelo_cuandoSeComparan_entoncesNoSeRedondean() {
        Candidato candidato = candidato(5, "2.5");

        assertThat(candidato.tieneHorasPara(new BigDecimal("2.50"))).isTrue();
        assertThat(candidato.tieneHorasPara(new BigDecimal("2.51"))).isFalse();
    }

    @Test
    void dadasHorasNegativas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> horas("-1", "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> horas("1", "-1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadasMasHorasEnOtrasCarrerasQueAsignadas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> horas("2", "3")).isInstanceOf(IllegalArgumentException.class);
    }

    private static Candidato candidato(int contratadas, String asignadas) {
        return new Candidato(
                "ID",
                "NOMBRE",
                Dedicacion.TP,
                contratadas,
                horas(asignadas, "0"),
                new PuntajeAfinidad("POO-204", "Programación", BigDecimal.ZERO, false, List.of()));
    }

    private static HorasDelDocente horas(String asignadas, String enOtrasCarreras) {
        return new HorasDelDocente(new BigDecimal(asignadas), new BigDecimal(enOtrasCarreras));
    }
}
