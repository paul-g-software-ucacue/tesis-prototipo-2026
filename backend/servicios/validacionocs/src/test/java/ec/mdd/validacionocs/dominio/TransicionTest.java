package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransicionTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-15T10:00:00Z");
    private static final RechazoDelFirmante RECHAZO =
            new RechazoDelFirmante(
                    3,
                    "RECTOR",
                    "rectorado.demo",
                    MOMENTO,
                    new Observacion("Falta la carga de un docente de Software."));

    private static Transicion de(TipoDeTransicion tipo, Optional<RechazoDelFirmante> rechazo) {
        return new Transicion(
                tipo,
                TipoDeEtapa.CONGELADO_EN_FIRMA,
                TipoDeEtapa.EN_REVISION_VRA,
                Optional.empty(),
                1,
                "a".repeat(64),
                "sistema",
                MOMENTO,
                Map.of(),
                rechazo);
    }

    @Test
    void dadoUnRechazoEnFirmaConSuRechazo_cuandoSeCrea_entoncesLoLleva() {
        assertThat(de(TipoDeTransicion.RECHAZO_EN_FIRMA, Optional.of(RECHAZO)).rechazo())
                .contains(RECHAZO);
    }

    @Test
    void dadoUnRechazoEnFirmaSinRechazoUOtroTipoConEl_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> de(TipoDeTransicion.RECHAZO_EN_FIRMA, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> de(TipoDeTransicion.PASO_DE_ETAPA, Optional.of(RECHAZO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoElConstructorDeAntes_cuandoSeCrea_entoncesNoLlevaRechazo() {
        assertThat(
                        new Transicion(
                                        TipoDeTransicion.PASO_DE_ETAPA,
                                        TipoDeEtapa.EN_REVISION_VRA,
                                        TipoDeEtapa.CONGELADO_EN_FIRMA,
                                        Optional.empty(),
                                        1,
                                        "a".repeat(64),
                                        "vicerrectorado.demo",
                                        MOMENTO)
                                .rechazo())
                .isEmpty();
    }
}
