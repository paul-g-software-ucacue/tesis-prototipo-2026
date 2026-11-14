package ec.mdd.firma.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Los cambios de Firma para la bitácora (F17-H01, F13-H02, ADR-018). */
@FunctionalInterface
public interface RegistroDeCambios {

    /** Publica el cambio en la transacción en curso. */
    void registrar(CambioDeFirma cambio);

    /**
     * Un cambio de Firma.
     *
     * @param momento cuándo
     * @param usuario quién: {@code sistema} o quien pidió el reintento
     * @param accion qué
     * @param detalle sobre qué: el periodo, la versión y los hashes
     * @param nuevo cómo queda
     */
    record CambioDeFirma(
            Instant momento,
            String usuario,
            String accion,
            Map<String, Object> detalle,
            Map<String, Object> nuevo) {

        /** El documento firmable generado (F13-H02). */
        public static final String DOCUMENTO_FIRMABLE_GENERADO = "DOCUMENTO_FIRMABLE_GENERADO";

        /** Esqueleto (F14-H02, T010). */
        public static final String FIRMA_ACEPTADA = "";

        /** Esqueleto (F14-H02, T010). */
        public static final String FIRMA_RECHAZADA = "";

        /** Exige todo y copia los mapas. */
        public CambioDeFirma {
            Objects.requireNonNull(momento, "momento");
            Objects.requireNonNull(usuario, "usuario");
            Objects.requireNonNull(accion, "accion");
            detalle = Map.copyOf(detalle);
            nuevo = Map.copyOf(nuevo);
        }
    }
}
