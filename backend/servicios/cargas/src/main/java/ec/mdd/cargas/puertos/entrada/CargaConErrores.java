package ec.mdd.cargas.puertos.entrada;

import java.io.Serial;

/** Se intentó confirmar un archivo con filas con errores (FR-003): no se aplica nada. */
public class CargaConErrores extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final int filas;

    /**
     * @param filas cuántas filas tienen errores
     */
    public CargaConErrores(int filas) {
        super("El archivo tiene " + filas + " filas con errores");
        this.filas = filas;
    }

    /** Cuántas filas tienen errores. */
    public int filas() {
        return filas;
    }
}
