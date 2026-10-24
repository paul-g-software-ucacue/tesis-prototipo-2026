package ec.mdd.academico.puertos.entrada;

/** Ya hay una asignatura con ese código. */
public class AsignaturaRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public AsignaturaRepetida(String codigo) {
        super("Ya hay una asignatura con el código " + codigo);
    }
}
