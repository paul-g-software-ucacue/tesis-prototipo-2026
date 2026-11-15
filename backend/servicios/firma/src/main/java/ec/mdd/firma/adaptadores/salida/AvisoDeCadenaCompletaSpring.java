package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;
import ec.mdd.firma.puertos.salida.AvisoDeCadenaCompleta;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el aviso de cadena completa como evento de Spring (F14-H03, D-104). Spring Modulith lo
 * guarda en el registro de publicaciones en la transacción en curso y lo entrega a los oyentes de
 * Aprobación después de confirmarla, con reintento (ADR-018).
 */
public final class AvisoDeCadenaCompletaSpring implements AvisoDeCadenaCompleta {

    private final ApplicationEventPublisher eventos;

    /** Recibe el publicador de Spring. */
    public AvisoDeCadenaCompletaSpring(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    public void avisar(CadenaDeFirmasCompleta aviso) {
        eventos.publishEvent(aviso);
    }
}
