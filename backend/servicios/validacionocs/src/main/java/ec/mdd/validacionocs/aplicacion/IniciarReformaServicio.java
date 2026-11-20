package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.EtapaNoAdmiteReforma;
import ec.mdd.validacionocs.dominio.MotivoDeReformaInvalido;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.IniciarReforma;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.VersionNuevaEnAprobacion;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Iniciar una reforma del distributivo publicado (F18-H02, RF-40, D-113). Esqueleto hasta T008. */
public final class IniciarReformaServicio implements IniciarReforma {

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
     * @param distributivo Generación, que crea la versión de la reforma
     * @param datos el estado del periodo, de Académico
     * @param registro los cambios para la bitácora
     * @param transaccion la transacción de la reforma
     * @param flujo el estado del flujo que se responde
     * @param reloj el momento de la reforma
     * @param notificacion las notificaciones de la transición (D-108)
     */
    public IniciarReformaServicio(
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
    public FlujoDelDistributivo iniciar(String periodo, String motivo, Solicitante quien) {
        Observacion razon = motivo(motivo);
        if (datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new).cerrado()) {
            throw new PeriodoCerrado();
        }
        distributivo.actual(periodo).orElseThrow(SinDistributivo::new);
        // Sin proceso todavía, el distributivo no llegó a publicarse.
        ProcesoAprobacion leido = procesos.de(periodo).orElseThrow(EtapaNoAdmiteReforma::new);
        leido.validarReforma(quien);
        int publicada =
                leido.publicacion()
                        .orElseThrow(() -> new IllegalStateException("Publicado sin publicar"))
                        .version();
        Instant ahora = reloj.instant();
        VersionNuevaEnAprobacion nueva =
                distributivo
                        .crearVersionDeReforma(periodo, publicada, quien.usuario(), ahora)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Generación no tiene la versión publicada "
                                                        + publicada
                                                        + " de "
                                                        + periodo
                                                        + " como vigente"));
        transaccion.ejecutar(
                () -> {
                    ProcesoAprobacion bloqueado =
                            procesos.bloquear(periodo).orElseThrow(EtapaNoAdmiteReforma::new);
                    List<Transicion> nuevas =
                            bloqueado.iniciarReforma(
                                    quien, razon, nueva.version(), nueva.hash(), ahora);
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

    private static Observacion motivo(String texto) {
        try {
            return new Observacion(texto);
        } catch (IllegalArgumentException invalido) {
            throw new MotivoDeReformaInvalido();
        }
    }
}
