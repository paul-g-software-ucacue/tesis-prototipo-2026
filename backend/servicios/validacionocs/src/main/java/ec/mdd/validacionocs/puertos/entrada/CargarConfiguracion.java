package ec.mdd.validacionocs.puertos.entrada;

/** Caso de uso: cargar la configuración de un periodo (RF-34, RF-58). */
public interface CargarConfiguracion {

    /**
     * @param periodo el código del periodo
     * @param archivo el archivo subido
     * @param quien quién lo carga
     * @return la configuración que queda vigente
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoSinPlanificacion si ya no está en Planificación
     * @throws ConfiguracionInvalida con todos sus problemas
     */
    ConfiguracionVigente cargar(String periodo, byte[] archivo, String quien);
}
