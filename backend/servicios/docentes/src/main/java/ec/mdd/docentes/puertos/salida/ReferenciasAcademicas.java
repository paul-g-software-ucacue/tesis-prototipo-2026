package ec.mdd.docentes.puertos.salida;

import ec.mdd.docentes.dominio.Referencias;

/**
 * Las carreras, las asignaturas y los campos amplios de Académico, que Docentes valida y muestra
 * (FR-005, research §4). Su adaptador usa la API publicada de Académico dentro del proceso.
 */
public interface ReferenciasAcademicas {

    /** Las referencias vigentes en este momento. */
    Referencias referencias();
}
