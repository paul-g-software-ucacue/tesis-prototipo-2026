package ec.mdd.generacion.puertos.entrada;

import java.math.BigDecimal;

/** Una asignación deja al docente sobre su límite (RN-02). */
public final class HorasExcedidas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal horasQueQuedan;

    /** Recibe las horas que todavía le caben. */
    public HorasExcedidas(BigDecimal horasQueQuedan) {
        super("HorasExcedidas");
        this.horasQueQuedan = horasQueQuedan;
    }

    /** Las horas que todavía le caben. */
    public BigDecimal horasQueQuedan() {
        return horasQueQuedan;
    }
}
