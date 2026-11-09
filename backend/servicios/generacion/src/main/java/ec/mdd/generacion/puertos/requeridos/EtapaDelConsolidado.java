package ec.mdd.generacion.puertos.requeridos;

import java.time.Instant;

/**
 * Si el consolidado del periodo todavía admite cambios: consolidar, justificar una blanda y
 * adjuntar el informe (#215, D-99). La etapa la conoce Aprobación, que implementa este puerto;
 * Generación pregunta en el momento de cada acción, sin copiar la etapa.
 */
public interface EtapaDelConsolidado {

    /**
     * Si el consolidado admite cambios: sin proceso de aprobación, con el proceso de otro
     * distributivo, o con el distributivo en una etapa anterior a EN_REVISION_VRA. Dentro de una
     * transacción, deja la fila del proceso con un bloqueo compartido hasta que termina, así que un
     * envío a revisión simultáneo espera (research §2).
     *
     * @param periodo el código del periodo
     * @param distributivoCreadoEn cuándo se creó el distributivo vigente
     * @return verdadero si admite cambios
     */
    boolean admiteCambios(String periodo, Instant distributivoCreadoEn);
}
