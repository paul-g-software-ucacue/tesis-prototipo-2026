package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.Matricula;

/**
 * Caso de uso: registrar y corregir los matriculados por carrera y periodo (F01-H04, carga 10).
 * Entran solo por la carga; no hay pantalla ni operación propia en el contrato.
 */
public interface MantenerMatriculas {

    /** Registra los matriculados de una carrera en un periodo. */
    void registrar(Matricula matricula, String quien);

    /** Corrige el total, la fecha de corte y la fuente. */
    void corregir(Matricula matricula, String quien);
}
