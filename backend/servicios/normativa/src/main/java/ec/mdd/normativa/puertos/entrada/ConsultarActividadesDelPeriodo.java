package ec.mdd.normativa.puertos.entrada;

/** El catálogo de actividades de un periodo, para asignarlas (F06-H04, ADR-023). */
public interface ConsultarActividadesDelPeriodo {

    /**
     * El catálogo de la versión que rige el periodo, también si el periodo ya se cerró.
     *
     * @param periodo el código del periodo
     * @throws PeriodoSinVersion si no tiene versión
     */
    ActividadesDeLaVersion actividades(String periodo);
}
