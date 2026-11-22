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
 * @param codigoDeCarrera el código de la carrera del parcial que la registró, para el CSV para SIGA
 *     (D-119); {@code null} en los reportes
 * @param codigoDeProyecto el código de su proyecto; {@code null} si no tiene o en los reportes
 */
public record FilaDeActividad(
        Componente componente,
        String codigo,
        String nombre,
        String unidadOProyecto,
        BigDecimal horas,
        String codigoDeCarrera,
        String codigoDeProyecto) {

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

    /** Una actividad de los reportes, sin los códigos de la carrera ni del proyecto. */
    public FilaDeActividad(
            Componente componente,
            String codigo,
            String nombre,
            String unidadOProyecto,
            BigDecimal horas) {
        this(componente, codigo, nombre, unidadOProyecto, horas, null, null);
    }

    /** El código de la carrera; vacío si no se leyó. */
    public Optional<String> codigoCarrera() {
        return Optional.ofNullable(codigoDeCarrera);
    }

    /** El código del proyecto; vacío si no tiene. */
    public Optional<String> codigoProyecto() {
        return Optional.ofNullable(codigoDeProyecto);
    }

    /** La unidad o el proyecto; vacío si no se sabe. */
    public Optional<String> unidad() {
        return Optional.ofNullable(unidadOProyecto);
    }
}
