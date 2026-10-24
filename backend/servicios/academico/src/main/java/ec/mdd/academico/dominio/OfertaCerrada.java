package ec.mdd.academico.dominio;

/**
 * La oferta del periodo ya no cambia: el periodo salió de Planificación (F03-H03, aclaración 2).
 */
public class OfertaCerrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     */
    public OfertaCerrada(String periodo) {
        super("La oferta del periodo " + periodo + " solo cambia en Planificación");
    }
}
