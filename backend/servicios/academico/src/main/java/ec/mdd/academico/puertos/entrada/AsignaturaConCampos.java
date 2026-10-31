package ec.mdd.academico.puertos.entrada;

/**
 * Lo que otro proceso necesita de una asignatura ofertada para calcular la afinidad (ADR-023).
 *
 * @param codigo su código
 * @param nombre su nombre
 * @param campoAmplio su campo amplio
 * @param campoEspecifico su campo específico
 */
public record AsignaturaConCampos(
        String codigo, String nombre, String campoAmplio, String campoEspecifico) {}
