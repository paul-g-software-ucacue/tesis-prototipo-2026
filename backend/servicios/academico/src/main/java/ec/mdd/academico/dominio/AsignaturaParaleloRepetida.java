package ec.mdd.academico.dominio;

/**
 * La asignatura ya está en ese paralelo del periodo, ofertada o retirada (F03-H03, carga 5: la
 * clave es periodo + asignatura + paralelo).
 */
public class AsignaturaParaleloRepetida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     * @param asignatura el código de la asignatura
     * @param paralelo el paralelo
     */
    public AsignaturaParaleloRepetida(String periodo, String asignatura, String paralelo) {
        super(
                "La asignatura "
                        + asignatura
                        + " ya está en el paralelo "
                        + paralelo
                        + " de "
                        + periodo);
    }
}
