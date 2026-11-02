package ec.mdd.generacion.dominio;

import java.io.Serial;

/** El archivo pasa de 10 MB. Responde {@code GENERACION_INFORME_DEMASIADO_GRANDE}. */
public final class InformeDemasiadoGrande extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
