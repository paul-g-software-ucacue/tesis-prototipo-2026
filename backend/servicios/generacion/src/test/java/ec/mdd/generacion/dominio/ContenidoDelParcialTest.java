package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El hash del contenido aprobado (F12-H02, research §4, SC-006). */
class ContenidoDelParcialTest {

    private static final UUID VERSION = UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f");
    private static final Instant ANTES = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant DESPUES = Instant.parse("2026-11-07T15:00:00Z");

    // El JSON canónico, escrito a mano: claves ordenadas, sin espacios; horas con dos decimales.
    private static final String CANONICO =
            "{\"actividades\":[{\"actividad\":\"140-b\",\"docente\":\"0102030405\",\"horas\":\"4.00\","
                    + "\"proyecto\":null}],\"carrera\":\"DS\",\"periodo\":\"2026-I\",\"propuestas\":["
                    + "{\"asignatura\":\"MAT-1\",\"docente\":\"0102030405\",\"justificacion\":"
                    + "{\"texto\":null,\"tipo\":\"AFINIDAD\"},\"paralelo\":\"A\"},"
                    + "{\"asignatura\":\"MAT-1\",\"docente\":\"0102030406\",\"justificacion\":"
                    + "{\"texto\":\"Dict\\u00f3 \\\"esta\\\" materia\",\"tipo\":\"EXCEPCION\"},"
                    + "\"paralelo\":\"B\"}],\"version\":3}";

    @Test
    void dadoUnParcial_cuandoSeArmaSuContenido_entoncesEsElJsonCanonicoYSuSha256() {
        ContenidoDelParcial contenido = ContenidoDelParcial.de("2026-I", "DS", version());

        assertThat(contenido.canonico()).isEqualTo(CANONICO);
        assertThat(contenido.hash()).isEqualTo(sha256(CANONICO)).matches("^[0-9a-f]{64}$");
    }

    @Test
    void dadoOtroOrdenDeLasFilas_cuandoSeCalcula_entoncesElHashNoCambia() {
        VersionDistributivo version = version();
        VersionDistributivo alReves =
                new VersionDistributivo(
                        VERSION,
                        3,
                        EstadoDeVersion.EDITABLE,
                        version.parciales().reversed(),
                        version.propuestas().reversed(),
                        version.actividades().reversed());

        assertThat(ContenidoDelParcial.de("2026-I", "DS", alReves).hash())
                .isEqualTo(ContenidoDelParcial.de("2026-I", "DS", version).hash());
    }

    @Test
    void dadoQuienYCuandoDistintos_cuandoSeCalcula_entoncesElHashNoCambia() {
        VersionDistributivo version = version();
        VersionDistributivo otraVez =
                new VersionDistributivo(
                        VERSION,
                        3,
                        EstadoDeVersion.EDITABLE,
                        version.parciales(),
                        version.propuestas().stream()
                                .map(
                                        una ->
                                                new Propuesta(
                                                        una.asignatura(),
                                                        una.paralelo(),
                                                        una.carrera(),
                                                        una.docente(),
                                                        "OTRO NOMBRE",
                                                        una.justificacion(),
                                                        DESPUES,
                                                        "otra.coordinacion"))
                                .toList(),
                        version.actividades());

        assertThat(ContenidoDelParcial.de("2026-I", "DS", otraVez).hash())
                .isEqualTo(ContenidoDelParcial.de("2026-I", "DS", version).hash());
    }

    @Test
    void dadoUnSoloDatoDistinto_cuandoSeCalcula_entoncesElHashCambia() {
        String original = ContenidoDelParcial.de("2026-I", "DS", version()).hash();
        VersionDistributivo otrasHoras = version().conActividad(actividad("DS", "4.5"));
        VersionDistributivo otroTexto =
                version()
                        .conPropuesta(
                                propuesta(
                                        "B",
                                        "0102030406",
                                        new Justificacion(
                                                TipoDeJustificacion.EXCEPCION,
                                                "Dictó \"esta\" materia.")));

        assertThat(ContenidoDelParcial.de("2026-I", "DS", otrasHoras).hash())
                .isNotEqualTo(original);
        assertThat(ContenidoDelParcial.de("2026-I", "DS", otroTexto).hash()).isNotEqualTo(original);
        assertThat(ContenidoDelParcial.de("2026-II", "DS", version()).hash())
                .isNotEqualTo(original);
    }

    @Test
    void dadasOtrasCarreras_cuandoSeArmaElParcial_entoncesSoloEntranLasFilasDeLaSuya() {
        VersionDistributivo conOtra =
                version()
                        .conActividad(actividad("TDS", "2"))
                        .conPropuesta(
                                new Propuesta(
                                        "TDS-1",
                                        "A",
                                        "TDS",
                                        "0102030407",
                                        "OTRO",
                                        new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                                        ANTES,
                                        "x"));

        assertThat(ContenidoDelParcial.de("2026-I", "DS", conOtra).canonico()).isEqualTo(CANONICO);
    }

    @Test
    void
            dadosLosHashesDeLosParciales_cuandoSeCalculaElDelDistributivo_entoncesDependeDeTodosYNoDelOrden() {
        String dos =
                ContenidoDelParcial.hashDelDistributivo(
                        "2026-I", 3, Map.of("DS", "a".repeat(64), "TDS", "b".repeat(64)));

        assertThat(dos)
                .isEqualTo(
                        sha256(
                                "{\"parciales\":[{\"carrera\":\"DS\",\"hash\":\""
                                        + "a".repeat(64)
                                        + "\"},{\"carrera\":\"TDS\",\"hash\":\""
                                        + "b".repeat(64)
                                        + "\"}],\"periodo\":\"2026-I\",\"version\":3}"));
        assertThat(
                        ContenidoDelParcial.hashDelDistributivo(
                                "2026-I", 3, Map.of("DS", "a".repeat(64), "TDS", "c".repeat(64))))
                .isNotEqualTo(dos);
    }

    private static VersionDistributivo version() {
        return new VersionDistributivo(
                VERSION,
                3,
                EstadoDeVersion.EDITABLE,
                List.of(ParcialCarrera.nuevo("DS")),
                List.of(
                        propuesta(
                                "B",
                                "0102030406",
                                new Justificacion(
                                        TipoDeJustificacion.EXCEPCION, "Dictó \"esta\" materia")),
                        propuesta(
                                "A",
                                "0102030405",
                                new Justificacion(TipoDeJustificacion.AFINIDAD, null))),
                List.of(actividad("DS", "4")));
    }

    private static Propuesta propuesta(String paralelo, String docente, Justificacion porque) {
        return new Propuesta("MAT-1", paralelo, "DS", docente, "NOMBRE", porque, ANTES, "coord");
    }

    private static ActividadNoLectiva actividad(String carrera, String horas) {
        return new ActividadNoLectiva(
                carrera,
                "0102030405",
                "NOMBRE",
                "140-b",
                "Planificar",
                Componente.OTRAS_DOCENCIA,
                null,
                null,
                new BigDecimal(horas),
                ANTES,
                "coord");
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
