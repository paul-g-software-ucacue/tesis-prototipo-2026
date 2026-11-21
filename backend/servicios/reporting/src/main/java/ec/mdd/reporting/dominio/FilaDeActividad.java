package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Una actividad no lectiva del docente en la versión.
 *
 * @param componente su componente; nunca {@link Componente#CLASE}
 * @param codigo el código de la actividad del catálogo
 * @param nombre su nombre
 * @param unidadOProyecto el proyecto o la carrera que la registró; {@code null} si no se sabe
 * @param horas sus horas semanales
 */
public record FilaDeActividad(
        Componente componente,
        String codigo,
        String nombre,
        String unidadOProyecto,
        BigDecimal horas) {

    /** Exige los datos, un componente que no es de clase y horas válidas. */
    public FilaDeActividad {
        Objects.requireNonNull(componente, "componente");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        if (componente == Componente.CLASE) {
            throw new IllegalArgumentException("una actividad no lectiva no es de clase");
        }
        horas = Horas.validas(horas);
    }

    /** La unidad o el proyecto; vacío si no se sabe. */
    public Optional<String> unidad() {
        return Optional.ofNullable(unidadOProyecto);
    }
}
