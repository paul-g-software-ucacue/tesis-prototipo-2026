package ec.mdd.academico.puertos.entrada;

/**
 * Ya hay un periodo académico con ese código. Está junto a {@link RegistrarPeriodo}, que la lanza,
 * para que los demás servicios del proceso la vean.
 */
public class CodigoRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     */
    public CodigoRepetido(String codigo) {
        super("Ya existe un periodo académico con el código " + codigo);
    }
}
