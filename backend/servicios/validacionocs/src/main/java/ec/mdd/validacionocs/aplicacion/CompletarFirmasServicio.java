package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.CompletarFirmas;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * El paso a EN_OCS con la cadena de firmas completa (F14-H03, D-104).
 *
 * <p>Bloquea el proceso, como los demás pasos de etapa, y deja que el agregado decida: si está en
 * CONGELADO_EN_FIRMA con esa versión, pasa a EN_OCS por el sistema; si no, no cambia nada. La
 * transición se guarda y se registra en la bitácora en la misma transacción. Un aviso que no
 * corresponde solo queda en el registro del proceso: lanzar haría que el registro de publicaciones
 * lo reintentara para siempre (ADR-018).
 */
public final class CompletarFirmasServicio implements CompletarFirmas {

    private static final Logger LOG = LoggerFactory.getLogger(CompletarFirmasServicio.class);

    private final RepositorioProceso procesos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;

    /**
     * @param procesos los procesos de aprobación
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción del paso
     */
    public CompletarFirmasServicio(
            RepositorioProceso procesos, RegistroDeCambios registro, Transaccion transaccion) {
        this.procesos = procesos;
        this.registro = registro;
        this.transaccion = transaccion;
    }

    @Override
    public void completar(String periodo, int version, Instant momento) {
        transaccion.ejecutar(
                () -> {
                    Optional<ProcesoAprobacion> bloqueado = procesos.bloquear(periodo);
                    if (bloqueado.isEmpty()) {
                        LOG.warn(
                                "Cadena completa de {} v{} sin proceso de aprobación",
                                periodo,
                                version);
                        return null;
                    }
                    List<Transicion> nuevas = bloqueado.get().completarFirmas(version, momento);
                    if (nuevas.isEmpty()) {
                        LOG.info(
                                "Cadena completa de {} v{}: el proceso no estaba en firma con esa"
                                        + " versión; no cambia",
                                periodo,
                                version);
                        return null;
                    }
                    procesos.guardar(bloqueado.get(), nuevas);
                    nuevas.forEach(
                            una ->
                                    registro.registrar(
                                            FlujoDelDistributivoServicio.cambio(
                                                    periodo, una, Optional.empty())));
                    return null;
                });
    }
}
