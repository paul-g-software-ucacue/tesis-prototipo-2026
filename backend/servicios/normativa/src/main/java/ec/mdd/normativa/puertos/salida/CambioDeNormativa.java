package ec.mdd.normativa.puertos.salida;

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
 * Un cambio de la normativa, con quién lo hizo y cuándo, para la bitácora (FR-009, principio VII).
 *
 * @param momento cuándo
 * @param usuario quién; {@code sistema} para la versión 1
 * @param accion qué
 * @param detalle sobre qué: el número de la versión y lo que cambió
 * @param anterior los valores antes del cambio; vacío al crear. Un valor puede ser {@code null}: el
 *     campo no tenía valor
 * @param nuevo los valores después del cambio. Un valor puede ser {@code null}
 */
public record CambioDeNormativa(
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
    public CambioDeNormativa {
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
                    case CREA_O_CAMBIA -> !nuevo.isEmpty();
                };
        if (!cumple) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase " + accion.clase() + ".");
        }
    }

    // Admite valores nulos, que Map.copyOf no admite: «antes no tenía ese parámetro».
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
            lista.forEach(CambioDeNormativa::sinSecretos);
        }
    }

    /** Qué valores lleva un cambio según su acción (research §2). Una versión no se borra. */
    public enum Clase {
        /** Solo {@code nuevo}. */
        CREA,
        /** {@code anterior} y {@code nuevo}. */
        CAMBIA,
        /** Crea la primera vez y cambia después: {@code nuevo}, y {@code anterior} si lo había. */
        CREA_O_CAMBIA
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Versión creada a partir de otra. */
        VERSION_CREADA(Clase.CREA),
        /** Versión editada, con cada valor anterior y nuevo. */
        VERSION_EDITADA(Clase.CAMBIA),
        /** Versión asociada a un periodo, con la que tenía antes si la había. */
        VERSION_ASOCIADA(Clase.CREA_O_CAMBIA);

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
