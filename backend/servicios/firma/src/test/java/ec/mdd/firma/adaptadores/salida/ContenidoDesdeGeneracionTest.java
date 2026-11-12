package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.generacion.puertos.entrada.ContenidoCongelado;
import ec.mdd.generacion.puertos.entrada.ContenidoDeLaVersion;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El contenido de Generación, en los tipos de Firma (principio V, F13-H02). */
class ContenidoDesdeGeneracionTest {

    private final ContenidoCongelado generacion = mock(ContenidoCongelado.class);
    private final ContenidoDesdeGeneracion adaptador = new ContenidoDesdeGeneracion(generacion);

    @Test
    void dadoElContenidoDeGeneracion_cuandoSePide_entoncesLoPasaConLaCarreraPorSuNombre() {
        ContenidoDeLaVersion.Horas horas =
                new ContenidoDeLaVersion.Horas(
                        new BigDecimal("4"),
                        new BigDecimal("2"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        when(generacion.contenido("2026-I"))
                .thenReturn(
                        new ContenidoDeLaVersion(
                                "2026-I",
                                3,
                                "e".repeat(64),
                                List.of(
                                        new ContenidoDeLaVersion.Carrera(
                                                "TDS",
                                                "DESARROLLO DE SOFTWARE",
                                                "ENVIADO",
                                                1,
                                                1,
                                                horas)),
                                horas,
                                List.of(
                                        new ContenidoDeLaVersion.Docente(
                                                "0102030405",
                                                "ZAMBRANO LUIS",
                                                List.of(
                                                        new ContenidoDeLaVersion.Asignatura(
                                                                "POO-204",
                                                                "Programación",
                                                                "A",
                                                                "TDS",
                                                                new BigDecimal("4"))),
                                                List.of(
                                                        new ContenidoDeLaVersion.Actividad(
                                                                "Tutorías",
                                                                "OTRAS_DOCENCIA",
                                                                new BigDecimal("2"))),
                                                horas))));

        ContenidoDelDocumento contenido = adaptador.contenido("2026-I");

        assertThat(contenido.version()).isEqualTo(3);
        assertThat(contenido.hashContenido()).isEqualTo("e".repeat(64));
        assertThat(contenido.carreras().getFirst().nombre()).isEqualTo("DESARROLLO DE SOFTWARE");
        assertThat(contenido.totales().otrasDocencia()).isEqualByComparingTo("2");
        ContenidoDelDocumento.Docente docente = contenido.docentes().getFirst();
        assertThat(docente.nombre()).isEqualTo("ZAMBRANO LUIS");
        // La carrera de cada asignatura, por su nombre.
        assertThat(docente.asignaturas().getFirst().carrera()).isEqualTo("DESARROLLO DE SOFTWARE");
        assertThat(docente.actividades().getFirst().componente()).isEqualTo("OTRAS_DOCENCIA");
        assertThat(contenido.firmantes()).isEmpty();
    }
}
