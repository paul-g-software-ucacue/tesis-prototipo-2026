package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * El plazo de una etapa (F12-H03, D-89, D-114): la fecha de la llegada en Ecuador más los días
 * corridos; vence al terminar ese día.
 */
class PlazoTest {

    // 2026-11-10 a las 10:00 de Ecuador.
    private static final Instant LLEGADA = Instant.parse("2026-11-10T15:00:00Z");

    @Test
    void dadaUnaLlegada_cuandoSeCalculaElPlazo_entoncesLaFechaLimiteEsLaLlegadaMasLosDias() {
        Plazo plazo = Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, LLEGADA, 5);

        assertThat(plazo.etapa()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(plazo.llegada()).isEqualTo(LLEGADA);
        assertThat(plazo.dias()).isEqualTo(5);
        assertThat(plazo.fechaLimite()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void dadaUnaLlegadaALas20DeEcuador_cuandoSeCalcula_entoncesCuentaElDiaDeEcuadorNoElDeUtc() {
        // 2026-11-10 a las 20:00 de Ecuador es 2026-11-11 a la 01:00 UTC.
        Instant noche = Instant.parse("2026-11-11T01:00:00Z");

        assertThat(Plazo.de(TipoDeEtapa.EN_OCS, noche, 1).fechaLimite())
                .isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void dadoUnPlazo_cuandoSeMiraCadaDia_entoncesEstaATiempoVenceHoyOVencio() {
        Plazo plazo = Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, LLEGADA, 5);

        assertThat(plazo.en(LocalDate.of(2026, 11, 10)))
                .isEqualTo(new Plazo.Situacion(EstadoDelPlazo.A_TIEMPO, 5));
        assertThat(plazo.en(LocalDate.of(2026, 11, 14)))
                .isEqualTo(new Plazo.Situacion(EstadoDelPlazo.A_TIEMPO, 1));
        assertThat(plazo.en(LocalDate.of(2026, 11, 15)))
                .isEqualTo(new Plazo.Situacion(EstadoDelPlazo.VENCE_HOY, 0));
        assertThat(plazo.en(LocalDate.of(2026, 11, 16)))
                .isEqualTo(new Plazo.Situacion(EstadoDelPlazo.VENCIDO, -1));
        assertThat(plazo.en(LocalDate.of(2026, 11, 25)))
                .isEqualTo(new Plazo.Situacion(EstadoDelPlazo.VENCIDO, -10));
    }

    @Test
    void dadoUnPlazo_cuandoSeCuentaElAtraso_entoncesSoloCuentaDespuesDeLaFechaLimite() {
        Plazo plazo = Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, LLEGADA, 5);

        assertThat(plazo.diasDeAtraso(LocalDate.of(2026, 11, 14))).isZero();
        assertThat(plazo.diasDeAtraso(LocalDate.of(2026, 11, 15))).isZero();
        assertThat(plazo.diasDeAtraso(LocalDate.of(2026, 11, 16))).isEqualTo(1);
        assertThat(plazo.diasDeAtraso(LocalDate.of(2026, 11, 22))).isEqualTo(7);
    }

    @Test
    void dadoUnPlazoDeCeroDiasOPublicado_cuandoSeArma_entoncesSeRechaza() {
        assertThatThrownBy(() -> Plazo.de(TipoDeEtapa.EN_OCS, LLEGADA, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Plazo.de(TipoDeEtapa.PUBLICADO, LLEGADA, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadasLasEtapasDeUnaConfiguracion_cuandoSeTomanLosPlazos_entoncesSoloCuentanLasActivas() {
        PlazosDeEtapas plazos = PlazosDeEtapas.de(etapasPorDefecto());

        assertThat(plazos.de(TipoDeEtapa.BORRADOR_CARRERA)).isEqualTo(OptionalInt.of(15));
        assertThat(plazos.de(TipoDeEtapa.EN_CONSOLIDACION)).isEqualTo(OptionalInt.of(5));
        assertThat(plazos.de(TipoDeEtapa.BORRADOR_UNIDADES)).isEmpty();
        assertThat(plazos.de(TipoDeEtapa.REMITIDO_ZONAL)).isEmpty();
        assertThat(plazos.de(TipoDeEtapa.PUBLICADO)).isEmpty();
    }

    @Test
    void dadasLasEtapasPorDefecto_cuandoSeSumanHastaAprobado_entoncesDan43Dias() {
        assertThat(PlazosDeEtapas.de(etapasPorDefecto()).sumaHastaAprobado()).isEqualTo(43);
    }

    @Test
    void dadaUnaEtapaOpcionalDesactivada_cuandoSeSuma_entoncesNoCuenta() {
        List<Etapa> etapas = new ArrayList<>(etapasPorDefecto());
        etapas.set(
                TipoDeEtapa.EN_REVISION_VRA.ordinal(),
                new Etapa(
                        TipoDeEtapa.EN_REVISION_VRA,
                        false,
                        "Revisión del Vicerrectorado",
                        "VICERRECTOR",
                        OptionalInt.of(5),
                        true));

        assertThat(PlazosDeEtapas.de(etapas).sumaHastaAprobado()).isEqualTo(38);
    }

    /** La configuración por defecto de 9.1, con sus plazos (F12-H01). */
    static List<Etapa> etapasPorDefecto() {
        return List.of(
                etapa(TipoDeEtapa.BORRADOR_CARRERA, true, "COORDINADOR_CARRERA", 15),
                etapa(TipoDeEtapa.BORRADOR_UNIDADES, false, "GESTOR_UNIDAD", 10),
                etapa(TipoDeEtapa.EN_CONSOLIDACION, true, "RESPONSABLE_CONSOLIDACION", 5),
                etapa(TipoDeEtapa.EN_REVISION_VRA, true, "VICERRECTOR", 5),
                etapa(TipoDeEtapa.CONGELADO_EN_FIRMA, true, "RESPONSABLE_CONSOLIDACION", 5),
                etapa(TipoDeEtapa.EN_OCS, true, "SECRETARIA_GENERAL", 10),
                etapa(TipoDeEtapa.REMITIDO_ZONAL, false, "VICERRECTOR", 10),
                etapa(TipoDeEtapa.APROBADO, true, "SECRETARIA_GENERAL", 3),
                new Etapa(
                        TipoDeEtapa.PUBLICADO,
                        true,
                        "Publicado",
                        "SECRETARIA_GENERAL",
                        OptionalInt.empty(),
                        false));
    }

    private static Etapa etapa(TipoDeEtapa tipo, boolean activa, String rol, int dias) {
        return new Etapa(tipo, activa, tipo.name(), rol, OptionalInt.of(dias), false);
    }
}
