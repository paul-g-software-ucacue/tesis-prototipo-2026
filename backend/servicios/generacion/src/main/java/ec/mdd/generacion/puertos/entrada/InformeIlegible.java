package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El archivo falta o no coincide con su huella. Responde {@code GENERACION_INFORME_ILEGIBLE}. */
public final class InformeIlegible extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
