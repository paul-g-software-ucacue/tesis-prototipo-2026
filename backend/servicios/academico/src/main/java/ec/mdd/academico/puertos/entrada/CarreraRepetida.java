package ec.mdd.academico.puertos.entrada;

/** Ya hay una carrera con ese código. */
public class CarreraRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código
     */
    public CarreraRepetida(String codigo) {
        super("Ya hay una carrera con el código " + codigo);
    }
}
