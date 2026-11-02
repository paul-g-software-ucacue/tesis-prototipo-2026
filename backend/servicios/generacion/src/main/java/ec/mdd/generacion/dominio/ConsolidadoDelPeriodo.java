package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El distributivo institucional de un periodo (RF-32). Se calcula al abrirlo: solo la consolidación
 * y el informe están guardados. Las unidades todavía no tienen parcial: lo suyo llega en los
 * totales.
 *
 * @param periodo el código del periodo
 * @param modificable si admite consolidar y adjuntar
 * @param version el id de la versión; vacío sin distributivo
 * @param numero el número de la versión; 0 sin distributivo
 * @param consolidacion la última; vacía si no se consolidó
 * @param desactualizado si una asignación cambió después de consolidar
 * @param carreras las carreras con parcial
 * @param totales las horas de toda la versión
 * @param duras las inconsistencias duras abiertas
 * @param blandas las inconsistencias blandas abiertas
 * @param informe el vigente; vacío si no hay
 */
public record ConsolidadoDelPeriodo(
        String periodo,
        boolean modificable,
        Optional<UUID> version,
        int numero,
        Optional<Consolidacion> consolidacion,
        boolean desactualizado,
        List<CarreraConsolidada> carreras,
        CargaHoraria totales,
        long duras,
        long blandas,
        Optional<InformeSustentado> informe) {

    private static final CargaHoraria SIN_HORAS =
            new CargaHoraria(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);

    /** Copia la lista. */
    public ConsolidadoDelPeriodo {
        carreras = List.copyOf(carreras);
    }

    /** Un periodo sin distributivo: no hay nada que consolidar. */
    public static ConsolidadoDelPeriodo sinDistributivo(String periodo, boolean modificable) {
        return new ConsolidadoDelPeriodo(
                periodo,
                modificable,
                Optional.empty(),
                0,
                Optional.empty(),
                false,
                List.of(),
                SIN_HORAS,
                0,
                0,
                Optional.empty());
    }

    /**
     * Arma el consolidado de una versión. Es una función pura: las carreras salen de sus parciales,
     * sin cambiar su estado (MD-04), y quedan ordenadas por su código.
     *
     * @param panel el panel de la versión, del que salen las inconsistencias abiertas
     * @param version la versión vigente
     * @param oferta las asignaturas-paralelo del periodo
     * @param consolidacion la última; vacía si no se consolidó
     * @param ultimoCambio cuándo cambió por última vez una asignación
     * @param informe el vigente
     * @return el consolidado
     */
    public static ConsolidadoDelPeriodo armar(
            PanelDeInconsistencias panel,
            VersionDistributivo version,
            List<AsignaturaParalelo> oferta,
            Optional<Consolidacion> consolidacion,
            Optional<Instant> ultimoCambio,
            Optional<InformeSustentado> informe) {
        return new ConsolidadoDelPeriodo(
                panel.periodo(),
                panel.modificable(),
                Optional.of(version.id()),
                version.numero(),
                consolidacion,
                desactualizado(consolidacion, ultimoCambio),
                version.parciales().stream()
                        .sorted(Comparator.comparing(ParcialCarrera::carrera))
                        .map(parcial -> carrera(parcial, version, oferta))
                        .toList(),
                CargaHoraria.sumar(version.propuestas(), version.actividades(), oferta),
                panel.abiertas(Severidad.DURA),
                panel.abiertas(Severidad.BLANDA),
                informe);
    }

    private static boolean desactualizado(
            Optional<Consolidacion> consolidacion, Optional<Instant> ultimoCambio) {
        return consolidacion
                .flatMap(una -> ultimoCambio.map(cambio -> cambio.isAfter(una.en())))
                .orElse(false);
    }

    private static CarreraConsolidada carrera(
            ParcialCarrera parcial, VersionDistributivo version, List<AsignaturaParalelo> oferta) {
        String carrera = parcial.carrera();
        return new CarreraConsolidada(
                carrera,
                parcial.estado(),
                version.avance(carrera, oferta),
                CargaHoraria.sumar(
                        version.propuestas().stream()
                                .filter(una -> una.carrera().equals(carrera))
                                .toList(),
                        version.actividades().stream()
                                .filter(una -> una.carrera().equals(carrera))
                                .toList(),
                        oferta));
    }

    /** Si el periodo tiene distributivo. */
    public boolean creado() {
        return version.isPresent();
    }
}
