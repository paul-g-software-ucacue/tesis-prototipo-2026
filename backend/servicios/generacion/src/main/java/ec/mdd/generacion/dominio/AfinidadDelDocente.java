package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;

/**
 * El puntaje de un docente en cada asignatura de la oferta de un periodo.
 *
 * @param periodo el código del periodo
 * @param version el número de la versión de normativa que da los pesos
 * @param puntajes del mayor al menor y, a igual puntaje, por nombre; vacío si no hay oferta
 */
public record AfinidadDelDocente(String periodo, int version, List<PuntajeAfinidad> puntajes) {

    /** Exige el periodo y copia los puntajes. */
    public AfinidadDelDocente {
        Objects.requireNonNull(periodo, "periodo");
        puntajes = List.copyOf(puntajes);
    }
}
