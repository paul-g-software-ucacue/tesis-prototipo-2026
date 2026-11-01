package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las horas ya asignadas a los docentes, a partir de las propuestas, las actividades y la oferta de
 * ese momento (RF-25, D-01, D-75): cada propuesta suma las horas actuales de su paralelo, y cada
 * actividad, las suyas. Es una función pura.
 *
 * <p>Una propuesta de un paralelo retirado no cuenta, y la del paralelo que se mira tampoco, porque
 * se puede reemplazar. Lo de otra carrera son «otras carreras».
 */
public final class HorasDelPeriodo {

    private HorasDelPeriodo() {}

    /**
     * Las horas de cada docente, propuestas y actividades juntas, mirando un paralelo.
     *
     * @param propuestas las de la versión
     * @param actividades las de la versión
     * @param oferta las asignaturas-paralelo del periodo
     * @param paralelo la asignatura-paralelo que se mira: su propuesta no cuenta
     * @return por número de identificación; un docente que no está no tiene horas asignadas
     */
    public static Map<String, HorasDelDocente> calcular(
            List<Propuesta> propuestas,
            List<ActividadNoLectiva> actividades,
            List<AsignaturaParalelo> oferta,
            AsignaturaParalelo paralelo) {
        Map<String, HorasDelDocente> horas = new HashMap<>();
        for (Propuesta propuesta : propuestas) {
            if (!propuesta.esDe(paralelo.asignatura().codigo(), paralelo.paralelo())) {
                sumarPropuesta(horas, propuesta, oferta, paralelo.carrera());
            }
        }
        actividades.forEach(una -> sumarActividad(horas, una, paralelo.carrera()));
        return horas;
    }

    /**
     * Las horas de cada docente sin la actividad que se está guardando: al reemplazarla, sus horas
     * anteriores no cuentan.
     *
     * @param propuestas las de la versión
     * @param actividades las de la versión
     * @param oferta las asignaturas-paralelo del periodo
     * @param nueva la actividad que se guarda
     * @return por número de identificación
     */
    public static Map<String, HorasDelDocente> sinLaActividad(
            List<Propuesta> propuestas,
            List<ActividadNoLectiva> actividades,
            List<AsignaturaParalelo> oferta,
            ActividadNoLectiva nueva) {
        Map<String, HorasDelDocente> horas = new HashMap<>();
        propuestas.forEach(una -> sumarPropuesta(horas, una, oferta, nueva.carrera()));
        actividades.stream()
                .filter(
                        una ->
                                !una.esDe(
                                        nueva.carrera(),
                                        nueva.docente(),
                                        nueva.actividad(),
                                        nueva.proyecto()))
                .forEach(una -> sumarActividad(horas, una, nueva.carrera()));
        return horas;
    }

    private static void sumarPropuesta(
            Map<String, HorasDelDocente> horas,
            Propuesta propuesta,
            List<AsignaturaParalelo> oferta,
            String carrera) {
        oferta.stream()
                .filter(
                        una ->
                                !una.retirada()
                                        && propuesta.esDe(
                                                una.asignatura().codigo(), una.paralelo()))
                .findFirst()
                .ifPresent(
                        una ->
                                sumar(
                                        horas,
                                        propuesta.docente(),
                                        una.horasSemanales(),
                                        !propuesta.carrera().equals(carrera)));
    }

    private static void sumarActividad(
            Map<String, HorasDelDocente> horas, ActividadNoLectiva actividad, String carrera) {
        sumar(horas, actividad.docente(), actividad.horas(), !actividad.carrera().equals(carrera));
    }

    private static void sumar(
            Map<String, HorasDelDocente> horas, String docente, BigDecimal suyas, boolean otra) {
        horas.merge(
                docente,
                new HorasDelDocente(suyas, otra ? suyas : BigDecimal.ZERO),
                (unas, otras) ->
                        new HorasDelDocente(
                                unas.asignadas().add(otras.asignadas()),
                                unas.enOtrasCarreras().add(otras.enOtrasCarreras())));
    }
}
