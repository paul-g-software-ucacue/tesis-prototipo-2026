package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.AdvertenciaDeCargo;
import ec.mdd.docentes.dominio.Cargo;
import java.util.List;

/**
 * Un cargo con el nombre de su docente y lo que hay que revisar de él hoy (F03-H05, FR-006).
 *
 * @param cargo el cargo
 * @param nombreDelDocente el nombre completo del docente
 * @param advertencias sus advertencias; vacío si no tiene
 */
public record CargoConsultado(
        Cargo cargo, String nombreDelDocente, List<AdvertenciaDeCargo> advertencias) {

    /** Copia la lista. */
    public CargoConsultado {
        advertencias = List.copyOf(advertencias);
    }
}
