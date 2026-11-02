package ec.mdd.normativa.puertos.entrada;

/** Las cifras de las reglas duras de un periodo, para validar cada asignación (F07-H02). */
public interface ConsultarReglasDeAsignacion {

    /**
     * Las cifras de la versión que rige el periodo.
     *
     * @param periodo el código del periodo
     * @throws PeriodoSinVersion si no tiene versión
     * @throws IllegalStateException si a la versión le falta una cifra
     */
    ReglasDeLaVersion reglas(String periodo);
}
