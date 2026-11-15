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

    // F15-H01: la resolución va solo en el paso de EN_OCS a APROBADO, y siempre en él.
    private static final ResolucionOcs RESOLUCION =
            new ResolucionOcs(
                    "RPC-SO-12-No.123-2026",
                    java.time.LocalDate.of(2026, 11, 15),
                    "b".repeat(64),
                    "resolucion.pdf",
                    2048);

    private static Transicion paso(
            TipoDeEtapa desde, TipoDeEtapa hacia, Optional<ResolucionOcs> resolucion) {
        return new Transicion(
                TipoDeTransicion.PASO_DE_ETAPA,
                desde,
                hacia,
                Optional.empty(),
                1,
                "a".repeat(64),
                "secretaria.demo",
                MOMENTO,
                Map.of(),
                Optional.empty(),
                resolucion);
    }

    @Test
    void dadoElPasoAAprobadoConSuResolucion_cuandoSeCrea_entoncesLaLleva() {
        assertThat(
                        paso(TipoDeEtapa.EN_OCS, TipoDeEtapa.APROBADO, Optional.of(RESOLUCION))
                                .resolucion())
                .contains(RESOLUCION);
    }

    @Test
    void dadoElPasoAAprobadoSinResolucionUOtroPasoConElla_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> paso(TipoDeEtapa.EN_OCS, TipoDeEtapa.APROBADO, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                paso(
                                        TipoDeEtapa.EN_REVISION_VRA,
                                        TipoDeEtapa.CONGELADO_EN_FIRMA,
                                        Optional.of(RESOLUCION)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadosLosConstructoresDeAntes_cuandoSeCrean_entoncesNoLlevanResolucion() {
        assertThat(de(TipoDeTransicion.RECHAZO_EN_FIRMA, Optional.of(RECHAZO)).resolucion())
                .isEmpty();
    }
}
