package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.FirmanteEsperado;
import java.time.Instant;
import java.util.Objects;

/**
 * Lo que hace falta para notificar al siguiente firmante (F16-H02, D-110).
 *
 * @param periodo el periodo del documento
 * @param version la versión congelada
 * @param autor quien subió la firma aceptada
 * @param momento cuándo la aceptó el MDD
 * @param rotulo el rótulo del paso siguiente («Revisado por»)
 * @param firmante el firmante esperado del paso siguiente, por su usuario o por su identificación
 * @param rotuloAnterior el rótulo del paso recién firmado
 * @param nombreAnterior el nombre impreso de quien lo firmó
 */
public record FirmaPendienteSolicitada(
        String periodo,
        int version,
        String autor,
        Instant momento,
        String rotulo,
        FirmanteEsperado firmante,
        String rotuloAnterior,
        String nombreAnterior) {

    /** Exige los datos. */
    public FirmaPendienteSolicitada {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(rotulo, "rotulo");
        Objects.requireNonNull(firmante, "firmante");
        Objects.requireNonNull(rotuloAnterior, "rotuloAnterior");
        Objects.requireNonNull(nombreAnterior, "nombreAnterior");
    }
}
