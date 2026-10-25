package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.Asignabilidad;
import ec.mdd.docentes.dominio.Proyecto;
import java.util.Map;
import java.util.Optional;

/**
 * Un proyecto con los nombres de sus docentes y, si se pidió un periodo, si es asignable en él
 * (F03-H05, FR-009).
 *
 * @param proyecto el proyecto
 * @param nombresDeDocentes el nombre completo del director y de cada participante, por
 *     identificación
 * @param asignabilidad si es asignable en el periodo pedido; vacío sin periodo
 */
public record ProyectoConsultado(
        Proyecto proyecto,
        Map<String, String> nombresDeDocentes,
        Optional<Asignabilidad> asignabilidad) {

    /** Copia el mapa. */
    public ProyectoConsultado {
        nombresDeDocentes = Map.copyOf(nombresDeDocentes);
    }
}
