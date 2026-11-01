package ec.mdd.generacion.dominio;

import java.util.List;

/**
 * Lo que hace falta saber de un docente para asignarle una actividad.
 *
 * @param proyectos los proyectos asignables del periodo en los que participa
 * @param cargos sus cargos vigentes
 */
public record ProyectosYCargosDelDocente(List<ProyectoDelDocente> proyectos, List<String> cargos) {

    /** Copia las listas. */
    public ProyectosYCargosDelDocente {
        proyectos = List.copyOf(proyectos);
        cargos = List.copyOf(cargos);
    }
}
