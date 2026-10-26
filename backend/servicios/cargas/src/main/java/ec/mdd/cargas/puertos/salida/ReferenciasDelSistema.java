package ec.mdd.cargas.puertos.salida;

import ec.mdd.cargas.dominio.ReferenciasExistentes;

/**
 * Lo que ya existe en el sistema, para comprobar las referencias de las filas (F01-H03, RF-08,
 * aclaración 2): las unidades, las carreras, los periodos y las asignaturas de Académico, y las
 * identificaciones de los docentes de Docentes.
 */
public interface ReferenciasDelSistema {

    /** Los códigos que existen ahora; se piden una vez por archivo. */
    ReferenciasExistentes existentes();
}
