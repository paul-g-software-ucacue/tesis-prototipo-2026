package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import java.math.BigDecimal;

/**
 * Lo que otro proceso necesita de una asignatura-paralelo para sugerir docentes (ADR-023).
 *
 * @param asignatura el código de su asignatura
 * @param nombre el nombre de su asignatura
 * @param carrera el código de su carrera
 * @param paralelo su paralelo
 * @param campoAmplio el campo amplio de su asignatura
 * @param campoEspecifico el campo específico de su asignatura
 * @param troncoComun si su asignatura es de tronco común
 * @param horasSemanales sus horas efectivas
 * @param estado ofertada o retirada
 */
public record ParaleloConCampos(
        String asignatura,
        String nombre,
        String carrera,
        String paralelo,
        String campoAmplio,
        String campoEspecifico,
        boolean troncoComun,
        BigDecimal horasSemanales,
        EstadoDeAsignaturaParalelo estado) {}
