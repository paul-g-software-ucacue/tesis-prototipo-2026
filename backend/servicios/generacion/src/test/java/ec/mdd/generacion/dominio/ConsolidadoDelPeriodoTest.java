package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Todas las carreras, con su estado y sus horas, y si sigue al día (RF-32). */
class ConsolidadoDelPeriodoTest {

    private static final String PERIODO = "2026-I";
    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final Consolidacion CONSOLIDACION =
            new Consolidacion("consolidacion.demo", MOMENTO);
    private static final PanelDeInconsistencias PANEL =
            new PanelDeInconsistencias(
                    PERIODO,
                    true,
                    Optional.of(UUID.randomUUID()),
                    1,
                    List.of(
                            entrada(TipoDeInconsistencia.DOCENTE_SIN_CLASE, Severidad.DURA),
                            entrada(TipoDeInconsistencia.SOBRECARGA, Severidad.DURA),
                            entrada(TipoDeInconsistencia.PROPORCION, Severidad.BLANDA)));

    private final List<AsignaturaParalelo> oferta =
            List.of(
                    paralelo("TDS", "POO", "A", "4", false),
                    paralelo("TDS", "BDD", "A", "6", false),
                    paralelo("TDS", "RED", "A", "9", true),
                    paralelo("ELE", "CIR", "A", "3", false));

    @Test
    void conLasCarreras_cuandoSeArma_entoncesCadaUnaTieneSuEstadoAvanceYHoras() {
        ConsolidadoDelPeriodo consolidado = armar(version(), Optional.empty(), Optional.empty());

        assertThat(consolidado.carreras())
                .extracting(
                        CarreraConsolidada::carrera,
                        CarreraConsolidada::estado,
                        una -> una.avance().ofertadas(),
                        una -> una.avance().conPropuesta())
                .containsExactly(
                        tuple("ELE", EstadoDelParcial.EN_ELABORACION, 1, 0),
                        tuple("TDS", EstadoDelParcial.ENVIADO, 2, 1));
        CargaHoraria tds = consolidado.carreras().get(1).horas();
        assertThat(tds.clase()).isEqualByComparingTo("4");
        assertThat(tds.otrasDocencia()).isEqualByComparingTo("2");
        assertThat(tds.gestion()).isEqualByComparingTo("5");
    }

    @Test
    void unaCarreraSinPropuestas_cuandoSeArma_entoncesSaleConCero() {
        CarreraConsolidada ele =
                armar(version(), Optional.empty(), Optional.empty()).carreras().getFirst();

        assertThat(ele.horas().asignadas()).isEqualByComparingTo("0");
    }

    @Test
    void conVariasCarreras_cuandoSeArma_entoncesLosTotalesLasSuman() {
        CargaHoraria totales = armar(version(), Optional.empty(), Optional.empty()).totales();

        assertThat(totales.clase()).isEqualByComparingTo("4");
        assertThat(totales.gestion()).isEqualByComparingTo("5");
        assertThat(totales.investigacion()).isEqualByComparingTo("0");
    }

    @Test
    void unRetirado_cuandoSeArma_entoncesNoCuentaEnLasHoras() {
        Propuesta retirada = propuesta("RED", "A", "TDS");
        VersionDistributivo version =
                new VersionDistributivo(
                        UUID.randomUUID(),
                        1,
                        EstadoDeVersion.EDITABLE,
                        List.of(new ParcialCarrera("TDS", EstadoDelParcial.EN_ELABORACION)),
                        List.of(retirada));

        assertThat(armar(version, Optional.empty(), Optional.empty()).totales().clase())
                .isEqualByComparingTo("0");
    }

    @Test
    void conElPanel_cuandoSeArma_entoncesCuentaLasAbiertas() {
        ConsolidadoDelPeriodo consolidado = armar(version(), Optional.empty(), Optional.empty());

        assertThat(consolidado.duras()).isEqualTo(2);
        assertThat(consolidado.blandas()).isEqualTo(1);
    }

    @Test
    void conUnCambioPosterior_cuandoSeArma_entoncesQuedaDesactualizado() {
        assertThat(
                        armar(
                                        version(),
                                        Optional.of(CONSOLIDACION),
                                        Optional.of(MOMENTO.plusSeconds(1)))
                                .desactualizado())
                .isTrue();
    }

    @Test
    void conUnCambioAnterior_cuandoSeArma_entoncesNoEstaDesactualizado() {
        assertThat(
                        armar(
                                        version(),
                                        Optional.of(CONSOLIDACION),
                                        Optional.of(MOMENTO.minusSeconds(1)))
                                .desactualizado())
                .isFalse();
    }

    @Test
    void enElMismoInstante_cuandoSeArma_entoncesNoEstaDesactualizado() {
        assertThat(
                        armar(version(), Optional.of(CONSOLIDACION), Optional.of(MOMENTO))
                                .desactualizado())
                .isFalse();
    }

    @Test
    void sinCambios_cuandoSeArma_entoncesNoEstaDesactualizado() {
        assertThat(armar(version(), Optional.of(CONSOLIDACION), Optional.empty()).desactualizado())
                .isFalse();
    }

    @Test
    void sinConsolidar_cuandoHayCambios_entoncesNoEstaDesactualizado() {
        assertThat(armar(version(), Optional.empty(), Optional.of(MOMENTO)).desactualizado())
                .isFalse();
    }

    @Test
    void sinDistributivo_cuandoSeArma_entoncesNoEstaCreado() {
        ConsolidadoDelPeriodo consolidado = ConsolidadoDelPeriodo.sinDistributivo(PERIODO, true);

        assertThat(consolidado.creado()).isFalse();
        assertThat(consolidado.carreras()).isEmpty();
    }

    private ConsolidadoDelPeriodo armar(
            VersionDistributivo version,
            Optional<Consolidacion> consolidacion,
            Optional<Instant> ultimoCambio) {
        return ConsolidadoDelPeriodo.armar(
                PANEL, version, oferta, consolidacion, ultimoCambio, Optional.empty());
    }

    private static VersionDistributivo version() {
        return new VersionDistributivo(
                UUID.randomUUID(),
                1,
                EstadoDeVersion.EDITABLE,
                List.of(
                        new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO),
                        new ParcialCarrera("ELE", EstadoDelParcial.EN_ELABORACION)),
                List.of(propuesta("POO", "A", "TDS")),
                List.of(
                        actividad("TDS", Componente.OTRAS_DOCENCIA, "2"),
                        actividad("TDS", Componente.GESTION, "5")));
    }

    private static Propuesta propuesta(String asignatura, String paralelo, String carrera) {
        return new Propuesta(
                asignatura,
                paralelo,
                carrera,
                "0109990010",
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                MOMENTO,
                "coordinacion.demo");
    }

    private static ActividadNoLectiva actividad(
            String carrera, Componente componente, String horas) {
        return new ActividadNoLectiva(
                carrera,
                "0109990010",
                "PEREZ LUIS",
                "140-b",
                "Planificar",
                componente,
                null,
                null,
                new BigDecimal(horas),
                MOMENTO,
                "coordinacion.demo");
    }

    private static AsignaturaParalelo paralelo(
            String carrera, String asignatura, String paralelo, String horas, boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada(asignatura, asignatura, "06", "061"),
                paralelo,
                carrera,
                new BigDecimal(horas),
                false,
                retirada);
    }

    private static EntradaDelPanel entrada(TipoDeInconsistencia tipo, Severidad severidad) {
        return new EntradaDelPanel(
                new InconsistenciaGlobal(
                        tipo,
                        severidad,
                        ObjetoAfectado.carrera(tipo.name()),
                        DatosDeLaInconsistencia.ninguno()),
                Optional.empty());
    }
}
