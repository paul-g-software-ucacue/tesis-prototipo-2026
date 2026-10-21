package ec.mdd.academico.puertos.entrada;

/** No hay un periodo académico con ese código. */
public class PeriodoNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     */
    public PeriodoNoEncontrado(String codigo) {
        super("No hay un periodo académico con el código " + codigo);
    }
}
