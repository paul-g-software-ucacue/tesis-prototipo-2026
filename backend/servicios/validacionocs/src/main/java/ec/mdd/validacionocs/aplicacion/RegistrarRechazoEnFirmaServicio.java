package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.RegistrarRechazoEnFirma;
import ec.mdd.validacionocs.puertos.salida.AvisoDeAnulacion;
import ec.mdd.validacionocs.puertos.salida.AvisoDeAnulacion.FirmasPorAnular;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.VersionNuevaEnAprobacion;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La vuelta a revisión con un rechazo en firma (F14-H04, D-105), con la versión nueva y la orden de
 * anular las firmas (F12-H04, D-106).
 *
 * <p>Tres pasos, como aprobar el contenido (D-100):
 *
 * <ol>
 *   <li>Lee el proceso sin bloquearlo: si el rechazo no cambiaría nada (fuera de CONGELADO_EN_FIRMA
 *       o con otra versión), no hace nada, y Generación ni se entera.
 *   <li>Ordena a Generación la versión nueva, en la transacción de Generación.
 *   <li>Con el proceso bloqueado, el agregado vuelve a comprobar y registra la vuelta a la etapa
 *       activa anterior y la versión nueva; en la misma transacción guarda la orden de anular las
 *       firmas y los cambios para la bitácora.
 * </ol>
 *
 * <p>Si el tercer paso falla, el registro de publicaciones reenvía el aviso (ADR-018): Generación
 * devuelve la versión que ya creó y el tercer paso se completa. Un aviso que no corresponde solo
 * queda en el registro del proceso: lanzar haría que se reintentara para siempre.
 */
public final class RegistrarRechazoEnFirmaServicio implements RegistrarRechazoEnFirma {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegistrarRechazoEnFirmaServicio.class);

    private final RepositorioProceso procesos;
    private final Distributivo distributivo;
    private final AvisoDeAnulacion aviso;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;

    /**
     * @param procesos los procesos de aprobación
     * @param distributivo Generación, que crea la versión nueva
     * @param aviso la orden de anular las firmas, para Firma
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de la vuelta
     */
    public RegistrarRechazoEnFirmaServicio(
            RepositorioProceso procesos,
            Distributivo distributivo,
            AvisoDeAnulacion aviso,
            RegistroDeCambios registro,
            Transaccion transaccion) {
        this.procesos = procesos;
        this.distributivo = distributivo;
        this.aviso = aviso;
        this.registro = registro;
        this.transaccion = transaccion;
    }

    @Override
    public void registrar(String periodo, int version, RechazoDelFirmante rechazo) {
        Optional<ProcesoAprobacion> leido = procesos.de(periodo);
        if (leido.isEmpty()) {
            LOG.warn("Rechazo en firma de {} v{} sin proceso de aprobación", periodo, version);
            return;
        }
        if (!leido.get().admiteRechazoEnFirma(version)) {
            LOG.info(
                    "Rechazo en firma de {} v{}: el proceso no está en firma con esa versión; no"
                            + " cambia",
                    periodo,
                    version);
            return;
        }
        Optional<VersionNuevaEnAprobacion> nueva =
                distributivo.crearVersionNueva(periodo, version, rechazo.rechazadoEn());
        if (nueva.isEmpty()) {
            LOG.warn(
                    "Rechazo en firma de {} v{}: Generación no tiene esa versión congelada ni la"
                            + " siguiente; no cambia",
                    periodo,
                    version);
            return;
        }
        transaccion.ejecutar(
                () -> {
                    ProcesoAprobacion bloqueado = procesos.bloquear(periodo).orElseThrow();
                    List<Transicion> nuevas =
                            new ArrayList<>(
                                    bloqueado.rechazarEnFirma(
                                            version, rechazo, rechazo.rechazadoEn()));
                    if (nuevas.isEmpty()) {
                        LOG.info(
                                "Rechazo en firma de {} v{}: otro aviso ya lo registró; no cambia",
                                periodo,
                                version);
                        return null;
                    }
                    nuevas.addAll(
                            bloqueado.registrarVersionNueva(
                                    version,
                                    nueva.get().version(),
                                    nueva.get().hash(),
                                    rechazo.rechazadoEn()));
                    procesos.guardar(bloqueado, nuevas);
                    nuevas.forEach(
                            una ->
                                    registro.registrar(
                                            FlujoDelDistributivoServicio.cambio(
                                                    periodo, una, Optional.empty())));
                    aviso.avisar(new FirmasPorAnular(periodo, version, rechazo.rechazadoEn()));
                    return null;
                });
    }
}
