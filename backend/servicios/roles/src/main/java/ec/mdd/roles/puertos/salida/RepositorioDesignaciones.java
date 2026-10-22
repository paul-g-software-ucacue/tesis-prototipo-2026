package ec.mdd.roles.puertos.salida;

import ec.mdd.roles.dominio.Designacion;
import java.util.List;
import java.util.Optional;

/** Las designaciones del Responsable de consolidación, una por periodo (F05-H03). */
public interface RepositorioDesignaciones {

    /** Todas, por código de periodo. */
    List<Designacion> todas();

    /** La designación del periodo, si tiene. */
    Optional<Designacion> dePeriodo(String periodo);

    /** Guarda la designación del periodo, o reemplaza la anterior. */
    void guardar(Designacion designacion);

    /** Los códigos de los periodos no cerrados que el usuario tiene designados, ordenados. */
    List<String> periodosAbiertosDe(String usuario);
}
