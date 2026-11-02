package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Evalúa una asignación al guardarla, comparando la carga del docente antes y después (RF-28). Una
 * regla se viola solo si el docente queda sobre el límite y además sube: así se puede llegar justo
 * al límite, y quien ya estaba excedido puede bajar o repetir, pero no subir. Es una función pura.
 *
 * <p>Las cifras son datos de la normativa; qué tipos de proyecto activan el tope de clase es parte
 * de la comprobación (principio VI).
 */
public final class MotorReglas {

    private static final Set<String> TIPOS_CON_TOPE = Set.of("INVESTIGACION", "VINCULACION");

    private MotorReglas() {}

    /**
     * Evalúa una asignación. Si viola las dos reglas, devuelve la de las horas.
     *
     * @param antes la carga del docente sin la asignación
     * @param despues su carga con la asignación
     * @param horasContratadas sus horas contratadas
     * @param conProyecto si tiene el tope de clase
     * @param reglas las cifras de la normativa
     * @return la regla violada; vacío si se puede guardar
     */
    public static Optional<Inconsistencia> evaluar(
            CargaHoraria antes,
            CargaHoraria despues,
            int horasContratadas,
            boolean conProyecto,
            ReglasDelPeriodo reglas) {
        BigDecimal limite = reglas.limite(horasContratadas);
        if (sePasaYSube(antes.asignadas(), despues.asignadas(), limite)) {
            return Optional.of(
                    new Inconsistencia(
                            ReglaDeAsignacion.RN_02, queQueda(limite, antes.asignadas())));
        }
        BigDecimal tope = reglas.horasClaseConProyecto();
        if (conProyecto && sePasaYSube(antes.clase(), despues.clase(), tope)) {
            return Optional.of(
                    new Inconsistencia(ReglaDeAsignacion.RN_13, queQueda(tope, antes.clase())));
        }
        return Optional.empty();
    }

    /** Si alguno de sus proyectos asignables activa el tope de clase (RN-13). */
    public static boolean conProyecto(Collection<String> tipos) {
        return tipos.stream().anyMatch(TIPOS_CON_TOPE::contains);
    }

    private static boolean sePasaYSube(BigDecimal antes, BigDecimal despues, BigDecimal limite) {
        return despues.compareTo(limite) > 0 && despues.compareTo(antes) > 0;
    }

    private static BigDecimal queQueda(BigDecimal limite, BigDecimal tiene) {
        return limite.subtract(tiene).max(BigDecimal.ZERO);
    }
}
