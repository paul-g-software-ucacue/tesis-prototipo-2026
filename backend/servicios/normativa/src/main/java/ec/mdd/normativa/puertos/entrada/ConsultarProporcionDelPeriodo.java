package ec.mdd.normativa.puertos.entrada;

/** La meta de la proporción de un periodo, para los totales del docente (F07-H01, ADR-023). */
public interface ConsultarProporcionDelPeriodo {

    /**
     * La meta de la versión que rige el periodo, también si el periodo ya se cerró.
     *
     * @param periodo el código del periodo
     * @throws PeriodoSinVersion si no tiene versión
     */
    ProporcionDeLaVersion proporcion(String periodo);
}
