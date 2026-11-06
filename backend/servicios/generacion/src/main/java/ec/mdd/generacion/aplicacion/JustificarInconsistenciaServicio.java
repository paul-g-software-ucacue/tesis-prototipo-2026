package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.EntradaDelPanel;
import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoJustificable;
import ec.mdd.generacion.puertos.entrada.JustificarInconsistencia;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDeJustificacion;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioJustificaciones;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Justifica una inconsistencia global blanda (RF-29, MD-07). Vuelve a evaluar el panel antes de
 * guardar, fuera de la transacción: no se abre una durante una llamada a otro proceso (D-83).
 * Cambiar una justificación agrega otra, y el texto no va al registro.
 */
public final class JustificarInconsistenciaServicio implements JustificarInconsistencia {

    private final ConsultarPanel panel;
    private final RepositorioJustificaciones justificaciones;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe el panel y los puertos de salida. */
    public JustificarInconsistenciaServicio(
            ConsultarPanel panel,
            RepositorioJustificaciones justificaciones,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.panel = panel;
        this.justificaciones = justificaciones;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public EntradaDelPanel justificar(SolicitudDeJustificacion solicitud) {
        if (solicitud.tipo() == TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD) {
            throw new InconsistenciaNoJustificable();
        }
        PanelDeInconsistencias actual = panel.panel(solicitud.periodo());
        UUID version = actual.version().orElseThrow(SinDistributivo::new);
        if (!actual.modificable()) {
            throw new PeriodoCerrado();
        }
        EntradaDelPanel entrada =
                actual.entrada(solicitud.tipo(), solicitud.objeto())
                        .orElseThrow(InconsistenciaNoEncontrada::new);
        if (entrada.inconsistencia().severidad() == Severidad.DURA) {
            throw new InconsistenciaNoJustificable();
        }
        Instant ahora = reloj.instant();
        JustificacionDeInconsistencia justificacion =
                JustificacionDeInconsistencia.nueva(
                        solicitud.tipo(),
                        solicitud.objeto(),
                        solicitud.texto(),
                        solicitud.quien(),
                        ahora);
        transaccion.ejecutar(
                () -> {
                    justificaciones.guardar(version, justificacion);
                    registro.registrar(
                            new CambioDeDistributivo(
                                    ahora,
                                    solicitud.quien(),
                                    CambioDeDistributivo.Accion.INCONSISTENCIA_JUSTIFICADA,
                                    Map.of(
                                            "periodo", solicitud.periodo(),
                                            "version", actual.numero(),
                                            "tipo", solicitud.tipo().name(),
                                            "objeto", solicitud.objeto()),
                                    entrada.justificacion()
                                            .map(JustificarInconsistenciaServicio::valores)
                                            .orElse(Map.of()),
                                    valores(justificacion)));
                    return version;
                });
        return new EntradaDelPanel(entrada.inconsistencia(), Optional.of(justificacion));
    }

    /** Si estaba justificada y por quién; nunca el texto. */
    private static Map<String, Object> valores(JustificacionDeInconsistencia justificacion) {
        return Map.of(
                "justificada", true,
                "justificadaPor", justificacion.por(),
                "justificadaEn", justificacion.en().toString());
    }
}
