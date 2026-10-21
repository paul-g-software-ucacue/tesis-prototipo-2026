package ec.mdd.roles.puertos.entrada;

/**
 * Caso de uso: recibir los eventos de otros procesos (ADR-018). Refleja la operación {@code
 * recibirEvento} del contrato de Roles. Desde F05-H03 solo admite {@code
 * academico.PeriodoActualizado}, con el que Roles guarda la copia de cada periodo.
 */
@FunctionalInterface
public interface RecibirEventos {

    /**
     * Aplica el evento una sola vez por {@code idEvento}; uno repetido no cambia nada.
     *
     * @throws ec.mdd.roles.aplicacion.EventoNoReconocido si el tipo o el contenido no se reconocen
     */
    void recibir(EventoRecibido evento);
}
