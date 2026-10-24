package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import java.math.BigDecimal;
import java.util.List;

/**
 * Los datos de una asignatura que se pueden corregir; el código, la carrera, la malla y el estado
 * no van aquí.
 *
 * @param nombre hasta 150 caracteres
 * @param ciclo de 1 a 10
 * @param unidadOrganizacionCurricular su unidad de organización curricular
 * @param horasSemanalesClase de 0,5 a 20
 * @param horasPracticasSemanales de 0 a 20, o {@code null}
 * @param creditos mayores que 0, o {@code null}
 * @param troncoComun si es de tronco común
 * @param campo su campo amplio y específico
 * @param palabrasClave al menos dos
 */
public record DatosDeAsignatura(
        String nombre,
        int ciclo,
        UnidadDeOrganizacionCurricular unidadOrganizacionCurricular,
        BigDecimal horasSemanalesClase,
        BigDecimal horasPracticasSemanales,
        BigDecimal creditos,
        boolean troncoComun,
        CampoConocimiento campo,
        List<String> palabrasClave) {}
