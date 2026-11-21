package ec.mdd.reporting.dominio;

/**
 * El contenido congelado no es el de la versión publicada: su hash no coincide, o sus filas no
 * suman su carga horaria (F20-H01, D-115). Es un error del sistema: el reporte no se genera, porque
 * imprimiría en un documento oficial datos de otra versión.
 */
public class ContenidoInconsistente extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param detalle qué no coincide; va al log, nunca a la respuesta
     */
    public ContenidoInconsistente(String detalle) {
        super(detalle);
    }
}
