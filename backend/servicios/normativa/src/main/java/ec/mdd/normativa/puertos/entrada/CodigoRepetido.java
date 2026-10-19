package ec.mdd.normativa.puertos.entrada;

/**
 * Otra versión de normativa ya usa ese código. Está junto a {@link CrearVersion} y {@link
 * EditarVersion}, que la lanzan, para que los demás servicios del proceso la vean.
 */
public class CodigoRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código repetido
     */
    public CodigoRepetido(String codigo) {
        super("Ya existe una versión de normativa con el código " + codigo);
    }
}
