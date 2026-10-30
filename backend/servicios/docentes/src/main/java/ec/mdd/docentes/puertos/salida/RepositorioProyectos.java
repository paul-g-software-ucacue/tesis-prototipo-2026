package ec.mdd.docentes.puertos.salida;

import ec.mdd.docentes.dominio.Proyecto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Los proyectos aprobados (F03-H05, RF-18, carga 9). Ninguno se borra: cambia de estado. */
public interface RepositorioProyectos {

    /** Todos los proyectos, por código. */
    List<Proyecto> todos();

    /** Si hay al menos un proyecto, en cualquier estado (F01-H05). */
    boolean hayProyectos();

    /** El proyecto con ese código, si existe. */
    Optional<Proyecto> buscar(String codigo);

    /** Bloquea los proyectos hasta que termine la transacción. */
    void bloquear();

    /** Guarda un proyecto nuevo, con quién y cuándo lo registró. */
    void registrar(Proyecto proyecto, String quien, Instant momento);

    /** Guarda los datos de un proyecto que ya existe; su código no cambia. */
    void guardar(Proyecto proyecto);
}
