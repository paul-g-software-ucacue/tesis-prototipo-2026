package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.entrada.FirmasAnuladas;
import ec.mdd.firma.puertos.salida.AvisoDeFirmasAnuladas;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el aviso de firmas anuladas como evento de Spring (F12-H04, D-106), como el de rechazo en
 * firma. Hoy no lo escucha nadie; lo escucharán las notificaciones de F16.
 */
public final class AvisoDeFirmasAnuladasSpring implements AvisoDeFirmasAnuladas {

    private final ApplicationEventPublisher eventos;

    /** Recibe el publicador de Spring. */
    public AvisoDeFirmasAnuladasSpring(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    public void avisar(FirmasAnuladas aviso) {
        eventos.publishEvent(aviso);
    }
}
