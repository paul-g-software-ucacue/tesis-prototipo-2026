package ec.mdd.docentes.puertos.salida;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Un cambio de un docente, con quién lo hizo y cuándo, para la bitácora (FR-010, principio VII).
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué: la identificación del docente y lo que cambió
 * @param anterior los valores antes del cambio; vacío al registrar. Un valor puede ser {@code
 *     null}: el campo no tenía valor
 * @param nuevo los valores después del cambio. Un valor puede ser {@code null}
 */
public record CambioRegistrado(
        Instant momento,
        String usuario,
        Accion accion,
        Map<String, Object> detalle,
        Map<String, Object> anterior,
        Map<String, Object> nuevo) {

    /**
     * Exige todo, copia los mapas y rechaza un cambio que no cumple la clase de su acción.
     *
     * @throws IllegalArgumentException si {@code anterior} o {@code nuevo} no son los de la clase
     */
    public CambioRegistrado {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
        anterior = copia(anterior);
        nuevo = copia(nuevo);
        sinSecretos(detalle);
        sinSecretos(anterior);
        sinSecretos(nuevo);
        boolean cumple =
                switch (accion.clase()) {
                    case CREA -> anterior.isEmpty() && !nuevo.isEmpty();
                    case CAMBIA -> !anterior.isEmpty() && !nuevo.isEmpty();
                };
        if (!cumple) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase " + accion.clase() + ".");
        }
    }

    // Admite valores nulos, que Map.copyOf no admite: «antes no tenía teléfono».
    private static Map<String, Object> copia(Map<String, Object> valores) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(valores));
    }

    private static final List<String> PARECEN_SECRETOS =
            List.of("clave", "contrasena", "contraseña", "password", "token", "secreto", "jwt");

    /** Las claves que parecen un secreto y no lo son, escritas tal cual (D-92). */
    private static final Set<String> NO_SON_SECRETOS = Set.of("palabrasClave");

    // FR-011: ninguna clave, a cualquier profundidad, parece un secreto. El mensaje nombra la
    // clave, nunca su valor.
    private static void sinSecretos(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            mapa.forEach(
                    (clave, adentro) -> {
                        String texto = String.valueOf(clave);
                        String minusculas = texto.toLowerCase(Locale.ROOT);
                        if (!NO_SON_SECRETOS.contains(texto)
                                && PARECEN_SECRETOS.stream().anyMatch(minusculas::contains)) {
                            throw new IllegalArgumentException(
                                    "Una clave del cambio parece un secreto: " + texto);
                        }
                        sinSecretos(adentro);
                    });
        } else if (valor instanceof Collection<?> lista) {
            lista.forEach(CambioRegistrado::sinSecretos);
        }
    }

    /** Qué valores lleva un cambio según su acción (research §2). En Docentes nada se borra. */
    public enum Clase {
        /** Solo {@code nuevo}. */
        CREA,
        /** {@code anterior} y {@code nuevo}. */
        CAMBIA
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Docente registrado, con su identificación, su carrera base y su dedicación. */
        DOCENTE_REGISTRADO(Clase.CREA),
        /** Datos o ficha corregidos, con cada valor anterior y nuevo. */
        DOCENTE_CORREGIDO(Clase.CAMBIA),
        /** Estado laboral cambiado, con el anterior y el nuevo. */
        DOCENTE_ESTADO_CAMBIADO(Clase.CAMBIA),
        /** Cargo registrado, con su clave, su unidad o su carrera (F03-H05). */
        CARGO_REGISTRADO(Clase.CREA),
        /** Cargo corregido o finalizado, con cada valor anterior y nuevo. */
        CARGO_CORREGIDO(Clase.CAMBIA),
        /** Proyecto registrado, con su código, su tipo y su estado. */
        PROYECTO_REGISTRADO(Clase.CREA),
        /** Proyecto corregido o con otro estado, con cada valor anterior y nuevo. */
        PROYECTO_CORREGIDO(Clase.CAMBIA);

        private final Clase clase;

        Accion(Clase clase) {
            this.clase = clase;
        }

        /** Qué valores lleva un cambio de esta acción. */
        public Clase clase() {
            return clase;
        }
    }
}
