package ec.mdd.academico.dominio;

/**
 * Se pidió para el periodo un estado que no es el siguiente: no se salta ni se retrocede (RF-13,
 * FR-004).
 */
public class TransicionNoPermitida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final EstadoPeriodo actual;
    private final EstadoPeriodo destino;

    /**
     * @param codigo el código del periodo
     * @param actual el estado en que está
     * @param destino el estado pedido
     */
    public TransicionNoPermitida(String codigo, EstadoPeriodo actual, EstadoPeriodo destino) {
        super(
                "El periodo "
                        + codigo
                        + " está en "
                        + actual
                        + " y no puede pasar a "
                        + destino
                        + "; el siguiente es "
                        + actual.siguiente().orElseThrow());
        this.actual = actual;
        this.destino = destino;
    }

    /** El estado en que está. */
    public EstadoPeriodo actual() {
        return actual;
    }

    /** El estado pedido. */
    public EstadoPeriodo destino() {
        return destino;
    }
}
