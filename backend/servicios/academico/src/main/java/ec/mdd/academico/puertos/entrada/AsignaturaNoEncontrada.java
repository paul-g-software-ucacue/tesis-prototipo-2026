package ec.mdd.academico.puertos.entrada;

/** No hay una asignatura con ese código. */
public class AsignaturaNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public AsignaturaNoEncontrada(String codigo) {
        super("No hay una asignatura con el código " + codigo);
    }
}
