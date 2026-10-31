package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * El periodo no tiene su versión de normativa: el puntaje siempre sale de ella. Responde {@code
 * GENERACION_PERIODO_SIN_ABRIR}.
 */
public final class PeriodoSinAbrir extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
