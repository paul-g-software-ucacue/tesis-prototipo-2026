package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las horas ya asignadas a los docentes, a partir de las propuestas y de la oferta de ese momento
 * (RF-25, D-01): cada propuesta suma las horas actuales de su paralelo. Es una función pura.
 *
 * <p>Una propuesta de un paralelo retirado no cuenta, y la del paralelo que se mira tampoco, porque
 * se puede reemplazar. Las de otra carrera son «otras carreras».
 */
public final class HorasDelPeriodo {

    private HorasDelPeriodo() {}

    /**
     * Las horas de cada docente con propuestas.
     *
     * @param propuestas las de la versión
     * @param oferta las asignaturas-paralelo del periodo
     * @param paralelo la asignatura-paralelo que se mira
     * @return por número de identificación; un docente que no está no tiene horas asignadas
     */
    public static Map<String, HorasDelDocente> calcular(
            List<Propuesta> propuestas,
            List<AsignaturaParalelo> oferta,
            AsignaturaParalelo paralelo) {
        Map<String, HorasDelDocente> horas = new HashMap<>();
        for (Propuesta propuesta : propuestas) {
            if (propuesta.esDe(paralelo.asignatura().codigo(), paralelo.paralelo())) {
                continue;
            }
            oferta.stream()
                    .filter(
                            una ->
                                    !una.retirada()
                                            && propuesta.esDe(
                                                    una.asignatura().codigo(), una.paralelo()))
                    .findFirst()
                    .ifPresent(
                            una -> {
                                boolean otra = !propuesta.carrera().equals(paralelo.carrera());
                                HorasDelDocente suyas =
                                        new HorasDelDocente(
                                                una.horasSemanales(),
                                                otra ? una.horasSemanales() : BigDecimal.ZERO);
                                horas.merge(propuesta.docente(), suyas, HorasDelPeriodo::sumar);
                            });
        }
        return horas;
    }

    private static HorasDelDocente sumar(HorasDelDocente unas, HorasDelDocente otras) {
        return new HorasDelDocente(
                unas.asignadas().add(otras.asignadas()),
                unas.enOtrasCarreras().add(otras.enOtrasCarreras()));
    }
}
