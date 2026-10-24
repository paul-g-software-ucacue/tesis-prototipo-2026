package ec.mdd.cargas.puertos.entrada;

/** No hay una de las 10 cargas con ese código. */
public class CargaNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código pedido
     */
    public CargaNoEncontrada(String codigo) {
        super("No hay una carga con el código " + codigo);
    }
}
