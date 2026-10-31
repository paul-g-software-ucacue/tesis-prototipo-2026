package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.FichaAcademica;

/** La formación de un docente, para calcular la afinidad (F06-H01, ADR-023). */
public interface ConsultarFormacion {

    /**
     * La ficha académica del docente.
     *
     * @param identificacion su número de identificación
     * @throws DocenteNoEncontrado si no existe
     */
    FichaAcademica formacion(String identificacion);
}
