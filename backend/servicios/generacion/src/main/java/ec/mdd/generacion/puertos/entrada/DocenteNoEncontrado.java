package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El docente no existe en Docentes. Responde {@code GENERACION_DOCENTE_NO_ENCONTRADO}. */
public final class DocenteNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
