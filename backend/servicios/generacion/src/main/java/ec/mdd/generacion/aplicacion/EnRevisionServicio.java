package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.puertos.entrada.EnRevision;
import ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;

/**
 * Pregunta a Aprobación, por el puerto que Generación requiere, si el consolidado del distributivo
 * vigente todavía admite cambios (#215, D-99). Dentro de una transacción, la pregunta deja un
 * bloqueo compartido sobre la etapa hasta que termina (research §2).
 */
public final class EnRevisionServicio implements EnRevision {

    private final EtapaDelConsolidado etapa;
    private final RepositorioDistributivos distributivos;

    /** Recibe el puerto de Aprobación y los distributivos. */
    public EnRevisionServicio(EtapaDelConsolidado etapa, RepositorioDistributivos distributivos) {
        this.etapa = etapa;
        this.distributivos = distributivos;
    }

    @Override
    public boolean enRevision(String periodo) {
        return distributivos
                .creadoEn(periodo)
                .map(creadoEn -> !etapa.admiteCambios(periodo, creadoEn))
                .orElse(false);
    }
}
