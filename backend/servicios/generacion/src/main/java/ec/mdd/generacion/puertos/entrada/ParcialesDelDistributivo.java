package ec.mdd.generacion.puertos.entrada;

/**
 * Lo que Aprobación (Validación OCS) le pide a Generación sobre los parciales del distributivo, en
 * el mismo proceso (F12-H02, MD-04, {@code specs/041-avanzar-etapas/contracts/generacion.md}). Solo
 * la usa Validación OCS; Generación no depende de ella.
 */
public interface ParcialesDelDistributivo {

    /**
     * La versión vigente del distributivo del periodo, cuándo se creó y el estado de cada parcial.
     *
     * @param periodo el código del periodo
     * @return el estado
     * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
     */
    EstadoDelDistributivo estadoDelDistributivo(String periodo);

    /**
     * Marca el parcial de una carrera como enviado y devuelve el hash de su contenido. Bloquea la
     * versión vigente durante su transacción. Es idempotente: si ya estaba enviado, devuelve el
     * mismo hash sin cambiar nada. No registra en el registro de cambios de Generación: la
     * transición la registra Aprobación (D-95).
     *
     * @param periodo el código del periodo
     * @param carrera la carrera del parcial
     * @return la versión y el hash del contenido del parcial
     * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
     * @throws ParcialNoEncontrado si la carrera no tiene parcial en la versión vigente
     */
    ParcialMarcado marcarParcialEnviado(String periodo, String carrera);

    /**
     * El hash del distributivo: el de los hashes de todos sus parciales (research §4).
     *
     * @param periodo el código del periodo
     * @return el SHA-256 en hexadecimal
     * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
     */
    String hashDelDistributivo(String periodo);

    /**
     * Las condiciones del consolidado para enviarlo a revisión (F10-H03, D-97): si se consolidó, si
     * quedó desactualizado, el informe vigente y las inconsistencias abiertas. Se calculan al
     * pedirlas, con el mismo panel que ve la pantalla; no guarda nada. Las reglas que el panel
     * todavía no evalúa no cuentan.
     *
     * @param periodo el código del periodo
     * @return las condiciones
     * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
     */
    CondicionesDeRevision condicionesDeRevision(String periodo);
}
