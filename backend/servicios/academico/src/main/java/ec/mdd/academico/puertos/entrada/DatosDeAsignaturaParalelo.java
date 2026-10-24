package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import java.math.BigDecimal;

/**
 * Los datos que se corrigen de una asignatura-paralelo; el periodo, la asignatura y el paralelo no
 * cambian.
 *
 * @param jornada una de las de su carrera
 * @param modalidad una del catálogo
 * @param sede la indicada, o {@code null} para la de la carrera
 * @param estudiantes los previstos
 * @param horasIndicadas las indicadas, o {@code null} para las de la asignatura
 */
public record DatosDeAsignaturaParalelo(
        Jornada jornada,
        Modalidad modalidad,
        String sede,
        int estudiantes,
        BigDecimal horasIndicadas) {}
