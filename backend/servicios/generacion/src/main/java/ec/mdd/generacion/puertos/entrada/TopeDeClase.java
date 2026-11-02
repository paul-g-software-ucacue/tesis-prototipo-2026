package ec.mdd.generacion.puertos.entrada;

import java.math.BigDecimal;

/** Una asignación deja a un docente con proyecto sobre su tope de clase (RN-13). */
public final class TopeDeClase extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal horasQueQuedan;

    /** Recibe las horas que todavía le caben. */
    public TopeDeClase(BigDecimal horasQueQuedan) {
        super("TopeDeClase");
        this.horasQueQuedan = horasQueQuedan;
    }

    /** Las horas que todavía le caben. */
    public BigDecimal horasQueQuedan() {
        return horasQueQuedan;
    }
}
