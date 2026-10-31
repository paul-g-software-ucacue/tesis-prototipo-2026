package ec.mdd.normativa.puertos.entrada;

/** Los pesos de afinidad de un periodo, para calcular la afinidad (F06-H01, ADR-023). */
public interface ConsultarPesosDelPeriodo {

    /**
     * Los pesos de la versión que rige el periodo, también si el periodo ya se cerró.
     *
     * @param periodo el código del periodo
     * @throws PeriodoSinVersion si no tiene versión
     */
    PesosDeLaVersion pesos(String periodo);
}
