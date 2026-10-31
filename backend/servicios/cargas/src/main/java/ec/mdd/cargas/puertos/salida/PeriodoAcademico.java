package ec.mdd.cargas.puertos.salida;

import java.time.LocalDate;

/**
 * Un periodo académico, como lo ve el tablero de preparación (F02-H02, glosario «Periodo
 * académico»).
 *
 * @param codigo como {@code 2026-I}
 * @param nombre su nombre
 * @param inicio su primer día, para ordenar y elegir el más reciente
 * @param abierto si tiene su versión de normativa (F02-H01)
 * @param cerrado si está cerrado
 */
public record PeriodoAcademico(
        String codigo, String nombre, LocalDate inicio, boolean abierto, boolean cerrado) {}
