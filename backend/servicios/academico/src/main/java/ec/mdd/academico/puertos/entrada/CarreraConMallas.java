package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Malla;
import java.math.BigDecimal;
import java.util.List;

/**
 * Una carrera con el resumen de cada una de sus mallas, por código.
 *
 * @param carrera la carrera
 * @param mallas sus mallas, con sus asignaturas activas y sus horas
 */
public record CarreraConMallas(Carrera carrera, List<Resumen> mallas) {

    /** Copia la lista. */
    public CarreraConMallas {
        mallas = List.copyOf(mallas);
    }

    /**
     * Una malla con cuántas asignaturas activas tiene y la suma de sus horas semanales de clase,
     * que la oferta (F03-H03) usa para RN-18.
     *
     * @param malla la malla
     * @param asignaturas sus asignaturas activas
     * @param horas la suma de sus horas semanales de clase
     */
    public record Resumen(Malla malla, int asignaturas, BigDecimal horas) {}
}
