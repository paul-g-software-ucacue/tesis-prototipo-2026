package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ActividadDelCatalogo;
import ec.mdd.generacion.dominio.ProyectoDelDocente;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una actividad del catálogo frente a un docente: la pantalla solo muestra esto.
 *
 * @param actividad la actividad
 * @param proyectos los proyectos del docente que se pueden elegir
 * @param motivo por qué no se le puede asignar; vacío si se puede
 */
public record ActividadPosible(
        ActividadDelCatalogo actividad,
        List<ProyectoDelDocente> proyectos,
        Optional<String> motivo) {

    /** Exige los datos y copia la lista. */
    public ActividadPosible {
        Objects.requireNonNull(actividad, "actividad");
        Objects.requireNonNull(motivo, "motivo");
        proyectos = List.copyOf(proyectos);
    }

    /** Si se le puede asignar. */
    public boolean disponible() {
        return motivo.isEmpty();
    }
}
