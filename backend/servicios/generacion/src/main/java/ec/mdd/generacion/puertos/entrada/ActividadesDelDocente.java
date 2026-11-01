package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.CargaHoraria;
import java.util.List;
import java.util.Objects;

/**
 * Las actividades de un docente en un parcial, con sus totales en todas las carreras (RF-27).
 *
 * @param docente su número de identificación
 * @param nombreDocente sus apellidos y nombres
 * @param actividades sus actividades
 * @param totales sus horas por componente, en todas las carreras y unidades
 */
public record ActividadesDelDocente(
        String docente,
        String nombreDocente,
        List<ActividadNoLectiva> actividades,
        CargaHoraria totales) {

    /** Exige los datos y copia la lista. */
    public ActividadesDelDocente {
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(nombreDocente, "nombreDocente");
        Objects.requireNonNull(totales, "totales");
        actividades = List.copyOf(actividades);
    }
}
