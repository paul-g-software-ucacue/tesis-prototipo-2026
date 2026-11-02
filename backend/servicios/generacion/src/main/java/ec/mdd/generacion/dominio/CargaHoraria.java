package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Los totales de un docente por componente, en todas las carreras y unidades de la versión (RF-27,
 * D-01), y su proporción (RN-05). Se calcula: no se guarda.
 *
 * <p>Puede superar el límite por datos anteriores a F07-H02 o por un cambio en la oferta.
 *
 * @param clase las horas actuales de sus propuestas
 * @param otrasDocencia las de otras actividades de docencia
 * @param investigacion las de investigación
 * @param vinculacion las de vinculación
 * @param gestion las de gestión
 */
public record CargaHoraria(
        BigDecimal clase,
        BigDecimal otrasDocencia,
        BigDecimal investigacion,
        BigDecimal vinculacion,
        BigDecimal gestion) {

    /**
     * Suma las asignaciones de un docente. Es una función pura.
     *
     * @param docente su número de identificación
     * @param propuestas las de la versión
     * @param actividades las de la versión
     * @param oferta las asignaturas-paralelo del periodo
     * @return sus totales
     */
    public static CargaHoraria calcular(
            String docente,
            List<Propuesta> propuestas,
            List<ActividadNoLectiva> actividades,
            List<AsignaturaParalelo> oferta) {
        List<ActividadNoLectiva> suyas =
                actividades.stream().filter(una -> una.docente().equals(docente)).toList();
        BigDecimal clase =
                propuestas.stream()
                        .filter(una -> una.docente().equals(docente))
                        .map(una -> HorasDelPeriodo.horasDe(una, oferta))
                        .flatMap(Optional::stream)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CargaHoraria(
                clase,
                suma(suyas, Componente.OTRAS_DOCENCIA),
                suma(suyas, Componente.INVESTIGACION),
                suma(suyas, Componente.VINCULACION),
                suma(suyas, Componente.GESTION));
    }

    /** Todas sus horas: la suma de los cinco componentes. */
    public BigDecimal asignadas() {
        return Stream.of(clase, otrasDocencia, investigacion, vinculacion, gestion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** La misma carga con más horas de clase. */
    public CargaHoraria conMasClase(BigDecimal horas) {
        return new CargaHoraria(
                clase.add(horas), otrasDocencia, investigacion, vinculacion, gestion);
    }

    /** Su límite menos las horas asignadas, nunca menos de 0 (D-75). */
    public BigDecimal disponibles(BigDecimal limite) {
        return limite.subtract(asignadas()).max(BigDecimal.ZERO);
    }

    /** Lo que pasa de su límite; 0 si no se pasa. */
    public BigDecimal exceso(BigDecimal limite) {
        return asignadas().subtract(limite).max(BigDecimal.ZERO);
    }

    /** Su proporción de clase y otras actividades de docencia; vacía si no tiene ninguna. */
    public Optional<Proporcion> proporcion() {
        return Proporcion.de(clase, otrasDocencia);
    }

    private static BigDecimal suma(List<ActividadNoLectiva> actividades, Componente componente) {
        return actividades.stream()
                .filter(una -> una.componente() == componente)
                .map(ActividadNoLectiva::horas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
