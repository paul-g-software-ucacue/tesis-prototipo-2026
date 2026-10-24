package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import java.util.List;

/**
 * Los datos de una carrera que se pueden corregir; el código y el estado no van aquí.
 *
 * @param nombre hasta 150 caracteres
 * @param siglas únicas, hasta 10
 * @param sede una sede, extensión o campus activo
 * @param nivelFormacion su nivel de formación
 * @param modalidad su modalidad
 * @param jornadas al menos una
 * @param campo su campo amplio y específico
 * @param campoDetallado el detallado, o {@code null}
 * @param numeroCiclos de 1 a 10
 */
public record DatosDeCarrera(
        String nombre,
        String siglas,
        String sede,
        NivelDeFormacion nivelFormacion,
        Modalidad modalidad,
        List<Jornada> jornadas,
        CampoConocimiento campo,
        String campoDetallado,
        int numeroCiclos) {}
