package ec.mdd.academico.puertos.entrada;

/** Otra carrera ya usa esas siglas. */
public class SiglasRepetidas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public SiglasRepetidas(String codigo) {
        super("Otra carrera ya usa las siglas " + codigo);
    }
}
