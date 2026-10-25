package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import java.util.List;

/**
 * Caso de uso: consultar los proyectos aprobados y si son asignables en un periodo (F03-H05,
 * RF-18). Refleja {@code listarProyectos} y {@code consultarProyecto}.
 */
public interface ConsultarProyectos {

    /**
     * Los proyectos por código.
     *
     * @param periodo el código de un periodo de Académico, para decir si cada uno es asignable; o
     *     {@code null}
     * @param tipo solo los de ese tipo; o {@code null}
     * @param estado solo los de ese estado; o {@code null}
     * @throws DatoInvalido en {@code periodo} si el periodo no existe
     */
    List<ProyectoConsultado> listar(String periodo, TipoDeProyecto tipo, EstadoDeProyecto estado);

    /**
     * Un proyecto.
     *
     * @param periodo como en {@link #listar}
     * @throws ProyectoNoEncontrado si no hay un proyecto con ese código
     * @throws DatoInvalido en {@code periodo} si el periodo no existe
     */
    ProyectoConsultado proyecto(String codigo, String periodo);
}
