package ec.mdd.validacionocs.puertos.entrada;

/** Caso de uso: ver qué rige en un periodo y descargar la plantilla (RF-34). */
public interface ConsultarConfiguracion {

    /**
     * @param periodo el código del periodo
     * @return la última cargada o, si no hay, la configuración por defecto
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    ConfiguracionVigente dePeriodo(String periodo);

    /** El texto de la plantilla, comentada en español. */
    String plantilla();
}
