package ec.mdd.validacionocs.dominio;

/**
 * El estado de un periodo, según Académico.
 *
 * @param estado el estado, como lo nombra Académico
 */
public record EstadoDelPeriodo(String estado) {

    /** La configuración solo cambia en Planificación (RF-34). */
    public boolean enPlanificacion() {
        return "PLANIFICACION".equals(estado);
    }

    /** Un periodo cerrado: el distributivo ya no cambia de etapa (F12-H02). */
    public boolean cerrado() {
        return "CERRADO".equals(estado);
    }
}
