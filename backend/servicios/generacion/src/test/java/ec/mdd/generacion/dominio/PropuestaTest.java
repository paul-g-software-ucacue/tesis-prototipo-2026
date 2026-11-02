package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Una excepción es una inconsistencia blanda justificada (MD-07). */
class PropuestaTest {

    @Test
    void conUnaExcepcion_cuandoSePregunta_entoncesTieneRn19Justificada() {
        Propuesta propuesta =
                con(new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó esta materia"));

        assertThat(propuesta.inconsistenciaJustificada()).contains(ReglaDeAsignacion.RN_19);
    }

    @Test
    void conAfinidad_cuandoSePregunta_entoncesNoTiene() {
        assertThat(
                        con(new Justificacion(TipoDeJustificacion.AFINIDAD, null))
                                .inconsistenciaJustificada())
                .isEmpty();
    }

    @Test
    void conDisponibilidad_cuandoSePregunta_entoncesNoTiene() {
        assertThat(
                        con(new Justificacion(TipoDeJustificacion.DISPONIBILIDAD, null))
                                .inconsistenciaJustificada())
                .isEmpty();
    }

    private static Propuesta con(Justificacion justificacion) {
        return new Propuesta(
                "POO-204",
                "A",
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                justificacion,
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
