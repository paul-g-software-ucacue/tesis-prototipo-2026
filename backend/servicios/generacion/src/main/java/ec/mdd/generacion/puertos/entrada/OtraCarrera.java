package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** La asignatura-paralelo es de otra carrera que la del coordinador. Responde 403 (D-64). */
public final class OtraCarrera extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
