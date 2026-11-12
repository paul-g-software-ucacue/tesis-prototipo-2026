package ec.mdd.generacion.puertos.entrada;

/**
 * El contenido de la versión congelada, para el documento firmable (F13-H02, D-101). Solo lee, y
 * puede llamar a Datos (D-83): no se usa dentro de una transacción.
 */
public interface ContenidoCongelado {

    /**
     * @param periodo el código del periodo
     * @return la versión, su hash, el resumen por carrera, los totales y cada docente
     * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
     * @throws VersionNoCongelada si la versión vigente no está CONGELADA
     */
    ContenidoDeLaVersion contenido(String periodo);
}
