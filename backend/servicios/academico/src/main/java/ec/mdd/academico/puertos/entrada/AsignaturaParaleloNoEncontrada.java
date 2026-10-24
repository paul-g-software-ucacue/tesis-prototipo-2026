package ec.mdd.academico.puertos.entrada;

/** La asignatura no está en ese paralelo de la oferta del periodo. */
public class AsignaturaParaleloNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     * @param asignatura el código de la asignatura
     * @param paralelo el paralelo
     */
    public AsignaturaParaleloNoEncontrada(String periodo, String asignatura, String paralelo) {
        super(
                "La asignatura "
                        + asignatura
                        + " no está en el paralelo "
                        + paralelo
                        + " de "
                        + periodo);
    }
}
