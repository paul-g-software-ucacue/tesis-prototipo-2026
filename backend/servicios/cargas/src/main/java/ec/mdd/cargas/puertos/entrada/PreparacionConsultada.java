package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.PreparacionDelPeriodo;
import java.util.List;

/**
 * El tablero de preparación de un periodo y los periodos para elegir (F02-H02).
 *
 * @param periodos los periodos, del que empieza más tarde al más antiguo
 * @param tablero el tablero del periodo pedido, o del que empieza más tarde
 */
public record PreparacionConsultada(
        List<PeriodoParaElegir> periodos, PreparacionDelPeriodo tablero) {

    /** Copia la lista. */
    public PreparacionConsultada {
        periodos = List.copyOf(periodos);
    }
}
