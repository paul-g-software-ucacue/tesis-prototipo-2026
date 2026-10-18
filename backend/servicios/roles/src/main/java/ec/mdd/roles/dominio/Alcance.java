package ec.mdd.roles.dominio;

/**
 * Sobre qué datos vale un permiso: todos, o solo los propios del usuario (su carrera, unidad o
 * persona). Se registra desde F05-H02; cada historia que trae esos datos aplica el filtro (D-64).
 */
public enum Alcance {
    /** Todos los datos de la funcionalidad. */
    TODO,
    /** Solo los de la carrera, la unidad o la persona del usuario. */
    PROPIO;

    /** El más amplio de los dos: si uno es {@link #TODO}, {@link #TODO} (FR-005). */
    public Alcance masAmplio(Alcance otro) {
        return this == TODO || otro == TODO ? TODO : PROPIO;
    }
}
