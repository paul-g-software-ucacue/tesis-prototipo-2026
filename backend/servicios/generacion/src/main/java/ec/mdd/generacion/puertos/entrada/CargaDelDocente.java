package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.MetaDeProporcion;
import java.util.Objects;

/**
 * La carga horaria de un docente en un periodo, con sus horas contratadas y la meta de la
 * proporción. Solo trae sumas (D-82).
 *
 * @param docente su número de identificación
 * @param nombreDocente sus apellidos y nombres
 * @param horasContratadas sus horas contratadas
 * @param carga sus totales
 * @param meta la meta de la versión de normativa que rige el periodo
 */
public record CargaDelDocente(
        String docente,
        String nombreDocente,
        int horasContratadas,
        CargaHoraria carga,
        MetaDeProporcion meta) {

    /** Exige los datos. */
    public CargaDelDocente {
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(nombreDocente, "nombreDocente");
        Objects.requireNonNull(carga, "carga");
        Objects.requireNonNull(meta, "meta");
    }
}
