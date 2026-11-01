package ec.mdd.generacion.dominio;

import java.io.Serial;

/** Una excepción exige un texto de 10 a 500 caracteres (RN-19). */
public final class JustificacionObligatoria extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
