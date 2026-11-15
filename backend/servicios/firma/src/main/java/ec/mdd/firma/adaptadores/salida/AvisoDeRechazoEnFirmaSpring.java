package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.entrada.RechazoEnFirma;
import ec.mdd.firma.puertos.salida.AvisoDeRechazoEnFirma;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el aviso de rechazo en firma como evento de Spring (F14-H04, D-105), como el de cadena
 * completa: Spring Modulith lo guarda en el registro de publicaciones en la transacción en curso y
 * lo entrega a los oyentes de Aprobación después de confirmarla, con reintento (ADR-018).
 */
public final class AvisoDeRechazoEnFirmaSpring implements AvisoDeRechazoEnFirma {

    private final ApplicationEventPublisher eventos;

    /** Recibe el publicador de Spring. */
    public AvisoDeRechazoEnFirmaSpring(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    public void avisar(RechazoEnFirma aviso) {
        eventos.publishEvent(aviso);
    }
}
