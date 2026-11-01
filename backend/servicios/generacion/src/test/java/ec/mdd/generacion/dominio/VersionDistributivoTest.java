package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La versión del distributivo, sus parciales y su avance (MD-01, RF-24, D-19). */
class VersionDistributivoTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void dadaUnaActividad_cuandoSeBusca_entoncesElProyectoEsParteDeLaLlave() {
        ActividadNoLectiva sinProyecto = actividad("140-b", null);
        ActividadNoLectiva conProyecto = actividad("141-a", "INV-2025-01");
        VersionDistributivo version =
                new VersionDistributivo(
                        ID,
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(ParcialCarrera.nuevo("TDS")),
                        List.of(),
                        List.of(sinProyecto, conProyecto));

        assertThat(version.actividadDe("TDS", "0109990010", "140-b", null)).contains(sinProyecto);
        assertThat(version.actividadDe("TDS", "0109990010", "141-a", "INV-2025-01"))
                .contains(conProyecto);
        assertThat(version.actividadDe("TDS", "0109990010", "141-a", "INV-2025-02")).isEmpty();
        assertThat(version.actividadDe("TDS", "0109990010", "141-a", null)).isEmpty();
        assertThat(version.actividadDe("CONT", "0109990010", "140-b", null)).isEmpty();
        assertThat(version.actividadDe("TDS", "0109990020", "140-b", null)).isEmpty();
    }

    private static ActividadNoLectiva actividad(String codigo, String proyecto) {
        return new ActividadNoLectiva(
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                codigo,
                "Una actividad",
                Componente.OTRAS_DOCENCIA,
                proyecto,
                proyecto == null ? null : "Un proyecto",
                new BigDecimal("4"),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }

    @Test
    void dadasLasCarreras_cuandoNace_entoncesTieneUnParcialEnElaboracionPorCarrera() {
        VersionDistributivo version =
                VersionDistributivo.primera(ID, List.of("TDS", "CONT", "TDS"));

        assertThat(version.numero()).isEqualTo(1);
        assertThat(version.estado()).isEqualTo(EstadoDeVersion.EDITABLE);
        assertThat(version.parciales())
                .containsExactly(ParcialCarrera.nuevo("CONT"), ParcialCarrera.nuevo("TDS"));
        assertThat(version.propuestas()).isEmpty();
    }

    @Test
    void dadaUnaPropuesta_cuandoSeBusca_entoncesEsLaDeEsaAsignaturaParalelo() {
        VersionDistributivo version = con(List.of(propuesta("POO", "A")));

        assertThat(version.propuestaDe("POO", "A")).isPresent();
        assertThat(version.propuestaDe("POO", "B")).isEmpty();
        assertThat(version.propuestaDe("RED", "A")).isEmpty();
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeExigeEditable_entoncesPasa() {
        assertThatCode(() -> con(List.of()).exigirEditable("NUEVA")).doesNotThrowAnyException();
    }

    @Test
    void dadoUnParcialDevuelto_cuandoSeExigeEditable_entoncesPasa() {
        VersionDistributivo version =
                new VersionDistributivo(
                        ID,
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(new ParcialCarrera("TDS", EstadoDelParcial.DEVUELTO)),
                        List.of());

        assertThatCode(() -> version.exigirEditable("TDS")).doesNotThrowAnyException();
    }

    @Test
    void dadoUnParcialEnviado_cuandoSeExigeEditable_entoncesRechaza() {
        VersionDistributivo version =
                new VersionDistributivo(
                        ID,
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO)),
                        List.of());

        assertThatThrownBy(() -> version.exigirEditable("TDS"))
                .isInstanceOf(ParcialNoEditable.class);
    }

    @Test
    void dadaUnaVersionCongelada_cuandoSeExigeEditable_entoncesRechaza() {
        VersionDistributivo version =
                new VersionDistributivo(
                        ID,
                        1,
                        EstadoDeVersion.CONGELADA,
                        List.of(ParcialCarrera.nuevo("TDS")),
                        List.of());

        assertThatThrownBy(() -> version.exigirEditable("TDS"))
                .isInstanceOf(ParcialNoEditable.class);
    }

    @Test
    void dadasDosOfertadasYUnaPropuesta_cuandoSeMideElAvance_entoncesFaltaUna() {
        AvanceDelParcial avance =
                con(List.of(propuesta("POO", "A")))
                        .avance("TDS", List.of(paralelo("A", false), paralelo("B", false)));

        assertThat(avance.ofertadas()).isEqualTo(2);
        assertThat(avance.conPropuesta()).isEqualTo(1);
        assertThat(avance.completo()).isFalse();
    }

    @Test
    void dadasTodasConPropuesta_cuandoSeMideElAvance_entoncesEstaCompleto() {
        assertThat(
                        con(List.of(propuesta("POO", "A")))
                                .avance("TDS", List.of(paralelo("A", false)))
                                .completo())
                .isTrue();
    }

    @Test
    void dadaUnaRetiradaConPropuesta_cuandoSeMideElAvance_entoncesNoCuenta() {
        AvanceDelParcial avance =
                con(List.of(propuesta("POO", "A"), propuesta("POO", "B")))
                        .avance("TDS", List.of(paralelo("A", true), paralelo("B", false)));

        assertThat(avance.ofertadas()).isEqualTo(1);
        assertThat(avance.conPropuesta()).isEqualTo(1);
    }

    @Test
    void dadoQueNoHayOfertadas_cuandoSeMideElAvance_entoncesNoEstaCompleto() {
        assertThat(con(List.of()).avance("TDS", List.of()).completo()).isFalse();
    }

    @Test
    void dadaOtraCarrera_cuandoSeMideElAvance_entoncesNoCuentaSusParalelos() {
        assertThat(con(List.of()).avance("CONT", List.of(paralelo("A", false))).ofertadas())
                .isZero();
    }

    private static VersionDistributivo con(List<Propuesta> propuestas) {
        return new VersionDistributivo(
                ID, 1, EstadoDeVersion.EDITABLE, List.of(ParcialCarrera.nuevo("TDS")), propuestas);
    }

    private static Propuesta propuesta(String asignatura, String paralelo) {
        return new Propuesta(
                asignatura,
                paralelo,
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                Instant.EPOCH,
                "coordinacion.demo");
    }

    private static AsignaturaParalelo paralelo(String paralelo, boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada("POO", "Programación", "06", "061"),
                paralelo,
                "TDS",
                new BigDecimal("4"),
                false,
                retirada);
    }
}
