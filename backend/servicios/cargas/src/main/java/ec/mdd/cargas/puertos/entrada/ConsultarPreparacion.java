package ec.mdd.cargas.puertos.entrada;

import java.util.Optional;

/**
 * Caso de uso: el tablero de preparación de un periodo (RF-56, F02-H02, glosario «Tablero de
 * preparación»). Refleja {@code consultarPreparacionDelPeriodo}.
 */
public interface ConsultarPreparacion {

    /**
     * El tablero del periodo y la lista de periodos.
     *
     * @param periodo el código del periodo; si no va, el que empieza más tarde
     * @throws ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado si no está en los ciclos académicos
     */
    PreparacionConsultada preparacion(Optional<String> periodo);
}
