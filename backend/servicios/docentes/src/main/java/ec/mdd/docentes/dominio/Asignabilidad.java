package ec.mdd.docentes.dominio;

import java.util.Set;

/**
 * Si un proyecto es asignable en un periodo y, si no, por qué (RF-18, RN-25).
 *
 * @param asignable si puede recibir horas de investigación, vinculación o prácticas en el periodo
 * @param motivos por qué no; vacío si es asignable
 */
public record Asignabilidad(boolean asignable, Set<MotivoNoAsignable> motivos) {

    /** Copia los motivos; asignable si no hay ninguno. */
    public Asignabilidad {
        motivos = Set.copyOf(motivos);
    }
}
