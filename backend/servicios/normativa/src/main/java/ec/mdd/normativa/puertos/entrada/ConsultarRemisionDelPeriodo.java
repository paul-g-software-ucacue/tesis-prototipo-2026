package ec.mdd.normativa.puertos.entrada;

/**
 * Los días de RN-23 de un periodo, para la advertencia de Aprobación al cargar la configuración del
 * flujo (F12-H03, D-114, ADR-023).
 */
@FunctionalInterface
public interface ConsultarRemisionDelPeriodo {

    /**
     * Los días de la versión que rige el periodo.
     *
     * @param periodo el código del periodo
     * @return la versión y los días antes del inicio del periodo
     * @throws PeriodoSinVersion si no tiene versión
     */
    RemisionDeLaVersion remision(String periodo);
}
