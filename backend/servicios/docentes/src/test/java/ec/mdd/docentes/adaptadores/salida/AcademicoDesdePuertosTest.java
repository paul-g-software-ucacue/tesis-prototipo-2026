package ec.mdd.docentes.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.docentes.dominio.Referencias;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Las referencias de Académico, pedidas por su interfaz publicada y traducidas a las de Docentes
 * (research §4). El doble de {@link ConsultarReferencias} está escrito a mano, sin Mockito, como en
 * {@code NormativaDesdePuertosTest} de Académico (principio VIII).
 */
class AcademicoDesdePuertosTest {

    private final ConsultarReferencias consultar =
            () ->
                    new ReferenciasDeAcademico(
                            List.of(new CarreraDeReferencia("C1", "DESARROLLO DE SOFTWARE", "DS")),
                            List.of(
                                    new AsignaturaDeReferencia("A1", "PROGRAMACIÓN", "C1"),
                                    new AsignaturaDeReferencia("A2", "BASES DE DATOS", "C1")),
                            List.of(
                                    new CampoAmplioDeReferencia(
                                            "06",
                                            "Tecnologías de la información y la comunicación")));

    @Test
    void dadasLasReferenciasDeAcademico_cuandoSePiden_entoncesTraenCadaNombrePorCodigo() {
        Referencias referencias = new AcademicoDesdePuertos(consultar).referencias();

        assertThat(referencias.carrera("C1")).contains("DESARROLLO DE SOFTWARE");
        assertThat(referencias.asignaturas()).containsOnlyKeys("A1", "A2");
        assertThat(referencias.asignatura("A2")).contains("BASES DE DATOS");
        assertThat(referencias.campoAmplio("06"))
                .contains("Tecnologías de la información y la comunicación");
        assertThat(referencias.campoAmplio("05")).isEmpty();
    }
}
