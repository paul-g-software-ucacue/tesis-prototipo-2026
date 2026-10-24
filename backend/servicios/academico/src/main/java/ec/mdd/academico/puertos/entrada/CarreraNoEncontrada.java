package ec.mdd.academico.puertos.entrada;

/** No hay una carrera con ese código. */
public class CarreraNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public CarreraNoEncontrada(String codigo) {
        super("No hay una carrera con el código " + codigo);
    }
}
