package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.DatoInvalido;

/** Los proyectos y cargos de un docente, para asignarle actividades (F06-H04, ADR-023). */
public interface ConsultarProyectosYCargos {

    /**
     * Sus proyectos asignables en el periodo y sus cargos vigentes.
     *
     * @param identificacion el número de identificación del docente
     * @param periodo el código de un periodo de Académico
     * @throws DocenteNoEncontrado si no existe
     * @throws DatoInvalido en {@code periodo} si el periodo no existe
     */
    ProyectosYCargos delDocente(String identificacion, String periodo);
}
