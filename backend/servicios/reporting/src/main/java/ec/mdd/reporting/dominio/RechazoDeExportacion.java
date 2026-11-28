package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Un CSV para SIGA que no se exportó porque la versión no tiene todas sus firmas o tiene
 * inconsistencias abiertas (F19-H02, D-120), para la bitácora. En el flujo normal no debería pasar:
 * por eso se anota, como señal de integridad (aclaración del 2026-11-22). No lleva hashes, nombres
 * ni identificaciones (principio IX).
 *
 * @param usuario quién lo pidió
 * @param momento cuándo
 * @param periodo el código del periodo
 * @param version la versión publicada
 * @param condiciones las condiciones, con al menos un motivo
 */
public record RechazoDeExportacion(
        String usuario,
        Instant momento,
        String periodo,
        int version,
        CondicionesDeExportacion condiciones) {

    /** Exige los datos y al menos un motivo. */
    public RechazoDeExportacion {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(condiciones, "condiciones");
        if (condiciones.motivos().isEmpty()) {
            throw new IllegalArgumentException("Un rechazo lleva al menos un motivo");
        }
    }

    /**
     * @param rechazo el rechazo del caso de uso
     * @param quien quién pidió el CSV
     * @param momento cuándo
     * @return el rechazo para la bitácora
     */
    public static RechazoDeExportacion de(
            ExportacionRechazada rechazo, String quien, Instant momento) {
        return new RechazoDeExportacion(
                quien, momento, rechazo.periodo(), rechazo.version(), rechazo.condiciones());
    }
}
