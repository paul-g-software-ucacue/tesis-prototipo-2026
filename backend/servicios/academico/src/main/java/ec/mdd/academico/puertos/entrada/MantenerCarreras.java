package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoDeMalla;
import ec.mdd.academico.dominio.Malla;

/**
 * Caso de uso: mantener las carreras, sus mallas y sus asignaturas (FR-003, FR-005, FR-009,
 * F03-H02). Cada cambio queda en el registro; nada se borra. Un dato inválido lanza {@link
 * DatoInvalido} con su campo.
 */
public interface MantenerCarreras {

    /**
     * @throws CarreraRepetida si ya hay una carrera con ese código
     * @throws SiglasRepetidas si otra carrera usa esas siglas
     */
    Carrera registrarCarrera(Carrera nueva, String quien);

    /**
     * @throws CarreraNoEncontrada si no hay una carrera con ese código
     * @throws SiglasRepetidas si otra carrera usa las siglas nuevas
     */
    Carrera corregirCarrera(String codigo, DatosDeCarrera datos, String quien);

    /**
     * @throws CarreraNoEncontrada si no hay una carrera con ese código
     */
    Carrera cambiarEstadoCarrera(String codigo, EstadoDeCarrera estado, String quien);

    /**
     * @throws CarreraNoEncontrada si no hay una carrera con ese código
     * @throws MallaRepetida si ya hay una malla con ese código de versión
     */
    Malla registrarMalla(String carrera, String codigo, String quien);

    /**
     * @throws MallaNoEncontrada si no hay una malla con ese código de versión
     */
    Malla cambiarEstadoMalla(String codigo, EstadoDeMalla estado, String quien);

    /**
     * @throws AsignaturaRepetida si ya hay una asignatura con ese código
     */
    Asignatura registrarAsignatura(Asignatura nueva, String quien);

    /**
     * @throws AsignaturaNoEncontrada si no hay una asignatura con ese código
     */
    Asignatura corregirAsignatura(String codigo, DatosDeAsignatura datos, String quien);

    /**
     * @throws AsignaturaNoEncontrada si no hay una asignatura con ese código
     */
    Asignatura cambiarEstadoAsignatura(String codigo, boolean activa, String quien);
}
