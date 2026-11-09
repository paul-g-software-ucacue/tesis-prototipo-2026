package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.puertos.entrada.ConsultarEtapaDelConsolidado;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import java.time.Instant;

/** Esqueleto de #215 (T002). */
public final class EtapaDelConsolidadoServicio implements ConsultarEtapaDelConsolidado {

    private final RepositorioProceso procesos;

    /** Recibe los procesos. */
    public EtapaDelConsolidadoServicio(RepositorioProceso procesos) {
        this.procesos = procesos;
    }

    @Override
    public boolean admiteCambios(String periodo, Instant distributivoCreadoEn) {
        return procesos != null;
    }
}
