package ec.mdd.cargas.puertos.salida;

import ec.mdd.cargas.dominio.ReferenciasExistentes;
import java.util.List;

/**
 * Lo que ya existe en el sistema, para comprobar las referencias de las filas (F01-H03, RF-08,
 * aclaración 2): las unidades, las carreras, los periodos y las asignaturas de Académico, y las
 * identificaciones de los docentes de Docentes; y los periodos, para el tablero de preparación
 * (F02-H02).
 */
public interface ReferenciasDelSistema {

    /** Los códigos que existen ahora; se piden una vez por archivo. */
    ReferenciasExistentes existentes();

    /** Los periodos académicos, del que empieza más tarde al más antiguo (F02-H02). */
    List<PeriodoAcademico> periodos();
}
