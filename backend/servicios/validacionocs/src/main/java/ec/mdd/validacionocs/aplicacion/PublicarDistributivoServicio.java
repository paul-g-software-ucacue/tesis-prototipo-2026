package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.EtapaNoAdmitePublicacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PublicarDistributivo;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Publicar el distributivo aprobado (F18-H01, RF-40, MD-11, D-111).
 *
 * <p>Publicar es de Aprobación: Publicación y consulta solo lee la versión publicada. En una sola
 * transacción, con el proceso bloqueado (dos publicaciones a la vez dejan una), registra el paso de
 * APROBADO a PUBLICADO, pide sus notificaciones (D-108) y deja {@code DISTRIBUTIVO_PUBLICADO} en la
 * bitácora. Un rechazo del agregado no deja nada.
 *
 * <p>La versión publicada es la congelada que llegó a APROBADO; su contenido ya no cambia (D-100),
 * y desde aquí el flujo no ofrece acciones.
 */
public final class PublicarDistributivoServicio implements PublicarDistributivo {

    private final RepositorioProceso procesos;
    private final Distributivo distributivo;
    private final DatosDelPeriodo datos;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final ConsultarFlujo flujo;
    private final Clock reloj;
    private final NotificacionDeTransiciones notificacion;

    /**
     * @param procesos los procesos de aprobación
     * @param distributivo Generación, para saber si el periodo tiene distributivo
     * @param datos el estado del periodo, de Académico
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de la publicación
     * @param flujo el estado del flujo que se responde
     * @param reloj el momento de la publicación
     * @param notificacion las notificaciones de la transición (D-108)
     */
    public PublicarDistributivoServicio(
            RepositorioProceso procesos,
            Distributivo distributivo,
            DatosDelPeriodo datos,
            RegistroDeCambios registro,
            Transaccion transaccion,
            ConsultarFlujo flujo,
            Clock reloj,
            NotificacionDeTransiciones notificacion) {
        this.procesos = procesos;
        this.distributivo = distributivo;
        this.datos = datos;
        this.registro = registro;
        this.transaccion = transaccion;
        this.flujo = flujo;
        this.reloj = reloj;
        this.notificacion = notificacion;
    }

    @Override
    public FlujoDelDistributivo publicar(String periodo, Solicitante quien) {
        if (datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new).cerrado()) {
            throw new PeriodoCerrado();
        }
        distributivo.actual(periodo).orElseThrow(SinDistributivo::new);
        Instant ahora = reloj.instant();
        transaccion.ejecutar(
                () -> {
                    // Sin proceso todavía, el distributivo no llegó a APROBADO.
                    ProcesoAprobacion bloqueado =
                            procesos.bloquear(periodo).orElseThrow(EtapaNoAdmitePublicacion::new);
                    List<Transicion> nuevas = bloqueado.publicar(quien, ahora);
                    procesos.guardar(bloqueado, nuevas);
                    notificacion.notificar(bloqueado, nuevas);
                    nuevas.forEach(
                            una ->
                                    registro.registrar(
                                            FlujoDelDistributivoServicio.cambio(
                                                    periodo, una, Optional.empty())));
                    return null;
                });
        return flujo.flujo(periodo, quien);
    }
}
