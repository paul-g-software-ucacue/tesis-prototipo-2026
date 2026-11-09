package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Qué etapas admiten cambios en el consolidado (#215, D-99). */
class TipoDeEtapaTest {

    @Test
    void dadasLasEtapas_cuandoSePreguntaSiAdmitenCambios_entoncesSoloHastaLaConsolidacion() {
        assertThat(
                        Arrays.stream(TipoDeEtapa.values())
                                .filter(TipoDeEtapa::admiteCambiosEnElConsolidado))
                .containsExactly(
                        TipoDeEtapa.BORRADOR_CARRERA,
                        TipoDeEtapa.BORRADOR_UNIDADES,
                        TipoDeEtapa.EN_CONSOLIDACION);
    }
}
