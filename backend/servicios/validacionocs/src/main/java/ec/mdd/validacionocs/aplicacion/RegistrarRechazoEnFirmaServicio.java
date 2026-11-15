package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.RegistrarRechazoEnFirma;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La vuelta a revisión con un rechazo en firma (F14-H04, D-105).
 *
 * <p>Como el paso a EN_OCS de F14-H03: bloquea el proceso y deja que el agregado decida. Si está en
 * CONGELADO_EN_FIRMA con esa versión, vuelve a la etapa activa anterior por el sistema; si no, no
 * cambia nada. La transición, con su rechazo, se guarda y se registra en la bitácora en la misma
 * transacción. Un aviso que no corresponde solo queda en el registro del proceso: lanzar haría que
 * el registro de publicaciones lo reintentara para siempre (ADR-018).
 */
public final class RegistrarRechazoEnFirmaServicio implements RegistrarRechazoEnFirma {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegistrarRechazoEnFirmaServicio.class);

    private final RepositorioProceso procesos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;

    /**
     * @param procesos los procesos de aprobación
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de la vuelta
     */
    public RegistrarRechazoEnFirmaServicio(
            RepositorioProceso procesos, RegistroDeCambios registro, Transaccion transaccion) {
        this.procesos = procesos;
        this.registro = registro;
        this.transaccion = transaccion;
    }

    @Override
    public void registrar(String periodo, int version, RechazoDelFirmante rechazo) {
        transaccion.ejecutar(
                () -> {
                    Optional<ProcesoAprobacion> bloqueado = procesos.bloquear(periodo);
                    if (bloqueado.isEmpty()) {
                        LOG.warn(
                                "Rechazo en firma de {} v{} sin proceso de aprobación",
                                periodo,
                                version);
                        return null;
                    }
                    List<Transicion> nuevas =
                            bloqueado
                                    .get()
                                    .rechazarEnFirma(version, rechazo, rechazo.rechazadoEn());
                    if (nuevas.isEmpty()) {
                        LOG.info(
                                "Rechazo en firma de {} v{}: el proceso no estaba en firma con esa"
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
