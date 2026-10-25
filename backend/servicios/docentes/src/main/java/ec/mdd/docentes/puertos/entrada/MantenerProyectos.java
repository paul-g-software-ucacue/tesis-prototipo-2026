package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Proyecto;

/**
 * Caso de uso: registrar y corregir los proyectos aprobados (F03-H05, RF-18, carga 9). Refleja
 * {@code registrarProyecto} y {@code corregirProyecto}. Ninguno se borra: cambia de estado.
 */
public interface MantenerProyectos {

    /**
     * Registra un proyecto.
     *
     * @param quien el usuario que lo hace
     * @throws DatoInvalido si una carrera no existe o el director o un participante no son docentes
     *     registrados
     * @throws ProyectoRepetido si ya existe un proyecto con ese código
     */
    ProyectoConsultado registrar(Proyecto proyecto, String quien);

    /**
     * Corrige un proyecto o cambia su estado; su código no cambia.
     *
     * @throws ProyectoNoEncontrado si no hay un proyecto con ese código
     * @throws DatoInvalido como al registrar
     */
    ProyectoConsultado corregir(Proyecto proyecto, String quien);
}
