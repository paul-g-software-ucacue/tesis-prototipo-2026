package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Una blanda justificada deja de estar abierta; una dura, nunca (MD-07). */
class PanelDeInconsistenciasTest {

    private static final String PERIODO = "2026-I";
    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final ObjetoAfectado DOCENTE =
            new ObjetoAfectado(ObjetoAfectado.Clase.DOCENTE, "D1", "Docente Uno");
    private static final InconsistenciaGlobal BLANDA =
            global(TipoDeInconsistencia.PROPORCION, Severidad.BLANDA, DOCENTE);
    private static final InconsistenciaGlobal DURA =
            global(TipoDeInconsistencia.DOCENTE_SIN_CLASE, Severidad.DURA, DOCENTE);

    private static final VersionDistributivo VERSION =
            new VersionDistributivo(
                    UUID.randomUUID(), 3, EstadoDeVersion.EDITABLE, List.of(), List.of());

    @Test
    void sinJustificacion_cuandoSeArma_entoncesEstaAbierta() {
        PanelDeInconsistencias panel = armar(VERSION, List.of(BLANDA), List.of());

        assertThat(panel.entradas().getFirst().abierta()).isTrue();
        assertThat(panel.abiertas(Severidad.BLANDA)).isEqualTo(1);
    }

    @Test
    void conJustificacion_cuandoSeArma_entoncesNoEstaAbierta() {
        PanelDeInconsistencias panel =
                armar(VERSION, List.of(BLANDA), List.of(justificacion(BLANDA)));

        EntradaDelPanel entrada = panel.entradas().getFirst();
        assertThat(entrada.abierta()).isFalse();
        assertThat(entrada.justificacion()).isPresent();
        assertThat(panel.abiertas(Severidad.BLANDA)).isZero();
    }

    @Test
    void unaJustificacionGuardada_cuandoLaInconsistenciaEsDura_entoncesNoLaCierra() {
        PanelDeInconsistencias panel = armar(VERSION, List.of(DURA), List.of(justificacion(DURA)));

        EntradaDelPanel entrada = panel.entradas().getFirst();
        assertThat(entrada.abierta()).isTrue();
        assertThat(entrada.justificacion()).isEmpty();
        assertThat(panel.abiertas(Severidad.DURA)).isEqualTo(1);
    }

    @Test
    void unaJustificacionDeOtroObjeto_cuandoSeArma_entoncesNoCuenta() {
        JustificacionDeInconsistencia otra =
                new JustificacionDeInconsistencia(
                        TipoDeInconsistencia.PROPORCION,
                        "D2",
                        "Es de otro docente",
                        "consolidacion.demo",
                        MOMENTO);

        assertThat(armar(VERSION, List.of(BLANDA), List.of(otra)).abiertas(Severidad.BLANDA))
                .isEqualTo(1);
    }

    @Test
    void unaJustificacionSinInconsistencia_cuandoSeArma_entoncesNoSeMuestra() {
        assertThat(armar(VERSION, List.of(), List.of(justificacion(BLANDA))).entradas()).isEmpty();
    }

    @Test
    void variasEntradas_cuandoSeArma_entoncesPrimeroLasDurasYAlFinalLasJustificadas() {
        InconsistenciaGlobal justificada =
                global(TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS, Severidad.BLANDA, DOCENTE);

        PanelDeInconsistencias panel =
                armar(
                        VERSION,
                        List.of(justificada, BLANDA, DURA),
                        List.of(justificacion(justificada)));

        assertThat(panel.entradas())
                .extracting(entrada -> entrada.inconsistencia().tipo())
                .containsExactly(
                        TipoDeInconsistencia.DOCENTE_SIN_CLASE,
                        TipoDeInconsistencia.PROPORCION,
                        TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS);
    }

    @Test
    void unaPropuestaConExcepcion_cuandoSeArma_entoncesSaleJustificada() {
        Propuesta excepcion =
                new Propuesta(
                        "POO",
                        "A",
                        "TDS",
                        "D1",
                        "Docente Uno",
                        new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó esta materia"),
                        MOMENTO,
                        "coordinacion.demo");
        VersionDistributivo version =
                new VersionDistributivo(
                        UUID.randomUUID(),
                        3,
                        EstadoDeVersion.EDITABLE,
                        List.of(),
                        List.of(excepcion));

        PanelDeInconsistencias panel =
                PanelDeInconsistencias.armar(
                        PERIODO, true, version, List.of(), List.of(), List.of(paralelo(false)));

        EntradaDelPanel entrada = panel.entradas().getFirst();
        assertThat(entrada.inconsistencia().tipo())
                .isEqualTo(TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD);
        assertThat(entrada.abierta()).isFalse();
        assertThat(entrada.inconsistencia().objeto().clave()).isEqualTo("POO/A");
        assertThat(entrada.inconsistencia().datos().lista()).containsExactly("Docente Uno");
        assertThat(entrada.justificacion().orElseThrow().texto()).isEqualTo("Dictó esta materia");
        assertThat(entrada.justificacion().orElseThrow().por()).isEqualTo("coordinacion.demo");
    }

    @Test
    void unaExcepcionEnUnRetirado_cuandoSeArma_entoncesNoSale() {
        Propuesta excepcion =
                new Propuesta(
                        "POO",
                        "A",
                        "TDS",
                        "D1",
                        "Docente Uno",
                        new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó esta materia"),
                        MOMENTO,
                        "coordinacion.demo");
        VersionDistributivo version =
                new VersionDistributivo(
                        UUID.randomUUID(),
                        3,
                        EstadoDeVersion.EDITABLE,
                        List.of(),
                        List.of(excepcion));

        assertThat(
                        PanelDeInconsistencias.armar(
                                        PERIODO,
                                        true,
                                        version,
                                        List.of(),
                                        List.of(),
                                        List.of(paralelo(true)))
                                .entradas())
                .isEmpty();
    }

    @Test
    void conEntradas_cuandoSeBusca_entoncesLaEncuentraPorTipoYObjeto() {
        PanelDeInconsistencias panel = armar(VERSION, List.of(BLANDA, DURA), List.of());

        assertThat(panel.entrada(TipoDeInconsistencia.PROPORCION, "D1")).isPresent();
        assertThat(panel.entrada(TipoDeInconsistencia.PROPORCION, "D2")).isEmpty();
        assertThat(panel.entrada(TipoDeInconsistencia.SOBRECARGA, "D1")).isEmpty();
    }

    @Test
    void sinDistributivo_cuandoSeArma_entoncesNoEstaCreadoNiTieneEntradas() {
        PanelDeInconsistencias panel = PanelDeInconsistencias.sinDistributivo(PERIODO, true);

        assertThat(panel.creado()).isFalse();
        assertThat(panel.entradas()).isEmpty();
    }

    private static PanelDeInconsistencias armar(
            VersionDistributivo version,
            List<InconsistenciaGlobal> halladas,
            List<JustificacionDeInconsistencia> justificaciones) {
        return PanelDeInconsistencias.armar(
                PERIODO, true, version, halladas, justificaciones, List.of());
    }

    private static InconsistenciaGlobal global(
            TipoDeInconsistencia tipo, Severidad severidad, ObjetoAfectado objeto) {
        return new InconsistenciaGlobal(tipo, severidad, objeto, DatosDeLaInconsistencia.ninguno());
    }

    private static JustificacionDeInconsistencia justificacion(InconsistenciaGlobal una) {
        return new JustificacionDeInconsistencia(
                una.tipo(),
                una.objeto().clave(),
                "Se acepta por el motivo escrito",
                "consolidacion.demo",
                MOMENTO);
    }

    private static AsignaturaParalelo paralelo(boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada("POO", "Programación", "06", "061"),
                "A",
                "TDS",
                new BigDecimal("4"),
                false,
                retirada);
    }
}
