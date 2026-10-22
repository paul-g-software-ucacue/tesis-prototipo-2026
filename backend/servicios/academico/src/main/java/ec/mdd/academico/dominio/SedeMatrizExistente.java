package ec.mdd.academico.dominio;

/** La institución ya tiene su sede matriz, y solo hay una (RF-12, research §3). */
public class SedeMatrizExistente extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código de la sede matriz que ya existe
     */
    public SedeMatrizExistente(String codigo) {
        super("La institución ya tiene la sede matriz " + codigo);
    }
}
