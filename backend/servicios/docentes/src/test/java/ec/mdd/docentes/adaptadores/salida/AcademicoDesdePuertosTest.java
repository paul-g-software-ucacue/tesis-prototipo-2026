package ec.mdd.docentes.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.docentes.dominio.Referencias;
import java.time.LocalDate;
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
                                            "Tecnologías de la información y la comunicación")),
                            List.of(
                                    new UnidadDeReferencia(
                                            "G-INV",
                                            "Gestión de Investigación",
                                            "GESTION",
                                            "G07",
                                            true),
                                    new UnidadDeReferencia(
                                            "COM-DIST",
                                            "Comisión del distributivo",
                                            "COMISION",
                                            null,
                                            false)),
                            List.of(
                                    new PeriodoDeReferencia(
                                            "2026-I",
                                            "Periodo académico 2026-I",
                                            LocalDate.of(2026, 4, 6),
                                            LocalDate.of(2026, 8, 7))));

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

    @Test
    void dadasLasUnidadesYLosPeriodos_cuandoSePiden_entoncesTraenSuTipoSuGestionYSusFechas() {
        Referencias referencias = new AcademicoDesdePuertos(consultar).referencias();

        assertThat(referencias.unidades())
                .containsEntry(
                        "G-INV",
                        new Referencias.Unidad("Gestión de Investigación", "GESTION", "G07", true))
                .containsEntry(
                        "COM-DIST",
                        new Referencias.Unidad(
                                "Comisión del distributivo", "COMISION", null, false));
        assertThat(referencias.unidad("G-INV")).contains("Gestión de Investigación");
        assertThat(referencias.periodo("2026-I"))
                .isEqualTo(
                        new Referencias.Periodo(
                                "Periodo académico 2026-I",
                                LocalDate.of(2026, 4, 6),
                                LocalDate.of(2026, 8, 7)));
    }

    @Test
    void dentroDeUnaTransaccion_lasReferenciasSeLeenUnaVez_yFueraCadaVez() {
        java.util.concurrent.atomic.AtomicInteger lecturas =
                new java.util.concurrent.atomic.AtomicInteger();
        AcademicoDesdePuertos contando =
                new AcademicoDesdePuertos(
                        () -> {
                            lecturas.incrementAndGet();
                            return consultar.referencias();
                        });

        contando.referencias();
        contando.referencias();
        assertThat(lecturas).as("fuera de una transacción").hasValue(2);

        org.springframework.transaction.support.TransactionSynchronizationManager
                .initSynchronization();
        try {
            contando.referencias();
            contando.referencias();
            assertThat(lecturas).as("dentro, una sola vez").hasValue(3);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations()
                    .forEach(sincronizacion -> sincronizacion.afterCompletion(0));
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .clearSynchronization();
        }
        contando.referencias();
        assertThat(lecturas).as("al terminar, se vuelven a leer").hasValue(4);
    }
}
