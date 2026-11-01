package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.SumaPorComponente;
import java.util.List;
import java.util.Objects;

/**
 * Las actividades de un docente en un parcial, con su suma.
 *
 * @param docente su número de identificación
 * @param nombreDocente sus apellidos y nombres
 * @param actividades sus actividades
 * @param suma las horas por componente
 */
public record ActividadesDelDocente(
        String docente,
        String nombreDocente,
        List<ActividadNoLectiva> actividades,
        SumaPorComponente suma) {

    /** Exige los datos y copia la lista. */
    public ActividadesDelDocente {
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(nombreDocente, "nombreDocente");
        Objects.requireNonNull(suma, "suma");
        actividades = List.copyOf(actividades);
    }
}
