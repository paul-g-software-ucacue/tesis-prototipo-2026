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
import ec.mdd.cargas.puertos.salida.PeriodoAcademico;
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
                                            false),
                                    new PeriodoDeReferencia(
                                            "2026-II",
                                            "Segundo periodo",
                                            LocalDate.of(2026, 10, 5),
                                            LocalDate.of(2027, 2, 5),
                                            false,
                                            false),
                                    new PeriodoDeReferencia(
                                            "2025-II",
                                            "Periodo cerrado",
                                            LocalDate.of(2025, 10, 6),
                                            LocalDate.of(2026, 2, 6),
                                            true,
                                            true)));
    private final ConsultarIdentificaciones docentes = () -> Set.of("0109990010");

    @Test
    void traduceLosCodigosDeLosDosPuertos_activosOno() {
        ReferenciasExistentes existentes =
                new ReferenciasDesdePuertos(academico, docentes).existentes();

        assertThat(existentes.unidades()).containsExactlyInAnyOrder("MATRIZ", "VIEJA");
        assertThat(existentes.carreras()).containsExactly("C1");
        assertThat(existentes.periodos()).containsExactlyInAnyOrder("2026-I", "2026-II", "2025-II");
        assertThat(existentes.asignaturas()).containsExactly("A1");
        assertThat(existentes.existe(TipoDeReferencia.DOCENTE, "0109990010")).isTrue();
        assertThat(existentes.existe(TipoDeReferencia.CARRERA, "C2")).isFalse();
    }

    @Test
    void losPeriodos_delQueEmpiezaMasTardeAlMasAntiguo_conSiEstanAbiertosOCerrados() {
        assertThat(new ReferenciasDesdePuertos(academico, docentes).periodos())
                .containsExactly(
                        new PeriodoAcademico(
                                "2026-II",
                                "Segundo periodo",
                                LocalDate.of(2026, 10, 5),
                                false,
                                false),
                        new PeriodoAcademico(
                                "2026-I", "Primer periodo", LocalDate.of(2026, 4, 1), true, false),
                        new PeriodoAcademico(
                                "2025-II",
                                "Periodo cerrado",
                                LocalDate.of(2025, 10, 6),
                                true,
                                true));
    }
}
