package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.CargoVigente;
import ec.mdd.generacion.dominio.ReglasDelPanel;
import java.util.List;
import java.util.Optional;

/** Lo que el panel de inconsistencias pide a Normativa y a Docentes (RF-29, ADR-023). */
public interface DatosParaElPanel {

    /**
     * Las severidades y las cifras del panel, de la versión que rige el periodo.
     *
     * @param periodo el código del periodo
     * @return vacío si el periodo no tiene versión
     */
    Optional<ReglasDelPanel> reglasDelPanel(String periodo);

    /** Los cargos vigentes de todos los docentes. */
    List<CargoVigente> cargosVigentes();
}
