package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El cálculo del contenido congelado de una versión (F20-H01, D-115). */
class ContenidoCongeladoTest {

    private static final String ANA = "0109990010";
    private static final String LUIS = "0109990020";

    private static final AsignaturaParalelo BD_A = paralelo("BD-301", "A", "TDS", "8", false);
    private static final AsignaturaParalelo SOC_B = paralelo("SOC-101", "B", "AGRO", "3.25", false);
    private static final AsignaturaParalelo RETIRADO = paralelo("MAT-101", "C", "TDS", "6", true);

    private static final VersionDistributivo VERSION =
            new VersionDistributivo(
                    UUID.randomUUID(),
                    1,
                    EstadoDeVersion.CONGELADA,
                    List.of(ParcialCarrera.nuevo("AGRO"), ParcialCarrera.nuevo("TDS")),
                    List.of(
                            propuesta(BD_A, ANA, "PEREZ ANA"),
                            propuesta(SOC_B, ANA, "PEREZ ANA"),
                            propuesta(RETIRADO, ANA, "PEREZ ANA")),
                    List.of(
                            actividad("TDS", ANA, "PEREZ ANA", Componente.INVESTIGACION, "3"),
                            actividad("AGRO", LUIS, "ANDRADE LUIS", Componente.GESTION, "4")));

    @Test
    void dadaUnaVersion_cuandoSeCalcula_entoncesCadaDocenteTieneSusAsignacionesYSuCarga() {
        ContenidoCongelado contenido =
                ContenidoCongelado.de(
                        "2026-I",
                        VERSION,
                        List.of(BD_A, SOC_B, RETIRADO),
                        Map.of("TDS", "Software", "AGRO", "Agronomía"),
                        Map.of(ANA, activo(ANA)));

        assertThat(contenido.numero()).isEqualTo(1);
        assertThat(contenido.hash())
                .isEqualTo(ContenidoDelParcial.hashDeLaVersion("2026-I", VERSION));
        assertThat(contenido.cargas())
                .extracting(CargaCongelada::nombre)
                .containsExactly("ANDRADE LUIS", "PEREZ ANA");
        CargaCongelada ana = contenido.cargas().get(1);
        assertThat(ana.asignaturas())
                .extracting(AsignaturaCongelada::codigo, AsignaturaCongelada::nombreCarrera)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SOC-101", "Agronomía"),
                        org.assertj.core.groups.Tuple.tuple("BD-301", "Software"));
        assertThat(ana.horas().clase()).isEqualByComparingTo("11.25");
        assertThat(ana.horas().investigacion()).isEqualByComparingTo("3");
        assertThat(ana.dedicacion()).contains(Dedicacion.TC);
        assertThat(ana.horasContratadas()).hasValue(40);
    }

    @Test
    void dadoUnDocenteQueYaNoEstaActivo_cuandoSeCalcula_entoncesNoTieneDedicacion() {
        ContenidoCongelado contenido =
                ContenidoCongelado.de(
                        "2026-I",
                        VERSION,
                        List.of(BD_A, SOC_B),
                        Map.of(),
                        Map.of(ANA, activo(ANA)));

        CargaCongelada luis = contenido.cargas().get(0);
        assertThat(luis.dedicacion()).isEmpty();
        assertThat(luis.actividades())
                .extracting(ActividadCongelada::componente)
                .containsExactly(Componente.GESTION);
        assertThat(luis.horas().gestion()).isEqualByComparingTo("4");
        assertThat(luis.asignaturas()).isEmpty();
        assertThat(contenido.cargas().get(1).asignaturas().get(0).nombreCarrera())
                .isEqualTo("AGRO");
    }

    private static DocenteActivo activo(String docente) {
        return new DocenteActivo(
                docente,
                "PEREZ ANA",
                Dedicacion.TC,
                40,
                new FormacionDelDocente(
                        new TituloDeTercerNivel("Título", "06", "061"),
                        List.of(),
                        List.of(),
                        List.of()));
    }

    private static AsignaturaParalelo paralelo(
            String asignatura, String paralelo, String carrera, String horas, boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada(asignatura, "Asignatura " + asignatura, "06", "061"),
                paralelo,
                carrera,
                new BigDecimal(horas),
                false,
                retirada);
    }

    private static Propuesta propuesta(AsignaturaParalelo paralelo, String docente, String nombre) {
        return new Propuesta(
                paralelo.asignatura().codigo(),
                paralelo.paralelo(),
                paralelo.carrera(),
                docente,
                nombre,
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.parse("2026-10-31T15:00:00Z"),
                "coordinacion.demo");
    }

    private static ActividadNoLectiva actividad(
            String carrera, String docente, String nombre, Componente componente, String horas) {
        return new ActividadNoLectiva(
                carrera,
                docente,
                nombre,
                "14x-" + componente.ordinal(),
                "Actividad " + componente,
                componente,
                null,
                null,
                new BigDecimal(horas),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
