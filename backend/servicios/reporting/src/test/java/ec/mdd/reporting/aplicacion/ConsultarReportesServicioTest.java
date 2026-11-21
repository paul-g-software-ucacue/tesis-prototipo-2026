package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes.DocentesDelPeriodo;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Consultar los reportes de la versión publicada vigente (F20-H01, RF-45). */
class ConsultarReportesServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");
    private static final String HASH_1 = "a".repeat(64);
    private static final String HASH_2 = "b".repeat(64);
    private static final Publicacion ANTIGUA =
            new Publicacion(
                    "2025-II",
                    2,
                    HASH_2,
                    Instant.parse("2025-06-01T15:00:00Z"),
                    "secretaria.demo",
                    false);
    private static final Publicacion EN_REFORMA =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH_1,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    true);
    private static final DocentesDeLaVersion.Docente ANA =
            new DocentesDeLaVersion.Docente("0109990010", "PEREZ ANA");

    private final PublicacionesDelDistributivo publicaciones =
            mock(PublicacionesDelDistributivo.class);
    private final ContenidoDeLaVersion contenido = mock(ContenidoDeLaVersion.class);
    private final ConsultarReportesServicio servicio =
            new ConsultarReportesServicio(
                    publicaciones, contenido, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadasDosPublicaciones_cuandoSePiden_entoncesVieneLaMasRecientePrimero() {
        when(publicaciones.vigentes()).thenReturn(List.of(ANTIGUA, EN_REFORMA));

        assertThat(servicio.publicaciones()).containsExactly(EN_REFORMA, ANTIGUA);
    }

    @Test
    void dadoUnPeriodoPublicado_cuandoSePidenSusDocentes_entoncesSonLosDeLaVersionPublicada() {
        when(publicaciones.vigentes()).thenReturn(List.of(EN_REFORMA));
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH_1, List.of(ANA))));

        assertThat(servicio.docentes("2026-I"))
                .isEqualTo(new DocentesDelPeriodo(EN_REFORMA, List.of(ANA)));
    }

    @Test
    void dadoUnPeriodoSinPublicacion_cuandoSePideAlgo_entoncesSinPublicacion() {
        when(publicaciones.vigentes()).thenReturn(List.of(ANTIGUA));

        assertThatThrownBy(() -> servicio.docentes("2026-I")).isInstanceOf(SinPublicacion.class);
        assertThatThrownBy(() -> servicio.reporte("2026-I", "0109990010", "rectorado.demo"))
                .isInstanceOf(SinPublicacion.class);
    }

    @Test
    void dadaUnaReforma_cuandoSePideElReporte_entoncesSaleDeLaVersionPublicadaConQuienYCuando() {
        when(publicaciones.vigentes()).thenReturn(List.of(EN_REFORMA));
        when(contenido.contenido("2026-I", 1, "0109990010"))
                .thenReturn(Optional.of(contenidoDeAna(HASH_1)));

        ReporteIndividual reporte = servicio.reporte("2026-I", "0109990010", "rectorado.demo");

        assertThat(reporte.publicacion()).isEqualTo(EN_REFORMA);
        assertThat(reporte.generadoPor()).isEqualTo("rectorado.demo");
        assertThat(reporte.generadoEn()).isEqualTo(AHORA);
    }

    @Test
    void dadoUnDocenteQueNoEsta_cuandoSePideSuReporte_entoncesDocenteSinDistributivo() {
        when(publicaciones.vigentes()).thenReturn(List.of(EN_REFORMA));
        when(contenido.contenido("2026-I", 1, "0109990099")).thenReturn(Optional.empty());
        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH_1, List.of(ANA))));

        assertThatThrownBy(() -> servicio.reporte("2026-I", "0109990099", "rectorado.demo"))
                .isInstanceOf(DocenteSinDistributivo.class);
    }

    @Test
    void dadaUnaVersionPublicadaSinContenidoOConOtroHash_cuandoSePide_entoncesInconsistente() {
        when(publicaciones.vigentes()).thenReturn(List.of(EN_REFORMA));
        when(contenido.contenido("2026-I", 1, "0109990010")).thenReturn(Optional.empty());
        when(contenido.docentes("2026-I", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reporte("2026-I", "0109990010", "rectorado.demo"))
                .isInstanceOf(ContenidoInconsistente.class);
        assertThatThrownBy(() -> servicio.docentes("2026-I"))
                .isInstanceOf(ContenidoInconsistente.class);

        when(contenido.docentes("2026-I", 1))
                .thenReturn(Optional.of(new DocentesDeLaVersion(HASH_2, List.of(ANA))));
        assertThatThrownBy(() -> servicio.docentes("2026-I"))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    private static ContenidoDelDocente contenidoDeAna(String hash) {
        return new ContenidoDelDocente(
                hash,
                new DatosDelDocente("0109990010", "PEREZ ANA", null, null),
                List.of(),
                List.of(),
                HorasPorComponente.NINGUNA);
    }
}
