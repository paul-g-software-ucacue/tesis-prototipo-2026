package ec.mdd.roles.puertos.entrada;

/**
 * Caso de uso: el Responsable de consolidación de un periodo y su nombre, para el bloque de firmas
 * del documento firmable (F13-H02). Refleja la ruta interna {@code consultarResponsableDelPeriodo}.
 */
@FunctionalInterface
public interface ConsultarResponsable {

    /**
     * @param periodo el código del periodo
     * @return quién es y su nombre
     * @throws SinResponsable si el periodo no tiene Responsable designado
     */
    ResponsableDelPeriodo responsable(String periodo);
}
