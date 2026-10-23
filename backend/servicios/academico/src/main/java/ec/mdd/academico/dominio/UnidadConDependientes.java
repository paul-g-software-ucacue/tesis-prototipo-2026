package ec.mdd.academico.dominio;

/**
 * La unidad tiene unidades activas que dependen de ella, y no se inactiva: así ninguna unidad
 * activa queda debajo de una inactiva (FR-003, research §3).
 */
public class UnidadConDependientes extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código de la unidad
     */
    public UnidadConDependientes(String codigo) {
        super("La unidad " + codigo + " tiene unidades activas que dependen de ella");
    }
}
