package ec.mdd.reporting.dominio;

import java.util.List;
import java.util.Objects;

/**
 * El contenido congelado de un docente en una versión, como lo da Generación (D-115).
 *
 * @param hash el hash de la versión de la que sale
 * @param docente sus datos
 * @param asignaturas sus asignaturas-paralelo
 * @param actividades sus actividades no lectivas
 * @param horas su carga horaria por componente
 */
public record ContenidoDelDocente(
        String hash,
        DatosDelDocente docente,
        List<FilaDeAsignatura> asignaturas,
        List<FilaDeActividad> actividades,
        HorasPorComponente horas) {

    /** Exige los datos y copia las listas. */
    public ContenidoDelDocente {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(horas, "horas");
        asignaturas = List.copyOf(asignaturas);
        actividades = List.copyOf(actividades);
    }
}
