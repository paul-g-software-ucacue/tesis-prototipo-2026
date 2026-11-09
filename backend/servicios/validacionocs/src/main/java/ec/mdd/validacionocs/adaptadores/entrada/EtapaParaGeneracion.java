package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado;
import ec.mdd.validacionocs.puertos.entrada.ConsultarEtapaDelConsolidado;
import java.time.Instant;

/**
 * El puerto que Generación requiere para saber si su consolidado admite cambios (#215, D-99). Lo
 * implementa Aprobación porque es la dueña de la etapa; Generación no depende de Validación OCS.
 */
public final class EtapaParaGeneracion implements EtapaDelConsolidado {

    private final ConsultarEtapaDelConsolidado consultar;

    /**
     * @param consultar el caso de uso de Aprobación
     */
    public EtapaParaGeneracion(ConsultarEtapaDelConsolidado consultar) {
        this.consultar = consultar;
    }

    @Override
    public boolean admiteCambios(String periodo, Instant distributivoCreadoEn) {
        return consultar.admiteCambios(periodo, distributivoCreadoEn);
    }
}
