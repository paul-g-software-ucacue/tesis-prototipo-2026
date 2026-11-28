package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.dominio.FirmasDeLaVersion.PasoFirmado;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Solo se exporta una versión firmada y sin inconsistencias (RF-53, F19-H02, D-120): las firmas
 * completas sobre el documento de esa versión, y 0 duras y 0 blandas sin justificar registradas al
 * congelarla. Sin el dato, no se exporta.
 */
class CondicionesDeExportacionTest {

    private static final String HASH = "a".repeat(64);
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I", 1, HASH, Instant.parse("2026-11-22T13:00:00Z"), "secretaria", false);
    private static final Optional<InconsistenciasAbiertas> NINGUNA =
            Optional.of(new InconsistenciasAbiertas(0, 0));

    private static FirmasDeLaVersion firmas(boolean... firmados) {
        return firmasCon(HASH, false, false, firmados);
    }

    private static FirmasDeLaVersion firmasCon(
            String hash, boolean anulada, boolean rechazada, boolean... firmados) {
        List<PasoFirmado> pasos =
                java.util.stream.IntStream.range(0, firmados.length)
                        .mapToObj(i -> new PasoFirmado(i + 1, firmados[i]))
                        .toList();
        return new FirmasDeLaVersion(hash, pasos, anulada, rechazada);
    }

    private static CondicionesDeExportacion de(
            Optional<FirmasDeLaVersion> firmas, Optional<InconsistenciasAbiertas> inconsistencias) {
        return CondicionesDeExportacion.de(PUBLICACION, firmas, inconsistencias);
    }

    @Test
    void dadasLasFirmasCompletasYNingunaInconsistencia_cuandoSeEvalua_entoncesSinMotivos() {
        CondicionesDeExportacion condiciones = de(Optional.of(firmas(true, true, true)), NINGUNA);

        assertThat(condiciones.motivos()).isEmpty();
        assertThatCode(condiciones::exigir).doesNotThrowAnyException();
    }

    @Test
    void dadaUnaCadenaDeUnSoloPasoFirmado_cuandoSeEvalua_entoncesSinMotivos() {
        assertThat(de(Optional.of(firmas(true)), NINGUNA).motivos()).isEmpty();
    }

    @Test
    void dadaUnaVersionSinDocumento_cuandoSeEvalua_entoncesSinDocumentoFirmado() {
        assertThat(de(Optional.empty(), NINGUNA).motivos())
                .containsExactly(MotivoDeRechazo.SIN_DOCUMENTO_FIRMADO);
    }

    @Test
    void dadoUnDocumentoDeOtraVersion_cuandoSeEvalua_entoncesDocumentoDeOtraVersion() {
        FirmasDeLaVersion otra = firmasCon("b".repeat(64), false, false, true, true, true);

        assertThat(de(Optional.of(otra), NINGUNA).motivos())
                .containsExactly(MotivoDeRechazo.DOCUMENTO_DE_OTRA_VERSION);
    }

    @Test
    void dadasFirmasAnuladasORechazadas_cuandoSeEvalua_entoncesAnuladasORechazadas() {
        FirmasDeLaVersion anulada = firmasCon(HASH, true, false, true, true, true);
        FirmasDeLaVersion rechazada = firmasCon(HASH, false, true, true, false, false);

        assertThat(de(Optional.of(anulada), NINGUNA).motivos())
                .containsExactly(MotivoDeRechazo.FIRMAS_ANULADAS_O_RECHAZADAS);
        assertThat(de(Optional.of(rechazada), NINGUNA).motivos())
                .containsExactly(
                        MotivoDeRechazo.FIRMAS_ANULADAS_O_RECHAZADAS,
                        MotivoDeRechazo.FIRMAS_INCOMPLETAS);
    }

    @Test
    void dadoUnPasoSinFirmarOSinPasos_cuandoSeEvalua_entoncesFirmasIncompletasConSusNumeros() {
        CondicionesDeExportacion dosDeTres = de(Optional.of(firmas(true, true, false)), NINGUNA);
        CondicionesDeExportacion sinPasos = de(Optional.of(firmas()), NINGUNA);

        assertThat(dosDeTres.motivos()).containsExactly(MotivoDeRechazo.FIRMAS_INCOMPLETAS);
        assertThat(dosDeTres.firmas())
                .hasValueSatisfying(
                        firmas -> {
                            assertThat(firmas.firmados()).isEqualTo(2);
                            assertThat(firmas.total()).isEqualTo(3);
                        });
        assertThat(sinPasos.motivos()).containsExactly(MotivoDeRechazo.FIRMAS_INCOMPLETAS);
    }

    @Test
    void dadoUnContenidoSinElRegistro_cuandoSeEvalua_entoncesInconsistenciasSinRegistro() {
        assertThat(de(Optional.of(firmas(true, true, true)), Optional.empty()).motivos())
                .containsExactly(MotivoDeRechazo.INCONSISTENCIAS_SIN_REGISTRO);
    }

    @Test
    void dadasInconsistenciasAbiertas_cuandoSeEvalua_entoncesInconsistenciasAbiertas() {
        Optional<FirmasDeLaVersion> completas = Optional.of(firmas(true, true, true));

        assertThat(de(completas, Optional.of(new InconsistenciasAbiertas(1, 0))).motivos())
                .containsExactly(MotivoDeRechazo.INCONSISTENCIAS_ABIERTAS);
        assertThat(de(completas, Optional.of(new InconsistenciasAbiertas(0, 2))).motivos())
                .containsExactly(MotivoDeRechazo.INCONSISTENCIAS_ABIERTAS);
    }

    @Test
    void dadasFaltaDeFirmasEInconsistencias_cuandoSeEvalua_entoncesLosDosMotivosEnOrden() {
        CondicionesDeExportacion condiciones =
                de(
                        Optional.of(firmas(true, false, false)),
                        Optional.of(new InconsistenciasAbiertas(1, 2)));

        assertThat(condiciones.motivos())
                .containsExactly(
                        MotivoDeRechazo.FIRMAS_INCOMPLETAS,
                        MotivoDeRechazo.INCONSISTENCIAS_ABIERTAS);
        assertThatThrownBy(condiciones::exigir)
                .isInstanceOfSatisfying(
                        ExportacionRechazada.class,
                        rechazo -> {
                            assertThat(rechazo.periodo()).isEqualTo("2026-I");
                            assertThat(rechazo.version()).isEqualTo(1);
                            assertThat(rechazo.condiciones()).isSameAs(condiciones);
                        });
    }

    @Test
    void dadosDatosFueraDeRango_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> new InconsistenciasAbiertas(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasoFirmado(0, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new FirmasDeLaVersion(
                                        HASH,
                                        List.of(new PasoFirmado(1, true), new PasoFirmado(1, true)),
                                        false,
                                        false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FirmasDeLaVersion("A".repeat(64), List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
