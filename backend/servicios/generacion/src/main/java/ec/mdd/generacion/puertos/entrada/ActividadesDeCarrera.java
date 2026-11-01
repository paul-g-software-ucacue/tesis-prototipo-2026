package ec.mdd.generacion.puertos.entrada;

import java.util.List;
import java.util.Objects;

/**
 * Las actividades del parcial de una carrera.
 *
 * @param carrera el código de la carrera
 * @param docentes sus docentes con actividades, por apellidos y nombres
 */
public record ActividadesDeCarrera(String carrera, List<ActividadesDelDocente> docentes) {

    /** Exige la carrera y copia la lista. */
    public ActividadesDeCarrera {
        Objects.requireNonNull(carrera, "carrera");
        docentes = List.copyOf(docentes);
    }
}
