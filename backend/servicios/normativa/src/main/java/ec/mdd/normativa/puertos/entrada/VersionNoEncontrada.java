package ec.mdd.normativa.puertos.entrada;

/** No hay una versión de normativa con ese número. */
public class VersionNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param numero el número buscado
     */
    public VersionNoEncontrada(int numero) {
        super("No existe la versión de normativa " + numero);
    }
}
