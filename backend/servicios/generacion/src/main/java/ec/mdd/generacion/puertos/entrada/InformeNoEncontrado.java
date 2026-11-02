package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** Descargar un informe que no existe. Responde {@code GENERACION_INFORME_NO_ENCONTRADO}. */
public final class InformeNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
