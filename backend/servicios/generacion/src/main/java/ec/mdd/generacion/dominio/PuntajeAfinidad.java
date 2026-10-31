package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * El puntaje de afinidad de un docente para una asignatura, con su desglose (RF-17, RN-24). Se
 * calcula en cada consulta; no se guarda.
 *
 * @param codigo el código de la asignatura
 * @param nombre el nombre de la asignatura
 * @param total la suma de los aportes; 0 si nada coincide
 * @param topeAplicado si los cursos relacionados pasaban del tope
 * @param desglose los aportes: tercer nivel, cuarto nivel, experiencia y cursos, en ese orden
 */
public record PuntajeAfinidad(
        String codigo,
        String nombre,
        BigDecimal total,
        boolean topeAplicado,
        List<AporteDeAfinidad> desglose) {

    /** Exige los datos y copia el desglose. */
    public PuntajeAfinidad {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(total, "total");
        desglose = List.copyOf(desglose);
    }
}
