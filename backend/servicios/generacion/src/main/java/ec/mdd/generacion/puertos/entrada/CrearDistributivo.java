package ec.mdd.generacion.puertos.entrada;

/** Crea el distributivo de un periodo listo (RF-56). */
public interface CrearDistributivo {

    /**
     * Crea el distributivo, con su versión 1 y un parcial por cada carrera con oferta.
     *
     * @param periodo el código del periodo
     * @param quien el usuario que lo crea
     * @return el distributivo creado
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoNoListo si no está listo o se cerró
     * @throws DistributivoRepetido si ya tiene distributivo
     */
    DistributivoDelPeriodo crear(String periodo, String quien);
}
