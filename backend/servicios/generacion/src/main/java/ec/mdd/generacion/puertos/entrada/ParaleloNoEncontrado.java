package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * La asignatura-paralelo no está en la oferta del periodo, o el periodo no existe. Responde {@code
 * GENERACION_PARALELO_NO_ENCONTRADO}.
 */
public final class ParaleloNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
