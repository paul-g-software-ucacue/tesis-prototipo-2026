package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;
import java.util.List;

/**
 * Una página de la consulta de la bitácora (esquema {@code PaginaDeBitacora}, FR-004).
 *
 * @param entradas las de la página, de la más reciente a la más antigua; como máximo {@code tamano}
 * @param total las que cumplen el filtro, recibidas hasta el corte
 * @param pagina el número de la página, desde 1
 * @param tamano las entradas por página
 * @param corte el instante hasta el que cuentan las entradas recibidas (research §2)
 */
public record PaginaDeEntradas(
        List<EntradaConsultada> entradas, long total, int pagina, int tamano, Instant corte) {

    /** Copia la lista. */
    public PaginaDeEntradas {
        entradas = List.copyOf(entradas);
    }
}
