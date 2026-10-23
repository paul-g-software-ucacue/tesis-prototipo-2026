package ec.mdd.academico.puertos.entrada;

/** No hay una unidad de la estructura orgánica con ese código. */
public class UnidadNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código de la unidad
     */
    public UnidadNoEncontrada(String codigo) {
        super("No hay una unidad con el código " + codigo);
    }
}
