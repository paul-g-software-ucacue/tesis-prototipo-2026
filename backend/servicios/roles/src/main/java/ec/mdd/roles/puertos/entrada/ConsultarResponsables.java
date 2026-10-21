package ec.mdd.roles.puertos.entrada;

import ec.mdd.roles.dominio.Designacion;
import java.util.List;

/**
 * Caso de uso: consultar los Responsables de consolidación y a quién se puede designar (F05-H03).
 * Refleja las operaciones {@code listarResponsables} y {@code listarDocentes} del contrato de
 * Roles. Las historias de consolidación y de firma (F09, F10) lo usan para limitar cada operación a
 * los periodos del Responsable (D-64).
 */
public interface ConsultarResponsables {

    /** Las designaciones, por código de periodo. */
    List<Designacion> responsables();

    /** Los nombres de usuario con el rol Docente, ordenados: los que se pueden designar. */
    List<String> docentes();
}
