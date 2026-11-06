package ec.mdd.validacionocs.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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
        if (nuevo.isEmpty()) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase: crea o cambia, con nuevo.");
        }
    }

    /** Las acciones que se registran. */
    public enum Accion {
        /**
         * Configuración del flujo cargada para un periodo. Crea la primera vez y cambia después:
         * lleva {@code nuevo}, y {@code anterior} si había una (research §2).
         */
        CONFIGURACION_FLUJO_CARGADA
    }
}
