package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.EntradaDelPanel;

/** Justificar una inconsistencia global blanda (RF-29, MD-07). */
public interface JustificarInconsistencia {

    /**
     * Guarda o cambia la justificación.
     *
     * @param solicitud lo que pide quien justifica
     * @return la inconsistencia justificada
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws InconsistenciaNoEncontrada si ya no está en el panel
     * @throws InconsistenciaNoJustificable si es dura o es de una propuesta
     * @throws ec.mdd.generacion.dominio.JustificacionObligatoria si el texto tiene un largo
     *     inválido
     */
    EntradaDelPanel justificar(SolicitudDeJustificacion solicitud);
}
