package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Aprobación escucha que la cadena de firmas quedó completa (F14-H03, D-104, MD-05).
 *
 * <p>Firma publica el aviso en su API y no conoce el flujo; este oyente decide qué pasa con el
 * distributivo. Con {@link ApplicationModuleListener} corre después de confirmar la firma, en otro
 * hilo y en una transacción nueva; si falla, el registro de publicaciones lo reintenta (ADR-018), y
 * completar es idempotente.
 */
public class CierreDeLaFirma {

    private final CompletarFirmas completar;

    /** Recibe el caso de uso de Aprobación. */
    public CierreDeLaFirma(CompletarFirmas completar) {
        this.completar = completar;
    }

    /**
     * @param aviso el periodo y la versión firmados
     */
    @ApplicationModuleListener
    public void alCompletarseLaCadena(CadenaDeFirmasCompleta aviso) {
        completar.completar(aviso.periodo(), aviso.version(), aviso.momento());
    }
}
