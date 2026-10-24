package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.Identificacion;

/**
 * Mantener los docentes: registrarlos, corregir sus datos y su ficha académica y cambiar su estado
 * laboral, con su registro (F03-H04, RF-16, RF-17, principio VII). Ninguno se borra.
 */
public interface MantenerDocentes {

    /**
     * Registra un docente con su ficha; queda activo.
     *
     * @param quien el usuario que lo hace
     * @throws DocenteRepetido si ya existe un docente con esa identificación
     * @throws CorreoRepetido si otro docente usa ese correo
     */
    Docente registrar(Identificacion identificacion, DatosDelDocente datos, String quien);

    /**
     * Corrige los datos y la ficha; la identificación y el estado no cambian.
     *
     * @throws DocenteNoEncontrado si no hay un docente con esa identificación
     * @throws CorreoRepetido si otro docente usa ese correo
     */
    Docente corregir(String identificacion, DatosDelDocente datos, String quien);

    /**
     * Pasa al docente a activo, en licencia o inactivo.
     *
     * @throws DocenteNoEncontrado si no hay un docente con esa identificación
     */
    Docente cambiarEstado(String identificacion, EstadoLaboral estado, String quien);
}
