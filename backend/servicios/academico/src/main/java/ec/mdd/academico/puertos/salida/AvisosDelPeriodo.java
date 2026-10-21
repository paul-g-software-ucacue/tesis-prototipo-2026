package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.PeriodoAcademico;

/**
 * Avisa a otros procesos que un periodo se registró o cambió de estado (F05-H03, ADR-018): Roles
 * guarda una copia de cada periodo para designar a su Responsable de consolidación y saber cuándo
 * se cierra. Se llama dentro de la transacción del cambio: el aviso sale solo si se confirma.
 */
public interface AvisosDelPeriodo {

    /** El periodo, con su estado nuevo. */
    void actualizado(PeriodoAcademico periodo);
}
