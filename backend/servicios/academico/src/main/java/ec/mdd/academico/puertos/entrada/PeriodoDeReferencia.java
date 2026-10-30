package ec.mdd.academico.puertos.entrada;

import java.time.LocalDate;

/**
 * Un periodo académico con sus fechas, contra el que Docentes decide si un proyecto es asignable
 * (F03-H05), y si está abierto o cerrado, para el tablero de preparación de Cargas (F02-H02).
 *
 * @param codigo como {@code 2026-I}
 * @param nombre su nombre
 * @param inicio el primer día
 * @param fin el último día
 * @param abierto si tiene su versión de normativa (F02-H01, glosario «Periodo académico»)
 * @param cerrado si su estado es Cerrado
 */
public record PeriodoDeReferencia(
        String codigo,
        String nombre,
        LocalDate inicio,
        LocalDate fin,
        boolean abierto,
        boolean cerrado) {}
