package ec.mdd.validacionocs.puertos.salida;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Un cambio de la configuración del flujo, con quién lo hizo y cuándo, para la bitácora (F17-H01).
 * Solo se publica: la fila de la carga ya es el registro local de Validación OCS.
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué: el periodo
 * @param anterior la versión que había, vacío si es la primera carga del periodo
 * @param nuevo la versión que queda vigente
 */
public record CambioDeFlujo(
        Instant momento,
        String usuario,
        Accion accion,
        Map<String, Object> detalle,
        Map<String, Object> anterior,
        Map<String, Object> nuevo) {

    /**
     * Exige todo, copia los mapas y rechaza un cambio sin {@code nuevo}: la carga siempre deja una
     * versión vigente.
     *
     * @throws IllegalArgumentException si falta {@code nuevo}
     */
    public CambioDeFlujo {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
        anterior = Map.copyOf(anterior);
        nuevo = Map.copyOf(nuevo);
        sinSecretos(detalle);
        sinSecretos(anterior);
        sinSecretos(nuevo);
        if (nuevo.isEmpty()) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase: crea o cambia, con nuevo.");
        }
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
            lista.forEach(CambioDeFlujo::sinSecretos);
        }
    }

    /** Las acciones que se registran. */
    public enum Accion {
        /**
         * Configuración del flujo cargada para un periodo. Crea la primera vez y cambia después:
         * lleva {@code nuevo}, y {@code anterior} si había una (research §2).
         */
        CONFIGURACION_FLUJO_CARGADA,
        /**
         * El parcial de una carrera enviado (F12-H02): cambia, con el estado del parcial antes y
         * después, y la versión y el hash en el detalle (contracts/bitacora.md, D-95).
         */
        PARCIAL_ENVIADO,
        /** El distributivo pasó a otra etapa (F12-H02): cambia, con la etapa antes y después. */
        ETAPA_AVANZADA,
        /**
         * El consolidado enviado a revisión (F10-H03): el paso desde EN_CONSOLIDACION, con la etapa
         * antes y después, y la versión, el hash y la huella del informe en el detalle
         * (contracts/bitacora.md, D-95, D-97).
         */
        CONSOLIDADO_ENVIADO_A_REVISION,
        /**
         * Parciales devueltos desde EN_CONSOLIDACION (F12-H05): la etapa antes y después, y la
         * versión, el hash y la observación de cada carrera devuelta en el detalle
         * (contracts/bitacora.md, D-95, D-98). Una entrada aunque devuelva varias carreras.
         */
        DISTRIBUTIVO_DEVUELTO
    }
}
