package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Si el distributivo está en revisión, según Aprobación (#215, D-99), o congelado (F13-H01). */
class EnRevisionServicioTest {

    private static final String PERIODO = "2026-I";
    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");

    private final EtapaDelConsolidado etapa = mock(EtapaDelConsolidado.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final EnRevisionServicio servicio = new EnRevisionServicio(etapa, distributivos);

    @Test
    void dadoQueAprobacionNoAdmiteCambios_cuandoSePregunta_entoncesEstaEnRevision() {
        when(distributivos.creadoEn(PERIODO)).thenReturn(Optional.of(CREADO));
        when(etapa.admiteCambios(PERIODO, CREADO)).thenReturn(false);

        assertThat(servicio.enRevision(PERIODO)).isTrue();
    }

    @Test
    void dadoQueAprobacionAdmiteCambios_cuandoSePregunta_entoncesNoEstaEnRevision() {
        when(distributivos.creadoEn(PERIODO)).thenReturn(Optional.of(CREADO));
        when(etapa.admiteCambios(PERIODO, CREADO)).thenReturn(true);

        assertThat(servicio.enRevision(PERIODO)).isFalse();
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSePregunta_entoncesNoEstaEnRevisionNiPregunta() {
        when(distributivos.creadoEn(PERIODO)).thenReturn(Optional.empty());

        assertThat(servicio.enRevision(PERIODO)).isFalse();
        verify(etapa, never()).admiteCambios(anyString(), any());
    }

    // F13-H01: la versión congelada la lee Generación, sin preguntar a Aprobación.
    @Test
    void dadaLaVersionVigenteCongelada_cuandoSePregunta_entoncesDaSuNumero() {
        when(distributivos.versionCongelada(PERIODO)).thenReturn(OptionalInt.of(2));

        assertThat(servicio.versionCongelada(PERIODO)).hasValue(2);
        verify(etapa, never()).admiteCambios(anyString(), any());
    }

    @Test
    void dadaLaVersionEditableOSinDistributivo_cuandoSePregunta_entoncesNoEstaCongelada() {
        when(distributivos.versionCongelada(PERIODO)).thenReturn(OptionalInt.empty());

        assertThat(servicio.versionCongelada(PERIODO)).isEmpty();
    }
}
