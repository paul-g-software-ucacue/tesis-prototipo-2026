package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el aviso de entrada en firma con el {@link ApplicationEventPublisher} del contexto
 * (F13-H02, D-101). El registro de Spring Modulith lo guarda en la transacción en curso (ADR-018):
 * si la transacción del paso no confirma, no sale.
 */
public final class AvisoDeFirmaSpring implements AvisoDeFirma {

    private final ApplicationEventPublisher eventos;

    /** Recibe el publicador de Spring. */
    public AvisoDeFirmaSpring(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    public void avisar(DistributivoEnFirma aviso) {
        eventos.publishEvent(aviso);
    }
}
