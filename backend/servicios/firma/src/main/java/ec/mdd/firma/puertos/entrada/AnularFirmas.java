package ec.mdd.firma.puertos.entrada;

import java.time.Instant;

/**
 * Anular las firmas del documento de una versión que se reemplazó (F12-H04, D-106, glosario {@code
 * anularFirmas()}). Lo ordena Aprobación (MD-05): Firma no sabe por qué cambió la versión.
 */
@FunctionalInterface
public interface AnularFirmas {

    /**
     * Registra la anulación: las firmas, sus archivos y sus validaciones se conservan, y nadie
     * queda en turno. Publica {@link FirmasAnuladas} en la misma transacción. Es idempotente: si ya
     * se anularon, o la versión no tiene documento, no hace nada.
     *
     * @param periodo el código del periodo
     * @param version la versión cuyo documento se anula
     * @param motivo por qué
     * @param momento cuándo
     */
    void anular(String periodo, int version, MotivoDeAnulacion motivo, Instant momento);
}
