package ec.mdd.generacion.puertos.entrada;

/** El distributivo de un periodo, si ya se creó. */
public interface ConsultarDistributivo {

    /**
     * El distributivo del periodo.
     *
     * @param periodo el código del periodo
     * @return su distributivo
     * @throws DistributivoNoEncontrado si no tiene
     */
    DistributivoDelPeriodo distributivo(String periodo);
}
