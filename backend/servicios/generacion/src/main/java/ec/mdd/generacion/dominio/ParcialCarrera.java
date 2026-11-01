package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * La parte del distributivo que arma una carrera (RF-24, D-19).
 *
 * @param carrera el código de su carrera
 * @param estado su estado
 */
public record ParcialCarrera(String carrera, EstadoDelParcial estado) {

    /** Exige los datos. */
    public ParcialCarrera {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(estado, "estado");
    }

    /** Un parcial que recién empieza. */
    public static ParcialCarrera nuevo(String carrera) {
        return new ParcialCarrera(carrera, EstadoDelParcial.EN_ELABORACION);
    }

    /** Si admite cambios: uno enviado no se edita hasta que se devuelve. */
    public boolean editable() {
        return estado != EstadoDelParcial.ENVIADO;
    }
}
