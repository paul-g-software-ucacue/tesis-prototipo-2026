package ec.mdd.roles.dominio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Parte del MDD sobre la que se dan permisos, con los tipos que tienen sentido para ella (D-65).
 *
 * @param codigo identificador estable, por ejemplo {@code CARGAS}
 * @param nombre nombre en español, por ejemplo «Cargas de datos»
 * @param tipos los tipos de permiso que admite, en el orden de {@link TipoDePermiso}; al menos uno
 */
public record Funcionalidad(String codigo, String nombre, Set<TipoDePermiso> tipos) {

    /** Exige los tres datos y copia los tipos, conservando su orden. */
    public Funcionalidad {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        if (tipos == null || tipos.isEmpty()) {
            throw new IllegalArgumentException("La funcionalidad " + codigo + " no admite tipos");
        }
        tipos = Collections.unmodifiableSet(EnumSet.copyOf(tipos));
    }

    /** Si admite permisos de ese tipo. */
    public boolean admite(TipoDePermiso tipo) {
        return tipos.contains(tipo);
    }
}
