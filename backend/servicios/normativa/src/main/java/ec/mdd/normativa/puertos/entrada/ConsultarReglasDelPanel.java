package ec.mdd.normativa.puertos.entrada;

/** Las severidades y las cifras de las reglas del panel de inconsistencias en un periodo. */
public interface ConsultarReglasDelPanel {

    /**
     * Lee las reglas del panel en la versión que rige el periodo.
     *
     * @param periodo el código del periodo
     * @return las severidades y las cifras
     * @throws PeriodoSinVersion si el periodo no tiene versión
     * @throws IllegalStateException si a la versión le falta un dato: no se inventa
     */
    ReglasDelPanelDeLaVersion reglas(String periodo);
}
