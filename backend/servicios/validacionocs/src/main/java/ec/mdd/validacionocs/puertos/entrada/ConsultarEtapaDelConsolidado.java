package ec.mdd.validacionocs.puertos.entrada;

import java.time.Instant;

/**
 * Caso de uso: si el consolidado del distributivo todavía admite cambios, según su etapa (#215,
 * D-99). Lo usa Generación a través de su puerto requerido.
 */
public interface ConsultarEtapaDelConsolidado {

    /**
     * @param periodo el código del periodo
     * @param distributivoCreadoEn cuándo se creó el distributivo vigente
     * @return verdadero sin proceso, con el de otro distributivo, o en una etapa anterior a
     *     EN_REVISION_VRA
     */
    boolean admiteCambios(String periodo, Instant distributivoCreadoEn);
}
