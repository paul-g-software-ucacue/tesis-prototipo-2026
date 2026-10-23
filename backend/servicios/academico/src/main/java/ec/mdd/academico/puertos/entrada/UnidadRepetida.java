package ec.mdd.academico.puertos.entrada;

/**
 * Ya hay una unidad de la estructura orgánica con ese código. Está junto a {@link
 * MantenerEstructura}, que la lanza, para que los demás servicios del proceso la vean.
 */
public class UnidadRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código de la unidad
     */
    public UnidadRepetida(String codigo) {
        super("Ya existe una unidad con el código " + codigo);
    }
}
