package ec.mdd.generacion.puertos.entrada;

/**
 * Guarda el contenido congelado de una versión, por orden de Aprobación al congelarla (MD-04,
 * F20-H01, D-115). Llama a Datos (D-83): no se usa dentro de una transacción.
 */
public interface GuardarContenidoCongelado {

    /**
     * Calcula y guarda el contenido si la versión está CONGELADA y todavía no lo tiene. Es
     * idempotente: si ya lo tiene, o la versión no existe o no está congelada, no hace nada.
     *
     * @param periodo el código del periodo
     * @param version el número de la versión
     */
    void guardar(String periodo, int version);
}
