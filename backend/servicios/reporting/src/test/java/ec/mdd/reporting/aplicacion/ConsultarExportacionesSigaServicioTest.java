package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import ec.mdd.reporting.puertos.salida.ExportacionesSiga;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Las exportaciones a SIGA de un periodo, de la más reciente a la más antigua (F19-H02, FR-018).
 */
class ConsultarExportacionesSigaServicioTest {

    private final ExportacionesSiga exportaciones = mock(ExportacionesSiga.class);

    private static ExportacionSiga exportacion(Instant momento) {
        return new ExportacionSiga(
                UUID.randomUUID(),
                "secretaria.demo",
                momento,
                "2026-I",
                1,
                "a".repeat(64),
                0,
                "siga-2026-I-v1.csv",
                3,
                new SumaDeVerificacion("b".repeat(64)));
    }

    @Test
    void dadasLasExportacionesDelPeriodo_cuandoSeConsultan_entoncesLasDaEnSuOrden() {
        ExportacionSiga reciente = exportacion(Instant.parse("2026-11-28T16:00:00Z"));
        ExportacionSiga anterior = exportacion(Instant.parse("2026-11-28T15:00:00Z"));
        when(exportaciones.delPeriodo("2026-I")).thenReturn(List.of(reciente, anterior));

        assertThat(new ConsultarExportacionesSigaServicio(exportaciones).delPeriodo("2026-I"))
                .containsExactly(reciente, anterior);
    }

    @Test
    void dadoUnPeriodoSinExportaciones_cuandoSeConsulta_entoncesVacia() {
        when(exportaciones.delPeriodo("2027-I")).thenReturn(List.of());

        assertThat(new ConsultarExportacionesSigaServicio(exportaciones).delPeriodo("2027-I"))
                .isEmpty();
    }
}
