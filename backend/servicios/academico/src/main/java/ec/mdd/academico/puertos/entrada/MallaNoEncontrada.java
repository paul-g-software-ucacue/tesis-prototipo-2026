package ec.mdd.academico.puertos.entrada;

/** No hay una malla con ese código de versión. */
public class MallaNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public MallaNoEncontrada(String codigo) {
        super("No hay una malla con el código " + codigo);
    }
}
