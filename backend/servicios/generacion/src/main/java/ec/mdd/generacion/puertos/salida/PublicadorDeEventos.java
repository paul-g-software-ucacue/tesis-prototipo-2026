package ec.mdd.generacion.puertos.salida;

/**
 * Publica un evento hacia otro proceso dentro de la transacción en curso (principio V, ADR-018). La
 * entrega ocurre después de confirmar la transacción; si se revierte, no se publica nada.
 */
public interface PublicadorDeEventos {

    /** Publica el evento en la transacción en curso. */
    void publicar(EventoSaliente evento);
}
