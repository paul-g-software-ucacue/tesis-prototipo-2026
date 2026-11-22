package ec.mdd.reporting.adaptadores.entrada;

/**
 * Un archivo de mapeo de más de 256 KB ({@code REPORTING_ARCHIVO_DEMASIADO_GRANDE}, 413). Es un
 * error de entrada: se comprueba antes de leerlo (principio XIII, D-119).
 */
class ArchivoDemasiadoGrande extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ArchivoDemasiadoGrande(long tamano) {
        super("El archivo de mapeo tiene " + tamano + " bytes.");
    }
}
