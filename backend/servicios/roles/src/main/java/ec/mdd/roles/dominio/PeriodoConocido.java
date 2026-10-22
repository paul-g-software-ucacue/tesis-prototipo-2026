package ec.mdd.roles.dominio;

import java.util.Objects;

/**
 * Un periodo académico tal como lo conoce Roles: su código y su estado, copiados de los eventos de
 * Académico (ADR-018). Roles no lee las tablas de Académico (principio V).
 *
 * @param codigo el código del periodo
 * @param estado su estado más avanzado conocido
 */
public record PeriodoConocido(String codigo, EstadoConocido estado) {

    /** Exige los dos datos. */
    public PeriodoConocido {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(estado, "estado");
    }

    /** La copia con el estado de un evento; un evento viejo no la hace retroceder. */
    public PeriodoConocido conEstado(EstadoConocido otro) {
        return new PeriodoConocido(codigo, estado.masAvanzado(otro));
    }
}
