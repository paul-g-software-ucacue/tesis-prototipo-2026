package ec.mdd.generacion.dominio;

import java.io.Serial;

/** El archivo está vacío o no es un PDF. Responde {@code GENERACION_INFORME_NO_ES_PDF}. */
public final class InformeNoEsPdf extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
