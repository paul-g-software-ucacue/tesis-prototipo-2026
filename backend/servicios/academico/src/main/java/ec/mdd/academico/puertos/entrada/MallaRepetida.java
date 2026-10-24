package ec.mdd.academico.puertos.entrada;

/** Ya hay una malla con ese código de versión. */
public class MallaRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public MallaRepetida(String codigo) {
        super("Ya hay una malla con el código " + codigo);
    }
}
