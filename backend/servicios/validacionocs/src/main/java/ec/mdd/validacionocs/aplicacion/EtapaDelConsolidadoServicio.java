package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.puertos.entrada.ConsultarEtapaDelConsolidado;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import java.time.Instant;

/**
 * Si el consolidado admite cambios, según la etapa del proceso de aprobación (#215, D-99). Lee la
 * etapa con un bloqueo compartido: dentro de la transacción de Generación, un envío a revisión
 * simultáneo espera a que termine (research §2). Sin proceso, o con el de un distributivo anterior,
 * el distributivo está en BORRADOR_CARRERA y admite cambios (research §5).
 */
public final class EtapaDelConsolidadoServicio implements ConsultarEtapaDelConsolidado {

    private final RepositorioProceso procesos;

    /** Recibe los procesos. */
    public EtapaDelConsolidadoServicio(RepositorioProceso procesos) {
        this.procesos = procesos;
    }

    @Override
    public boolean admiteCambios(String periodo, Instant distributivoCreadoEn) {
        return procesos.estadoCompartido(periodo)
                .filter(uno -> uno.distributivoCreadoEn().equals(distributivoCreadoEn))
                .map(uno -> uno.estado().admiteCambiosEnElConsolidado())
                .orElse(true);
    }
}
