package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * La advertencia de RN-23 al cargar la configuración (F12-H03, D-114): el último plazo es el día de
 * la carga más la suma de los plazos hasta APROBADO, y se compara con el inicio del periodo menos
 * los días de la normativa. Nunca rechaza: solo advierte.
 */
class AdvertenciaDeRemisionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    private static final PlazosDeEtapas POR_DEFECTO =
            PlazosDeEtapas.de(PlazoTest.etapasPorDefecto());
    private static final OptionalInt TREINTA = OptionalInt.of(30);

    @Test
    void dadaLaPlantillaYUnInicioA60Dias_cuandoSeCarga_entoncesFaltan13Dias() {
        LocalDate inicio = HOY.plusDays(60);

        AdvertenciaDeRemision advertencia =
                AdvertenciaDeRemision.de(HOY, POR_DEFECTO, inicio, TREINTA).orElseThrow();

        assertThat(advertencia.codigo()).isEqualTo(CodigoDeAdvertencia.RN23_MARGEN_INSUFICIENTE);
        assertThat(advertencia.ultimoPlazo()).isEqualTo(HOY.plusDays(43));
        assertThat(advertencia.inicioDelPeriodo()).isEqualTo(inicio);
        assertThat(advertencia.diasAntesDelInicio()).hasValue(30);
        assertThat(advertencia.fechaDeRemision()).contains(inicio.minusDays(30));
        assertThat(advertencia.diasDeMargen()).hasValue(-13);
        assertThat(advertencia.mensaje())
                .isEqualTo(
                        "Con estos plazos, contados desde hoy, el distributivo se aprobaría el"
                                + " 02/01/2027, después del 20/12/2026, que es 30 días antes del"
                                + " inicio del periodo (RN-23). Le faltan 13 días: acorta plazos o"
                                + " desactiva etapas opcionales.");
    }

    @Test
    void dadoUnInicioA120Dias_cuandoSeCarga_entoncesNoHayAdvertencia() {
        assertThat(AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(120), TREINTA))
                .isEmpty();
    }

    @Test
    void dadoElLimiteExacto_cuandoSeCarga_entoncesNoHayAdvertencia() {
        // 43 días de plazos más los 30 de RN-23: aprobado justo el día que pide la norma.
        assertThat(AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(73), TREINTA)).isEmpty();
        assertThat(AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(72), TREINTA))
                .hasValueSatisfying(una -> assertThat(una.diasDeMargen()).hasValue(-1));
    }

    @Test
    void dadoUnInicioQueYaPaso_cuandoSeCarga_entoncesAdvierte() {
        assertThat(AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.minusDays(5), TREINTA))
                .hasValueSatisfying(
                        una ->
                                assertThat(una.codigo())
                                        .isEqualTo(CodigoDeAdvertencia.RN23_MARGEN_INSUFICIENTE));
    }

    @Test
    void dadoUnMargenDeUnDia_cuandoSeArmaElMensaje_entoncesLoDiceEnSingular() {
        assertThat(
                        AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(72), TREINTA)
                                .orElseThrow()
                                .mensaje())
                .contains("Le falta 1 día:");
    }

    @Test
    void dadoUnPeriodoSinNormativa_cuandoSeCarga_entoncesAdvierteQueNoSePudoComprobar() {
        LocalDate inicio = HOY.plusDays(200);

        AdvertenciaDeRemision advertencia =
                AdvertenciaDeRemision.de(HOY, POR_DEFECTO, inicio, OptionalInt.empty())
                        .orElseThrow();

        assertThat(advertencia.codigo()).isEqualTo(CodigoDeAdvertencia.RN23_SIN_NORMATIVA);
        assertThat(advertencia.ultimoPlazo()).isEqualTo(HOY.plusDays(43));
        assertThat(advertencia.diasAntesDelInicio()).isEmpty();
        assertThat(advertencia.fechaDeRemision()).isEmpty();
        assertThat(advertencia.diasDeMargen()).isEmpty();
        assertThat(advertencia.mensaje())
                .isEqualTo(
                        "El periodo no tiene versión de normativa, así que no se pudo comprobar"
                                + " RN-23. Con estos plazos, contados desde hoy, el distributivo se"
                                + " aprobaría el 02/01/2027, y el periodo empieza el 08/06/2027."
                                + " Elige la versión de normativa del periodo y vuelve a cargar la"
                                + " configuración para comprobarlo.");
    }

    @Test
    void dadaUnaEtapaOpcionalDesactivada_cuandoSeCarga_entoncesLaSumaEsMenorYPuedeAlcanzar() {
        List<Etapa> etapas = new ArrayList<>(PlazoTest.etapasPorDefecto());
        etapas.set(
                TipoDeEtapa.EN_REVISION_VRA.ordinal(),
                new Etapa(
                        TipoDeEtapa.EN_REVISION_VRA,
                        false,
                        "Revisión del Vicerrectorado",
                        "VICERRECTOR",
                        OptionalInt.of(5),
                        true));
        PlazosDeEtapas sinRevision = PlazosDeEtapas.de(etapas);

        // 38 días de plazos más los 30 de RN-23 caben en 68.
        assertThat(AdvertenciaDeRemision.de(HOY, sinRevision, HOY.plusDays(68), TREINTA)).isEmpty();
        assertThat(AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(68), TREINTA))
                .isPresent();
    }

    @Test
    void dadaUnaAdvertenciaGuardada_cuandoSeVuelveAArmar_entoncesEsLaMisma() {
        AdvertenciaDeRemision guardada =
                AdvertenciaDeRemision.de(HOY, POR_DEFECTO, HOY.plusDays(60), TREINTA).orElseThrow();

        assertThat(
                        new AdvertenciaDeRemision(
                                guardada.codigo(),
                                guardada.ultimoPlazo(),
                                guardada.inicioDelPeriodo(),
                                guardada.diasAntesDelInicio(),
                                guardada.fechaDeRemision(),
                                guardada.diasDeMargen()))
                .isEqualTo(guardada);
        assertThat(Optional.of(guardada.mensaje())).isPresent();
    }
}
