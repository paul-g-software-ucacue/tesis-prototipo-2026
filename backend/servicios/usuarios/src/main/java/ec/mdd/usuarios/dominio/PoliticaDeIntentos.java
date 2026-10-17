package ec.mdd.usuarios.dominio;

import java.time.Duration;
import java.time.Instant;

/**
 * Bloqueo del inicio de sesión por intentos fallidos seguidos (RNF-SEG-07, D-60).
 *
 * <p>Los intentos se cuentan por nombre intentado, exista o no, así que el bloqueo no revela qué
 * usuarios existen. Al llegar al máximo, el nombre queda bloqueado un tiempo; un intento durante el
 * bloqueo no cuenta. Al vencer el bloqueo, la cuenta vuelve a empezar.
 */
public final class PoliticaDeIntentos {

    private final int maximo;
    private final Duration bloqueo;

    /**
     * @param maximo intentos fallidos seguidos que bloquean el nombre; 5 en el MDD
     * @param bloqueo cuánto dura el bloqueo; 15 minutos en el MDD
     */
    public PoliticaDeIntentos(int maximo, Duration bloqueo) {
        if (maximo < 1 || bloqueo == null || bloqueo.isNegative() || bloqueo.isZero()) {
            throw new IllegalArgumentException("El máximo y el bloqueo deben ser positivos");
        }
        this.maximo = maximo;
        this.bloqueo = bloqueo;
    }

    /** Si el nombre está bloqueado en este instante. */
    public boolean estaBloqueado(EstadoDeIntentos estado, Instant ahora) {
        return estado.bloqueadoHasta() != null && estado.bloqueadoHasta().isAfter(ahora);
    }

    /** Si el nombre tuvo un bloqueo que ya terminó: su cuenta debe volver a empezar. */
    public boolean bloqueoVencido(EstadoDeIntentos estado, Instant ahora) {
        return estado.bloqueadoHasta() != null && !estado.bloqueadoHasta().isAfter(ahora);
    }

    /** Si esta cantidad de intentos fallidos seguidos bloquea el nombre. */
    public boolean alcanzaElMaximo(int fallidos) {
        return fallidos >= maximo;
    }

    /** Hasta cuándo queda bloqueado un nombre que se bloquea en este instante. */
    public Instant bloqueadoHasta(Instant ahora) {
        return ahora.plus(bloqueo);
    }

    /** Segundos enteros que faltan para que termine el bloqueo, redondeados hacia arriba. */
    public long segundosRestantes(EstadoDeIntentos estado, Instant ahora) {
        long milisegundos = Duration.between(ahora, estado.bloqueadoHasta()).toMillis();
        return Math.max(1, (milisegundos + 999) / 1000);
    }
}
