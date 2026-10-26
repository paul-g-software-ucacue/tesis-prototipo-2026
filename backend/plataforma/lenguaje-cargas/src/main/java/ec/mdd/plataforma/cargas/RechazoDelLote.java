package ec.mdd.plataforma.cargas;

import java.io.Serial;
import java.util.Optional;

/**
 * Una regla del servicio dueño rechazó una fila del lote al aplicarlo, y nada se aplicó (FR-005).
 * La causa es la excepción del caso de uso del dueño: Cargas busca su código y su texto en el
 * catálogo del proceso. El mensaje no repite valores (principio IX).
 */
public class RechazoDelLote extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final int fila;
    private final String campo;

    /**
     * @param fila la fila del archivo que se aplicaba
     * @param causa la excepción del dueño
     */
    public RechazoDelLote(int fila, RuntimeException causa) {
        super("El servicio dueño rechazó la fila " + fila, causa);
        this.fila = fila;
        this.campo = null;
    }

    /**
     * Un dato que el dueño no admite, con su campo, como su {@code DatoInvalido}.
     *
     * @param fila la fila del archivo
     * @param campo el campo, como lo nombra el dueño
     * @param motivo qué hacer
     */
    public RechazoDelLote(int fila, String campo, String motivo) {
        super(motivo);
        this.fila = fila;
        this.campo = campo;
    }

    /**
     * Un rechazo propio del lote, sin excepción del dueño.
     *
     * @param fila la fila del archivo
     * @param motivo qué hacer
     */
    public RechazoDelLote(int fila, String motivo) {
        super(motivo);
        this.fila = fila;
        this.campo = null;
    }

    /** La fila del archivo que se aplicaba. */
    public int fila() {
        return fila;
    }

    /** El campo que el dueño no admite, si lo dijo. */
    public Optional<String> campo() {
        return Optional.ofNullable(campo);
    }
}
