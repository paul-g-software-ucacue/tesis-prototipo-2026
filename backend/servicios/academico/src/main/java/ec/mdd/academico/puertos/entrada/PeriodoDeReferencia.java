package ec.mdd.academico.puertos.entrada;

import java.time.LocalDate;

/**
 * Un periodo académico con sus fechas, contra el que Docentes decide si un proyecto es asignable
 * (F03-H05).
 *
 * @param codigo como {@code 2026-I}
 * @param nombre su nombre
 * @param inicio el primer día
 * @param fin el último día
 */
public record PeriodoDeReferencia(String codigo, String nombre, LocalDate inicio, LocalDate fin) {}
