package ec.mdd.cargas.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.cargas.dominio.ReferenciasExistentes;
import ec.mdd.cargas.dominio.TipoDeReferencia;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Las referencias de Académico y de Docentes, pedidas por sus interfaces publicadas y traducidas a
 * códigos (research §6). Los dobles de los dos puertos están escritos a mano, sin Mockito
 * (principio VIII).
 */
class ReferenciasDesdePuertosTest {

    private final ConsultarReferencias academico =
            () ->
                    new ReferenciasDeAcademico(
                            List.of(new CarreraDeReferencia("C1", "DESARROLLO DE SOFTWARE", "DS")),
                            List.of(new AsignaturaDeReferencia("A1", "PROGRAMACIÓN", "C1")),
                            List.of(new CampoAmplioDeReferencia("06", "TIC")),
                            List.of(
                                    new UnidadDeReferencia(
                                            "MATRIZ", "Sede matriz", "SEDE_MATRIZ", null, true),
                                    new UnidadDeReferencia(
                                            "VIEJA", "Unidad inactiva", "GESTION", "G07", false)),
                            List.of(
                                    new PeriodoDeReferencia(
                                            "2026-I",
                                            "Primer periodo",
                                            LocalDate.of(2026, 4, 1),
                                            LocalDate.of(2026, 8, 31),
                                            true,
                                            false)));
    private final ConsultarIdentificaciones docentes = () -> Set.of("0109990010");

    @Test
    void traduceLosCodigosDeLosDosPuertos_activosOno() {
        ReferenciasExistentes existentes =
                new ReferenciasDesdePuertos(academico, docentes).existentes();

        assertThat(existentes.unidades()).containsExactlyInAnyOrder("MATRIZ", "VIEJA");
        assertThat(existentes.carreras()).containsExactly("C1");
        assertThat(existentes.periodos()).containsExactly("2026-I");
        assertThat(existentes.asignaturas()).containsExactly("A1");
        assertThat(existentes.existe(TipoDeReferencia.DOCENTE, "0109990010")).isTrue();
        assertThat(existentes.existe(TipoDeReferencia.CARRERA, "C2")).isFalse();
    }
}
