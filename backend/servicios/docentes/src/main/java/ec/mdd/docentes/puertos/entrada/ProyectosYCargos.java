package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.TipoDeCargo;
import java.util.List;

/**
 * Lo que Generación necesita para asignarle una actividad a un docente (RN-25, principio IX).
 *
 * @param proyectos los proyectos asignables del periodo en los que participa
 * @param cargos sus cargos vigentes
 */
public record ProyectosYCargos(List<Proyecto> proyectos, List<TipoDeCargo> cargos) {

    /** Copia las listas. */
    public ProyectosYCargos {
        proyectos = List.copyOf(proyectos);
        cargos = List.copyOf(cargos);
    }
}
