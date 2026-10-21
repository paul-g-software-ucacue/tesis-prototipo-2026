package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.PeriodoAcademico;
import java.util.OptionalInt;

/**
 * Un periodo académico con la versión de normativa que lo rige, si ya la tiene (FR-009).
 *
 * @param periodo el periodo
 * @param version el número de la versión; vacío si no tiene
 */
public record PeriodoConsultado(PeriodoAcademico periodo, OptionalInt version) {}
